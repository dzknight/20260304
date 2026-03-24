import { BrowserRouter, Navigate, NavLink, Route, Routes } from "react-router-dom";
import { useEffect, useMemo, useRef, useState } from "react";
import api, { API_BASE_URL, emitToast } from "./api/http";
import { useAuth } from "./context/AuthContext";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import PostsPage from "./pages/PostsPage";
import PostDetailPage from "./pages/PostDetailPage";
import MessagePage from "./pages/MessagePage";
import AuditLogPage from "./pages/AuditLogPage";

const MESSAGE_SYNC_EVENT = "app-message-event";

function Protected({ children }) {
  const { token } = useAuth();
  return token ? children : <Navigate to="/login" replace />;
}

function emitMessageEvent(detail) {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent(MESSAGE_SYNC_EVENT, { detail }));
  }
}

function NotificationArea() {
  const { token, user, logout } = useAuth();
  const [unread, setUnread] = useState(0);
  const pollerRef = useRef(null);
  const sseRef = useRef(null);
  const unreadRef = useRef(0);

  const stopPolling = () => {
    if (pollerRef.current) {
      clearInterval(pollerRef.current);
      pollerRef.current = null;
    }
  };

  const closeSse = () => {
    if (sseRef.current) {
      sseRef.current.close();
      sseRef.current = null;
    }
  };

  const updateUnread = (nextCount) => {
    const count = Number(nextCount);
    if (!Number.isFinite(count) || count < 0) {
      return;
    }
    if (unreadRef.current !== count) {
      unreadRef.current = count;
      setUnread(count);
      emitMessageEvent({ type: "UNREAD_COUNT", unreadCount: count });
    }
  };

  const refreshUnread = async () => {
    try {
      const res = await api.get("/messages/unread-count");
      updateUnread(res.data);
    } catch (error) {
      const status = error?.response?.status;
      if (status === 401 || status === 403) {
        closeSse();
        stopPolling();
        updateUnread(0);
      }
    }
  };

  const startPolling = () => {
    if (pollerRef.current) {
      return;
    }
    refreshUnread();
    pollerRef.current = setInterval(refreshUnread, 20000);
  };

  const onSseMessage = (event) => {
    if (!event.data) {
      return;
    }
    try {
      const data = JSON.parse(event.data);
      if (typeof data.unreadCount === "number") {
        updateUnread(data.unreadCount);
      }
      if (data.type === "NEW_MESSAGE") {
        emitToast(`${data.sender}님이 새 쪽지를 보냈습니다: ${data.title}`, "info", 3500);
        emitMessageEvent(data);
      }
      if (data.type === "UNREAD_COUNT") {
        emitMessageEvent(data);
      }
    } catch {
      return;
    }
  };

  const startSse = () => {
    closeSse();

    try {
      const source = new EventSource(`${API_BASE_URL}/messages/stream?token=${token}`);
      sseRef.current = source;
      source.addEventListener("notification", onSseMessage);
      source.onmessage = onSseMessage;
      source.onopen = () => stopPolling();
      source.onerror = () => {
        closeSse();
        startPolling();
      };
    } catch {
      startPolling();
    }
  };

  useEffect(() => {
    if (!token) {
      closeSse();
      stopPolling();
      unreadRef.current = 0;
      setUnread(0);
      emitMessageEvent({ type: "UNREAD_COUNT", unreadCount: 0 });
      return;
    }

    startPolling();
    startSse();

    return () => {
      closeSse();
      stopPolling();
    };
  }, [token]);

  if (!token) return null;

  return (
    <header className="top-nav">
      <h2>회원제 커뮤니티</h2>
      <nav>
        <NavLink to="/posts">게시판</NavLink>
        <NavLink to="/messages">쪽지함</NavLink>
        <NavLink to="/audit-logs">운영로그</NavLink>
      </nav>
      <div className="user-box">
        <span>{user?.username}</span>
        <span className="badge">{unread}</span>
        <button onClick={logout}>로그아웃</button>
      </div>
    </header>
  );
}

function ToastPortal() {
  const [toast, setToast] = useState(null);
  const timer = useRef(null);

  useEffect(() => {
    const handler = (event) => {
      const item = {
        id: Date.now(),
        message: event?.detail?.message || "알 수 없는 오류가 발생했습니다.",
        type: event?.detail?.type || "error",
        duration: event?.detail?.duration || 3500
      };

      setToast(item);
      if (timer.current) {
        clearTimeout(timer.current);
      }
      timer.current = setTimeout(() => {
        setToast((prev) => (prev?.id === item.id ? null : prev));
      }, item.duration);
    };

    window.addEventListener("app-toast", handler);
    return () => {
      if (timer.current) clearTimeout(timer.current);
      window.removeEventListener("app-toast", handler);
    };
  }, []);

  if (!toast) return null;

  return (
    <div className={`toast-popup toast-${toast.type}`} role="status">
      {toast.message}
    </div>
  );
}

export default function App() {
  const routes = useMemo(() => (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/posts" element={<Protected><PostsPage /></Protected>} />
      <Route path="/posts/:id" element={<Protected><PostDetailPage /></Protected>} />
      <Route path="/messages" element={<Protected><MessagePage /></Protected>} />
      <Route path="/audit-logs" element={<Protected><AuditLogPage /></Protected>} />
      <Route path="*" element={<Navigate to="/posts" replace />} />
    </Routes>
  ), []);

  return (
    <BrowserRouter>
      <ToastPortal />
      <div className="app-shell">
        <NotificationArea />
        {routes}
      </div>
    </BrowserRouter>
  );
}
