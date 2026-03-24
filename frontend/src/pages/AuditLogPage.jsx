import { useEffect, useState } from "react";
import api from "../api/http";
import Pager from "../components/Pager";

export default function AuditLogPage() {
  const [logs, setLogs] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const load = async () => {
    const res = await api.get("/audit-logs", {
      params: { page, size: 20 }
    });
    setLogs(res.data.content || []);
    setTotalPages(res.data.totalPages || 0);
  };

  useEffect(() => {
    load();
  }, [page]);

  return (
    <div className="page">
      <h2>운영 로그</h2>
      <p className="muted">삭제/차단 등 운영 이벤트를 시간 순으로 확인합니다.</p>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>시간</th>
              <th>행위자</th>
              <th>액션</th>
              <th>타입</th>
              <th>대상 ID</th>
              <th>대상</th>
              <th>세부</th>
            </tr>
          </thead>
          <tbody>
            {logs.map((log) => (
              <tr key={log.id}>
                <td>{new Date(log.createdAt).toLocaleString()}</td>
                <td>{log.actor}</td>
                <td>{log.action}</td>
                <td>{log.targetType}</td>
                <td>{log.targetId || "-"}</td>
                <td>{log.targetRef || "-"}</td>
                <td>{log.detail || "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <Pager current={page} total={totalPages} onChange={setPage} />
    </div>
  );
}
