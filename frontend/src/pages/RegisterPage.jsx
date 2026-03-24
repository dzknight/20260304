import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../api/http";
import { useAuth } from "../context/AuthContext";

export default function RegisterPage() {
  const { login } = useAuth();
  const nav = useNavigate();
  const [form, setForm] = useState({
    username: "",
    email: "",
    nickname: "",
    password: ""
  });
  const [error, setError] = useState("");

  const onChange = (key, value) => setForm((prev) => ({ ...prev, [key]: value }));

  const submit = async (event) => {
    event.preventDefault();
    setError("");
    try {
      const res = await api.post("/auth/register", form);
      login(res.data);
      nav("/posts");
    } catch (err) {
      setError(err?.response?.data?.message || "회원가입에 실패했습니다.");
    }
  };

  return (
    <div className="form-shell">
      <form className="auth-form" onSubmit={submit}>
        <h2>회원가입</h2>
        {error && <p className="error">{error}</p>}
        <input value={form.username} onChange={(e) => onChange("username", e.target.value)} placeholder="아이디" />
        <input value={form.email} onChange={(e) => onChange("email", e.target.value)} placeholder="이메일" />
        <input value={form.nickname} onChange={(e) => onChange("nickname", e.target.value)} placeholder="닉네임" />
        <input
          type="password"
          value={form.password}
          onChange={(e) => onChange("password", e.target.value)}
          placeholder="비밀번호"
        />
        <button type="submit">가입</button>
        <p>
          계정이 있다면 <Link to="/login">로그인</Link>
        </p>
      </form>
    </div>
  );
}

