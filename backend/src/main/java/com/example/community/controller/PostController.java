package com.example.community.controller;

import com.example.community.dto.*;
import com.example.community.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @GetMapping
    public Page<PostItemResponse> list(
        @RequestParam(value = "q", required = false) String q,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size,
        @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort,
        Authentication auth
    ) {
        return postService.list(q, page, size, sort, auth.getName());
    }

    @GetMapping("/{id}")
    public PostDetailResponse detail(@PathVariable Long id, Authentication auth) {
        return postService.detail(id, auth.getName());
    }

    @PostMapping
    public PostItemResponse create(@Valid @RequestBody PostRequest req, Authentication auth) {
        return postService.create(auth.getName(), req);
    }

    @PutMapping("/{id}")
    public PostItemResponse update(@PathVariable Long id, @Valid @RequestBody PostRequest req, Authentication auth) {
        return postService.update(auth.getName(), id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, Authentication auth) {
        postService.delete(auth.getName(), id);
    }

    @PostMapping("/{id}/likes")
    public LikeResponse toggleLike(@PathVariable Long id, Authentication auth) {
        return postService.toggleLike(auth.getName(), id);
    }

    @GetMapping("/{id}/likes/me")
    public LikeResponse likeMe(@PathVariable Long id, Authentication auth) {
        return postService.likeState(auth.getName(), id);
    }

    @GetMapping("/{id}/comments")
    public Page<CommentResponse> comments(
        @PathVariable Long id,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return postService.comments(id, page, size);
    }

    @PostMapping("/{id}/comments")
    public CommentResponse writeComment(@PathVariable Long id, @Valid @RequestBody CommentRequest req, Authentication auth) {
        return postService.writeComment(auth.getName(), id, req);
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public void deleteComment(@PathVariable Long id, @PathVariable Long commentId, Authentication auth) {
        postService.deleteComment(auth.getName(), id, commentId);
    }

    @PutMapping("/{id}/comments/{commentId}")
    public CommentResponse updateComment(
        @PathVariable Long id,
        @PathVariable Long commentId,
        @Valid @RequestBody CommentRequest req,
        Authentication auth
    ) {
        return postService.updateComment(auth.getName(), id, commentId, req);
    }
}
