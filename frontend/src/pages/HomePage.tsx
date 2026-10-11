import { Link } from 'react-router'
import { postApi, workLogApi } from '../api'
import { ErrorMessage, Loading } from '../components/Status'
import { formatDate, formatYearMonth } from '../utils/format'
import { useAsync } from '../utils/useAsync'

export default function HomePage() {
  const posts = useAsync(() => postApi.list(0), [])
  const workLogs = useAsync(() => workLogApi.list(), [])

  return (
    <>
      <section className="hero">
        <h1>안녕하세요, 동현입니다.</h1>
        <p className="muted">C++/MFC 공장 자동화와 Java/Spring 웹 개발을 하고 있습니다.</p>
      </section>

      <section>
        <div className="section-head">
          <h2>최근 글</h2>
          <Link to="/posts">전체 보기 →</Link>
        </div>
        {posts.loading && <Loading />}
        {posts.error && <ErrorMessage error={posts.error} />}
        <ul className="list">
          {posts.data?.content.slice(0, 5).map((p) => (
            <li key={p.id}>
              <Link to={`/posts/${p.id}`}>{p.title}</Link>
              <span className="muted">{formatDate(p.createdAt)}</span>
            </li>
          ))}
          {posts.data?.content.length === 0 && <li className="muted">아직 작성한 글이 없습니다.</li>}
        </ul>
      </section>

      <section>
        <div className="section-head">
          <h2>최근 작업</h2>
          <Link to="/work-logs">전체 보기 →</Link>
        </div>
        {workLogs.loading && <Loading />}
        {workLogs.error && <ErrorMessage error={workLogs.error} />}
        <ul className="list">
          {workLogs.data?.slice(0, 3).map((w) => (
            <li key={w.id}>
              <span>{w.title}</span>
              <span className="muted">
                {formatYearMonth(w.startDate)} ~ {w.endDate ? formatYearMonth(w.endDate) : '진행 중'}
              </span>
            </li>
          ))}
        </ul>
      </section>
    </>
  )
}
