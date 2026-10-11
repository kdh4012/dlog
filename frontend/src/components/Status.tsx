export function Loading() {
  return <p className="muted">불러오는 중...</p>
}

export function ErrorMessage({ error }: { error: Error }) {
  return <p className="error">{error.message}</p>
}
