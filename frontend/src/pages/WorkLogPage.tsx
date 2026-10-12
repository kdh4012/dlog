import { Link } from 'react-router'
import { workLogApi } from '../api'
import { useAuth } from '../auth/AuthContext'
import Markdown from '../components/Markdown'
import ProjectCards from '../components/ProjectCards'
import { ErrorMessage, Loading } from '../components/Status'
import { formatYearMonth } from '../utils/format'
import { PROJECTS } from '../data/projects'
import { useAsync } from '../utils/useAsync'

export default function WorkLogPage() {
  const { isAdmin } = useAuth()
  const { data, error, loading, reload } = useAsync(() => workLogApi.list(), [])

  const handleDelete = async (id: number) => {
    if (!confirm('이 작업 이력을 삭제할까요?')) return
    await workLogApi.remove(id)
    reload()
  }

  return (
    <>
      <h1>프로젝트</h1>

      <section className="section">
        <div className="section-head">
          <h2>개인 프로젝트</h2>
        </div>
        <ProjectCards projects={PROJECTS} />
      </section>

      <div className="section-head">
        <h2>작업 이력</h2>
        {isAdmin && (
          <Link to="/work-logs/new" className="button">
            추가
          </Link>
        )}
      </div>

      {loading && <Loading />}
      {error && <ErrorMessage error={error} />}

      <ol className="timeline">
        {data?.map((w) => (
          <li key={w.id}>
            <div className="period">
              {formatYearMonth(w.startDate)} ~ {w.endDate ? formatYearMonth(w.endDate) : '진행 중'}
            </div>
            <div className="body">
              <h3>{w.title}</h3>
              <p className="muted">{[w.organization, w.role].filter(Boolean).join(' · ')}</p>
              {w.techStack && (
                <div className="tags">
                  {w.techStack
                    .split(',')
                    .map((t) => t.trim())
                    .filter(Boolean)
                    .map((t) => (
                      <span key={t} className="tag">
                        {t}
                      </span>
                    ))}
                </div>
              )}
              {w.content && <Markdown>{w.content}</Markdown>}
              {isAdmin && (
                <div className="actions">
                  <Link to={`/work-logs/${w.id}/edit`}>수정</Link>
                  <button type="button" className="link danger" onClick={() => handleDelete(w.id)}>
                    삭제
                  </button>
                </div>
              )}
            </div>
          </li>
        ))}
      </ol>
      {data?.length === 0 && <p className="muted">작업 이력이 없습니다.</p>}
    </>
  )
}
