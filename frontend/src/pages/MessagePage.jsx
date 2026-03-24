import { useEffect, useState } from "react";
import api, { emitToast } from "../api/http";
import { useAuth } from "../context/AuthContext";

export default function MessagePage() {
  const { user } = useAuth();
  const [mode, setMode] = useState("received");

  const [receivedMessages, setReceivedMessages] = useState([]);
  const [receivedPage, setReceivedPage] = useState(0);
  const [receivedTotalPages, setReceivedTotalPages] = useState(0);
  const [onlyUnread, setOnlyUnread] = useState(false);

  const [sentMessages, setSentMessages] = useState([]);
  const [sentPage, setSentPage] = useState(0);
  const [sentTotalPages, setSentTotalPages] = useState(0);

  const [selected, setSelected] = useState(null);
  const [blockedUsers, setBlockedUsers] = useState([]);
  const [form, setForm] = useState({
    receiverUsername: "",
    title: "",
    content: ""
  });

  const onChange = (key, value) => setForm((prev) => ({ ...prev, [key]: value }));

  const currentMessages = mode === "received" ? receivedMessages : sentMessages;
  const currentPage = mode === "received" ? receivedPage : sentPage;
  const currentTotal = mode === "received" ? receivedTotalPages : sentTotalPages;

  const setCurrentPage = (value) => {
    if (mode === "received") {
      setReceivedPage(value);
    } else {
      setSentPage(value);
    }
  };

  const loadReceived = async () => {
    const params = { page: receivedPage, size: 10 };
    if (onlyUnread) params.onlyUnread = true;
    const res = await api.get("/messages/received", { params });
    const rows = res.data.content || [];
    setReceivedMessages(rows);
    setReceivedTotalPages(res.data.totalPages || 0);
  };

  const loadSent = async () => {
    const res = await api.get("/messages/sent", {
      params: { page: sentPage, size: 10 }
    });
    const rows = res.data.content || [];
    setSentMessages(rows);
    setSentTotalPages(res.data.totalPages || 0);
  };

  const loadBlocks = async () => {
    const res = await api.get("/messages/blocks");
    setBlockedUsers(res.data || []);
  };

  const syncAllMailbox = async () => {
    await Promise.all([loadReceived(), loadSent(), loadBlocks()]);
  };

  const refreshCurrent = async () => {
    if (mode === "received") {
      await loadReceived();
    } else {
      await loadSent();
    }
  };

  const refreshAll = async () => {
    await syncAllMailbox();
    emitToast("쪽지함을 동기화했습니다.", "success", 1300);
  };

  const send = async (event) => {
    event.preventDefault();
    const receiver = form.receiverUsername.trim();
    const title = form.title.trim();
    const content = form.content.trim();
    if (!receiver) {
      return;
    }
    if (!title || !content) {
      return;
    }
    if (blockedUsers.includes(receiver)) {
      emitToast("차단한 사용자에게는 메시지를 보낼 수 없습니다.", "error");
      return;
    }
    await api.post("/messages/send", {
      receiverUsername: receiver,
      title,
      content
    });
    emitToast("쪽지를 보냈습니다.", "success", 1300);
    setForm({ receiverUsername: "", title: "", content: "" });
    await refreshAll();
    setSentPage(0);
    setMode("sent");
  };

  const open = async (message) => {
    if (mode === "received" && !message.read) {
      const readRes = await api.post(`/messages/${message.id}/read`);
      setSelected(readRes.data);
      await refreshAll();
      return;
    }
    setSelected(message);
  };

  const removeMessage = async (message) => {
    if (!window.confirm("쪽지를 삭제하시겠습니까?")) return;
    await api.delete(`/messages/${message.id}`);
    emitToast("쪽지가 삭제되었습니다.", "success");
    if (selected?.id === message.id) {
      setSelected(null);
    }
    await refreshAll();
  };

  const blockUser = async (username) => {
    if (username === user?.username) return;
    await api.post(`/messages/block/${username}`);
    emitToast(`${username}님을 차단했습니다.`, "success", 1300);
    await loadBlocks();
    await loadReceived();
  };

  const unblockUser = async (username) => {
    await api.delete(`/messages/block/${username}`);
    emitToast(`${username}님을 차단 해제했습니다.`, "success", 1300);
    await loadBlocks();
    await loadReceived();
  };

  const switchMode = (next) => {
    setMode(next);
    setSelected(null);
  };

  const toggleOnlyUnread = (next) => {
    setOnlyUnread(next);
    setReceivedPage(0);
  };

  useEffect(() => {
    refreshCurrent();
  }, [mode, receivedPage, sentPage, onlyUnread]);

  useEffect(() => {
    const handler = () => {
      if (!user?.username || mode !== "received" && mode !== "sent") {
        return;
      }
      if (mode === "received" || mode === "sent") {
        syncAllMailbox();
      }
    };
    window.addEventListener("app-message-event", handler);
    return () => window.removeEventListener("app-message-event", handler);
  }, [mode, receivedPage, sentPage, onlyUnread]);

  useEffect(() => {
    loadBlocks();
  }, [user?.username]);

  useEffect(() => {
    if (!selected && mode === "received") {
      if (onlyUnread && currentMessages.length === 0 && receivedPage > 0) {
        setReceivedPage((prev) => prev - 1);
      }
    }
  }, [currentMessages.length, currentPage, onlyUnread]);

  return (
    <div className="page">
      <div className="message-header">
        <h2>쪽지함</h2>
        <div className="message-refresh">
          <button onClick={refreshAll}>전체 새로고침</button>
        </div>
      </div>
      <div className="message-layout">
        <aside className="message-list">
          <div className="message-tabs">
            <button className={mode === "received" ? "active" : ""} onClick={() => switchMode("received")}>받은 쪽지</button>
            <button className={mode === "sent" ? "active" : ""} onClick={() => switchMode("sent")}>보낸 쪽지</button>
          </div>

          {mode === "received" && (
            <label className="toggle">
              <input type="checkbox" checked={onlyUnread} onChange={(e) => toggleOnlyUnread(e.target.checked)} />
              안 읽은 쪽지만 보기
            </label>
          )}

            <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>상태</th>
                  <th>{mode === "received" ? "보낸사람" : "받는사람"}</th>
                  <th>제목</th>
                  <th>시간</th>
                  <th>작업</th>
                </tr>
              </thead>
              <tbody>
                {currentMessages.map((msg) => (
                  <tr
                    key={msg.id}
                    className={(mode === "received" && !msg.read) ? "unread-row" : ""}
                    onClick={() => open(msg)}
                  >
                    <td>{mode === "received" ? (msg.read ? "읽음" : "안읽음") : "-"}</td>
                    <td>{mode === "received" ? msg.sender : msg.receiver}</td>
                    <td>{msg.title}</td>
                    <td>{new Date(msg.createdAt).toLocaleString()}</td>
                    <td className="message-actions-cell" onClick={(event) => event.stopPropagation()}>
                      {mode === "received" && msg.sender !== user?.username && (
                        blockedUsers.includes(msg.sender) ? (
                          <button
                            type="button"
                            className="danger-btn"
                            onClick={() => unblockUser(msg.sender)}
                          >
                            차단해제
                          </button>
                        ) : (
                          <button type="button" onClick={() => blockUser(msg.sender)}>
                            차단
                          </button>
                        )
                      )}
                      <button type="button" className="danger-btn" onClick={() => removeMessage(msg)}>
                        삭제
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="message-pager">
            <button disabled={currentPage <= 0} onClick={() => setCurrentPage(currentPage - 1)}>이전</button>
            <span>{currentPage + 1}/{Math.max(currentTotal, 1)}</span>
            <button disabled={currentPage >= currentTotal - 1} onClick={() => setCurrentPage(currentPage + 1)}>다음</button>
          </div>
        </aside>

        <section className="message-detail">
          <form className="send-form" onSubmit={send}>
            <h3>쪽지 보내기</h3>
            <input value={form.receiverUsername} onChange={(e) => onChange("receiverUsername", e.target.value)} placeholder="받는 아이디" />
            <input value={form.title} onChange={(e) => onChange("title", e.target.value)} placeholder="제목" />
            <textarea value={form.content} onChange={(e) => onChange("content", e.target.value)} placeholder="내용" />
            <button type="submit">보내기</button>
          </form>

          {selected ? (
            <article className="selected-box">
              <h4>{selected.title}</h4>
              <p className="meta">
                {mode === "received" ? `보낸 사람: ${selected.sender}` : `받는 사람: ${selected.receiver}`}
                {mode === "received" ? ` / 상태: ${selected.read ? "읽음" : "안읽음"}` : ""}
              </p>
              <p>{selected.content}</p>
              <div className="comment-actions">
                <button className="danger-btn" onClick={() => removeMessage(selected)}>
                  삭제
                </button>
              </div>
            </article>
          ) : (
            <p className="select-guide">목록에서 쪽지를 선택하면 상세를 볼 수 있습니다.</p>
          )}

          <section className="block-list">
            <h3>차단 목록</h3>
            {blockedUsers.length === 0 ? (
              <p className="muted">차단한 회원이 없습니다.</p>
            ) : (
              <ul>
                {blockedUsers.map((name) => (
                  <li key={name}>
                    <span>{name}</span>
                    <button type="button" className="danger-btn" onClick={() => unblockUser(name)}>
                      차단해제
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </section>
      </div>
    </div>
  );
}
