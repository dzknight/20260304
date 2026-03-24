export default function Pager({ current, total, onChange }) {
  const prev = Math.max(current - 1, 0);
  const next = Math.min(current + 1, Math.max(total - 1, 0));
  if (total <= 0) return null;
  return (
    <div className="pager">
      <button disabled={current === 0} onClick={() => onChange(prev)}>
        이전
      </button>
      <span>{current + 1}/{total}</span>
      <button disabled={current >= total - 1} onClick={() => onChange(next)}>
        다음
      </button>
    </div>
  );
}

