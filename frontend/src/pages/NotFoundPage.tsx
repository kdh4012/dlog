import { Link } from 'react-router'

export default function NotFoundPage() {
  return (
    <div className="center">
      <h1>404</h1>
      <p className="muted">페이지를 찾을 수 없습니다.</p>
      <Link to="/">홈으로</Link>
    </div>
  )
}
