import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api, { emitToast } from "../api/http";
import { useAuth } from "../context/AuthContext";

export default function PostDetailPage() {
  const { id } = useParams();
  const nav = useNavigate();
  const { user } = useAuth();
  const [post, setPost] = useState(null);
  const [comments, setComments] = useState([]);
  const [commentContent, setCommentContent] = useState("");
  const [commentPage, setCommentPage] = useState(0);
  const [commentTotal, setCommentTotal] = useState(0);
  const [editingCommentId, setEditingCommentId] = useState(null);
  const [editingCommentContent, setEditingCommentContent] = useState("");
  const [editingPost, setEditingPost] = useState(false);
  const [editingPostTitle, setEditingPostTitle] = useState("");
  const [editingPostContent, setEditingPostContent] = useState("");

  const load = async () => {
    const [postRes, commentRes] = await Promise.all([
      api.get(`/posts/${id}`),
      api.get(`/posts/${id}/comments`, { params: { page: commentPage, size: 10 } })
    ]);
    setPost(postRes.data);
    if (!editingPost) {
      setEditingPostTitle(postRes.data.title);
      setEditingPostContent(postRes.data.content);
    }
    setComments(commentRes.data.content || []);
    setCommentTotal(commentRes.data.totalPages || 0);
  };

  const saveComment = async (event) => {
    event.preventDefault();
    if (!commentContent.trim()) return;
    await api.post(`/posts/${id}/comments`, { content: commentContent });
    emitToast("댓글이 등록되었습니다.", "success");
    setCommentContent("");
    setCommentPage(0);
    await load();
  };

  const saveEditedComment = async (commentId) => {
    const content = editingCommentContent.trim();
    const target = comments.find((item) => item.id === commentId);
    if (!target || target.author !== user?.username) {
      return;
    }
    if (!content) return;
    await api.put(`/posts/${id}/comments/${commentId}`, { content });
    emitToast("댓글이 수정되었습니다.", "success");
    setEditingCommentId(null);
    setEditingCommentContent("");
    await load();
  };

  const deleteComment = async (commentId) => {
    const target = comments.find((item) => item.id === commentId);
    if (!target || target.author !== user?.username) {
      return;
    }
    if (!window.confirm("댓글을 삭제하시겠습니까?")) return;
    await api.delete(`/posts/${id}/comments/${commentId}`);
    emitToast("댓글이 삭제되었습니다.", "success");
    await load();
  };

  const removePost = async () => {
    if (!window.confirm("게시글을 삭제하시겠습니까?")) return;
    await api.delete(`/posts/${id}`);
    emitToast("게시글이 삭제되었습니다.", "success");
    nav("/posts");
  };

  const startEditPost = () => {
    if (!post || user?.username !== post.author) {
      return;
    }
    setEditingPostTitle(post.title);
    setEditingPostContent(post.content);
    setEditingPost(true);
  };

  const cancelEditPost = () => {
    setEditingPost(false);
    if (post) {
      setEditingPostTitle(post.title);
      setEditingPostContent(post.content);
    }
  };

  const saveEditedPost = async (event) => {
    event.preventDefault();
    if (!post || user?.username !== post.author) {
      return;
    }
    const title = editingPostTitle.trim();
    const content = editingPostContent.trim();
    if (!title || !content) return;
    const res = await api.put(`/posts/${id}`, { title, content });
    setPost(res.data);
    setEditingPost(false);
    emitToast("게시글이 수정되었습니다.", "success");
  };

  const beginEditComment = (comment) => {
    if (comment.author !== user?.username) {
      return;
    }
    setEditingCommentId(comment.id);
    setEditingCommentContent(comment.content);
  };

  const cancelEditComment = () => {
    setEditingCommentId(null);
    setEditingCommentContent("");
  };

  const toggleLike = async () => {
    const res = await api.post(`/posts/${id}/likes`);
    const detail = { ...post };
    detail.likeCount = res.data.likeCount;
    detail.likedByMe = res.data.liked;
    setPost(detail);
  };

  useEffect(() => {
    setCommentPage(0);
    setEditingPost(false);
    load();
  }, [id]);

  useEffect(() => {
    load();
  }, [commentPage]);

  if (!post) return <p>불러오는 중...</p>;

  return (
    <div className="page">
      <button onClick={() => nav("/posts")}>목록으로</button>
      <article className="post-card">
        {editingPost ? (
          <form className="post-edit" onSubmit={saveEditedPost}>
            <input
              value={editingPostTitle}
              onChange={(event) => setEditingPostTitle(event.target.value)}
              placeholder="제목"
            />
            <textarea
              value={editingPostContent}
              onChange={(event) => setEditingPostContent(event.target.value)}
              placeholder="내용"
            />
            <div className="comment-actions">
              <button type="submit">저장</button>
              <button type="button" onClick={cancelEditPost}>취소</button>
            </div>
          </form>
        ) : (
          <>
            <h1>{post.title}</h1>
            <p className="meta">작성자: {post.author} | {new Date(post.createdAt).toLocaleString()} | 좋아요 {post.likeCount}</p>
            <pre>{post.content}</pre>
          </>
        )}
        <div className="comment-actions">
          {!editingPost && <button className="like-btn" onClick={toggleLike}>
            {post.likedByMe ? "좋아요 취소" : "좋아요"}
          </button>}
          {user?.username === post.author && !editingPost && (
            <>
              <button type="button" onClick={startEditPost}>
                글 수정
              </button>
              <button className="danger-btn" onClick={removePost}>
                글 삭제
              </button>
            </>
          )}
        </div>
      </article>

      <section className="comment-block">
        <h3>댓글</h3>
        <form onSubmit={saveComment}>
          <textarea
            value={commentContent}
            onChange={(e) => setCommentContent(e.target.value)}
            placeholder="댓글을 입력하세요"
          />
          <button type="submit">등록</button>
        </form>

        {comments.map((item) => (
          <div key={item.id} className="comment-item">
            <div className="meta">{item.author} · {new Date(item.createdAt).toLocaleString()}</div>
            {editingCommentId === item.id ? (
              <div className="comment-edit">
                <textarea
                  value={editingCommentContent}
                  onChange={(event) => setEditingCommentContent(event.target.value)}
                  placeholder="댓글을 수정하세요"
                />
                <div className="comment-actions">
                  <button
                    type="button"
                    onClick={() => saveEditedComment(item.id)}
                  >
                    저장
                  </button>
                  <button type="button" onClick={cancelEditComment}>
                    취소
                  </button>
                </div>
              </div>
            ) : (
              <p>{item.content}</p>
            )}
            {item.author === user?.username && editingCommentId !== item.id && (
              <div className="comment-actions">
                <button type="button" onClick={() => beginEditComment(item)}>
                  수정
                </button>
                <button type="button" onClick={() => deleteComment(item.id)}>
                  삭제
                </button>
              </div>
            )}
          </div>
        ))}

        {commentTotal > 1 && (
          <div className="comment-pager">
            <button disabled={commentPage <= 0} onClick={() => setCommentPage(commentPage - 1)}>이전</button>
            <span>{commentPage + 1}/{commentTotal}</span>
            <button disabled={commentPage >= commentTotal - 1} onClick={() => setCommentPage(commentPage + 1)}>다음</button>
          </div>
        )}
      </section>
    </div>
  );
}
