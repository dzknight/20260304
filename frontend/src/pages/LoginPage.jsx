import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../api/http";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
  const { login } = useAuth();
  const nav = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const submit = async (event) => {
    event.preventDefault();
    setError("");
    try {
      const res = await api.post("/auth/login", { username, password });
      login(res.data);
      nav("/posts");
    } catch {
      setError("아이디 또는 비밀번호가 올바르지 않습니다.");
    }
  };

  return (
    <div className="form-shell">
      <form className="auth-form" onSubmit={submit}>
        <h2>로그인</h2>
        {error && <p className="error">{error}</p>}
        <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="아이디" />
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="비밀번호"
        />
        <button type="submit">로그인</button>
        <p>
          계정이 없다면 <Link to="/register">회원가입</Link>
        </p>
      </form>
    </div>
  );
}

