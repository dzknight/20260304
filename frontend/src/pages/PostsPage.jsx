import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../api/http";
import Pager from "../components/Pager";

export default function PostsPage() {
  const [query, setQuery] = useState("");
  const [searchText, setSearchText] = useState("");
  const [draft, setDraft] = useState({ title: "", content: "" });
  const [posts, setPosts] = useState([]);
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [sort, setSort] = useState("createdAt,desc");
  const [totalPages, setTotalPages] = useState(0);

  const loadPosts = async () => {
    const res = await api.get("/posts", {
      params: {
        q: query || undefined,
        page,
        size,
        sort
      }
    });
    setPosts(res.data.content || []);
    setTotalPages(res.data.totalPages || 0);
  };

  const createPost = async (event) => {
    event.preventDefault();
    if (!draft.title.trim() || !draft.content.trim()) return;
    await api.post("/posts", draft);
    setDraft({ title: "", content: "" });
    await loadPosts();
  };

  const doSearch = (event) => {
    event.preventDefault();
    setPage(0);
    setQuery(searchText.trim());
  };

  useEffect(() => {
    loadPosts();
  }, [page, query, sort]);

  return (
    <div className="page">
      <h2>게시글</h2>

      <form className="search-bar" onSubmit={doSearch}>
        <input value={searchText} onChange={(e) => setSearchText(e.target.value)} placeholder="제목/내용/작성자 검색" />
        <button type="submit">검색</button>
      </form>

      <div className="search-bar" style={{ marginTop: 0 }}>
        <select value={sort} onChange={(e) => setSort(e.target.value)}>
          <option value="createdAt,desc">최신순</option>
          <option value="createdAt,asc">오래된순</option>
          <option value="title,asc">제목 오름차순</option>
          <option value="title,desc">제목 내림차순</option>
        </select>
      </div>

      <form className="post-form" onSubmit={createPost}>
        <h3>새 글</h3>
        <input value={draft.title} onChange={(e) => setDraft((prev) => ({ ...prev, title: e.target.value }))} placeholder="제목" />
        <textarea value={draft.content} onChange={(e) => setDraft((prev) => ({ ...prev, content: e.target.value }))} placeholder="내용" />
        <button type="submit">작성</button>
      </form>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th style={{ width: "8%" }}>ID</th>
              <th>제목</th>
              <th style={{ width: "12%" }}>작성자</th>
              <th style={{ width: "20%" }}>작성일</th>
              <th style={{ width: "8%" }}>좋아요</th>
              <th style={{ width: "8%" }}>댓글</th>
            </tr>
          </thead>
          <tbody>
            {posts.map((p) => (
              <tr key={p.id} className="post-row">
                <td>{p.id}</td>
                <td>
                  <Link className="title-link" to={`/posts/${p.id}`}>
                    {p.title}
                  </Link>
                </td>
                <td>{p.author}</td>
                <td>{new Date(p.createdAt).toLocaleString()}</td>
                <td>❤ {p.likeCount}</td>
                <td>💬 {p.commentCount}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <Pager current={page} total={totalPages} onChange={setPage} />
    </div>
  );
}

