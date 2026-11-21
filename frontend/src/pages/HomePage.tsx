import { Link } from 'react-router'
import { diaryApi, workLogApi } from '../api'
import { useAuth } from '../auth/AuthContext'
import DiaryList from '../components/DiaryList'
import { ErrorMessage, Loading } from '../components/Status'
import { formatYearMonth } from '../utils/format'
import { useAsync } from '../utils/useAsync'

export default function HomePage() {
  const { isAdmin } = useAuth()
  // 일기는 나만 보는 공간이라 로그인했을 때만 불러온다
  const diary = useAsync(() => (isAdmin ? diaryApi.list(0, 3) : Promise.resolve(null)), [isAdmin])
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

      {isAdmin && (
        <section className="section">
          <div className="section-head">
            <h2>최근 일기</h2>
            <Link to="/diary">전체 보기 →</Link>
          </div>
          {diary.loading && <Loading />}
          {diary.error && <ErrorMessage error={diary.error} />}
          {diary.data && <DiaryList entries={diary.data.content} />}
          {diary.data?.content.length === 0 && (
            <p className="empty">
              아직 쓴 일기가 없습니다. <Link to="/diary/new">오늘 일기 쓰기</Link>
            </p>
          )}
        </section>
      )}

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
