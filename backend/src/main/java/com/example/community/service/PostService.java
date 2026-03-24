package com.example.community.service;

import com.example.community.domain.Comment;
import com.example.community.domain.Member;
import com.example.community.domain.Post;
import com.example.community.domain.PostLike;
import com.example.community.dto.CommentRequest;
import com.example.community.dto.CommentResponse;
import com.example.community.dto.LikeResponse;
import com.example.community.dto.PostDetailResponse;
import com.example.community.dto.PostItemResponse;
import com.example.community.dto.PostRequest;
import com.example.community.repository.CommentRepository;
import com.example.community.repository.MemberRepository;
import com.example.community.repository.PostLikeRepository;
import com.example.community.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final OperationLogService operationLogService;

    public Page<PostItemResponse> list(String query, int page, int size, String sort, String viewerUsername) {
        Pageable pageable = pageable(page, size, sort);
        String q = query == null || query.isBlank() ? null : query.trim();
        Page<Post> posts = (q == null)
            ? postRepository.findAll(pageable)
            : postRepository.search(q, pageable);
        return posts.map(p -> toItem(p, viewerUsername));
    }

    public PostDetailResponse detail(Long id, String viewerUsername) {
        Post post = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        Member viewer = resolveViewer(viewerUsername);
        boolean liked = viewer != null && postLikeRepository.existsByPostAndMember(post, viewer);
        long likeCount = postLikeRepository.countByPost(post);
        long commentCount = commentRepository.countByPost(post);
        return PostDetailResponse.of(post, likeCount, commentCount, liked);
    }

    @Transactional
    public PostItemResponse create(String username, PostRequest req) {
        Member author = memberRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("작성자 없음"));
        Post post = Post.builder()
            .title(req.title())
            .content(req.content())
            .author(author)
            .build();
        Post saved = postRepository.save(post);
        return toItem(saved, username);
    }

    @Transactional
    public PostItemResponse update(String username, Long id, PostRequest req) {
        Post post = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        if (!post.getAuthor().getUsername().equals(username)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }
        post.setTitle(req.title());
        post.setContent(req.content());
        return toItem(post, username);
    }

    @Transactional
    public void delete(String username, Long id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        if (!post.getAuthor().getUsername().equals(username)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }
        String title = post.getTitle();
        // Remove dependents first so MariaDB foreign key constraints do not reject post deletion.
        postLikeRepository.deleteByPost(post);
        commentRepository.deleteByPost(post);
        postRepository.delete(post);
        operationLogService.record(
            username,
            OperationLogService.ACTION_DELETE_POST,
            "POST",
            id,
            title,
            String.format("삭제된 게시글 제목: %s", title)
        );
    }

    @Transactional
    public LikeResponse toggleLike(String username, Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        Member member = memberRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        java.util.Optional<PostLike> already = postLikeRepository.findByPostAndMember(post, member);
        if (already.isPresent()) {
            postLikeRepository.delete(already.get());
            long count = postLikeRepository.countByPost(post);
            return new LikeResponse(postId, count, false);
        }

        postLikeRepository.save(PostLike.builder().post(post).member(member).build());
        long count = postLikeRepository.countByPost(post);
        return new LikeResponse(postId, count, true);
    }

    public LikeResponse likeState(String username, Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        long count = postLikeRepository.countByPost(post);
        Member viewer = resolveViewer(username);
        boolean liked = viewer != null && postLikeRepository.existsByPostAndMember(post, viewer);
        return new LikeResponse(postId, count, liked);
    }

    public Page<CommentResponse> comments(Long postId, int page, int size) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        Pageable pageable = PageRequest.of(
            Math.max(page, 0),
            Math.min(size <= 0 ? 10 : size, 50),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Comment> comments = commentRepository.findByPostOrderByCreatedAtDesc(post, pageable);
        return comments.map(CommentResponse::from);
    }

    @Transactional
    public CommentResponse writeComment(String username, Long postId, CommentRequest req) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        Member member = memberRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
        Comment comment = Comment.builder()
            .post(post)
            .author(member)
            .content(req.content())
            .build();
        return CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(String username, Long postId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new IllegalArgumentException("댓글 없음"));
        if (!comment.getPost().getId().equals(postId)) {
            throw new IllegalArgumentException("해당 게시글의 댓글이 아닙니다.");
        }
        if (!comment.getAuthor().getUsername().equals(username)) {
            throw new IllegalStateException("댓글 삭제 권한이 없습니다.");
        }
        String preview = comment.getContent();
        commentRepository.delete(comment);
        operationLogService.record(
            username,
            OperationLogService.ACTION_DELETE_COMMENT,
            "COMMENT",
            commentId,
            null,
            String.format("게시글=%s, 댓글=%s", postId, preview == null ? "" : preview)
        );
    }

    @Transactional
    public CommentResponse updateComment(String username, Long postId, Long commentId, CommentRequest req) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new IllegalArgumentException("댓글 없음"));
        if (!comment.getPost().getId().equals(postId)) {
            throw new IllegalArgumentException("해당 게시글의 댓글이 아닙니다.");
        }
        if (!comment.getAuthor().getUsername().equals(username)) {
            throw new IllegalStateException("댓글 수정 권한이 없습니다.");
        }
        comment.setContent(req.content());
        return CommentResponse.from(comment);
    }

    private Pageable pageable(int page, int size, String sort) {
        int pageNo = Math.max(page, 0);
        int pageSize = Math.max(1, Math.min(size <= 0 ? 10 : size, 50));

        String[] sortParts = (sort == null || sort.isBlank())
            ? new String[]{"createdAt", "desc"}
            : sort.split(",");
        String sortBy = sanitizeSort(sortParts[0]);
        Sort.Direction direction = (sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1]))
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        return PageRequest.of(pageNo, pageSize, Sort.by(direction, sortBy));
    }

    private PostItemResponse toItem(Post p, String username) {
        Member viewer = resolveViewer(username);
        long likeCount = postLikeRepository.countByPost(p);
        long commentCount = commentRepository.countByPost(p);
        boolean liked = viewer != null && postLikeRepository.existsByPostAndMember(p, viewer);
        return PostItemResponse.of(p, likeCount, commentCount, liked);
    }

    private Member resolveViewer(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return memberRepository.findByUsername(username).orElse(null);
    }

    private String sanitizeSort(String sortBy) {
        Set<String> allowed = Set.of("createdAt", "updatedAt", "title", "id");
        if (allowed.contains(sortBy)) {
            return sortBy;
        }
        return "createdAt";
    }
}
