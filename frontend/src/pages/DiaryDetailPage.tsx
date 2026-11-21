import { Link, useNavigate, useParams } from 'react-router'
import { diaryApi } from '../api'
import Markdown from '../components/Markdown'
import { ErrorMessage, Loading } from '../components/Status'
import { formatDiaryDate } from '../utils/diary'
import { useAsync } from '../utils/useAsync'

export default function DiaryDetailPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const { data: entry, error, loading } = useAsync(() => diaryApi.get(id), [id])

  const handleDelete = async () => {
    if (!confirm('이 일기를 삭제할까요?')) return
    await diaryApi.remove(id)
    navigate('/diary', { replace: true })
  }

  if (loading) return <Loading />
  if (error) return <ErrorMessage error={error} />
  if (!entry) return null

  return (
    <article>
      <header className="article-head">
        <p className="diary-date-label">{formatDiaryDate(entry.entryDate)}</p>
        {entry.title && <h1>{entry.title}</h1>}
        <div className="meta">
          <Link to="/diary">← 목록</Link>
          <span className="actions">
            <Link to={`/diary/${id}/edit`}>수정</Link>
            <button type="button" className="link danger" onClick={handleDelete}>
              삭제
            </button>
          </span>
        </div>
      </header>
      <Markdown>{entry.content}</Markdown>
    </article>
  )
}
