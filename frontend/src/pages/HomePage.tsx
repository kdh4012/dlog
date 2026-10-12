import { Link } from 'react-router'
import { postApi, workLogApi } from '../api'
import PostList from '../components/PostList'
import { ErrorMessage, Loading } from '../components/Status'
import { formatYearMonth } from '../utils/format'
import { useAsync } from '../utils/useAsync'

export default function HomePage() {
  const posts = useAsync(() => postApi.list(0), [])
  const workLogs = useAsync(() => workLogApi.list(), [])

  return (
    <>
      <section className="hero">
        <h1>
          만들고, 고치고,
          <br />
          기록합니다.
        </h1>
        <p className="muted">현장 자동화부터 웹 서비스까지, 개발하며 배운 것들을 남기는 공간입니다.</p>
      </section>

      <section className="section">
        <div className="section-head">
          <h2>최근 글</h2>
          <Link to="/posts">전체 보기 →</Link>
        </div>
        {posts.loading && <Loading />}
        {posts.error && <ErrorMessage error={posts.error} />}
        {posts.data && <PostList posts={posts.data.content.slice(0, 5)} />}
        {posts.data?.content.length === 0 && <p className="empty">아직 작성한 글이 없습니다.</p>}
      </section>

      <section className="section">
        <div className="section-head">
          <h2>작업 이력</h2>
          <Link to="/work-logs">전체 보기 →</Link>
        </div>
        {workLogs.loading && <Loading />}
        {workLogs.error && <ErrorMessage error={workLogs.error} />}
        <ul className="work-list">
          {workLogs.data?.slice(0, 3).map((w) => (
            <li key={w.id}>
              <strong>{w.title}</strong>
              <span className="muted">
                {formatYearMonth(w.startDate)} ~ {w.endDate ? formatYearMonth(w.endDate) : '진행 중'}
              </span>
            </li>
          ))}
        </ul>
        {workLogs.data?.length === 0 && <p className="empty">작업 이력이 없습니다.</p>}
      </section>
    </>
  )
}
