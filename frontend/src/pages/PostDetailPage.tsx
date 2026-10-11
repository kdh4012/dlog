import { Link, useNavigate, useParams } from 'react-router'
import { postApi } from '../api'
import { useAuth } from '../auth/AuthContext'
import Markdown from '../components/Markdown'
import { ErrorMessage, Loading } from '../components/Status'
import { formatDate } from '../utils/format'
import { useAsync } from '../utils/useAsync'

export default function PostDetailPage() {
  const id = Number(useParams().id)
  const { isAdmin } = useAuth()
  const navigate = useNavigate()
  const { data: post, error, loading } = useAsync(() => postApi.get(id), [id, isAdmin])

  const handleDelete = async () => {
    if (!confirm('이 글을 삭제할까요?')) return
    await postApi.remove(id)
    navigate('/posts', { replace: true })
  }

  if (loading) return <Loading />
  if (error) return <ErrorMessage error={error} />
  if (!post) return null

  return (
    <article>
      <header className="article-head">
        <h1>
          {!post.published && <span className="badge">비공개</span>}
          {post.title}
        </h1>
        <div className="meta">
          {post.category && <span className="tag">{post.category}</span>}
          <span className="muted">{formatDate(post.createdAt)}</span>
          {isAdmin && (
            <span className="actions">
              <Link to={`/posts/${id}/edit`}>수정</Link>
              <button type="button" className="link danger" onClick={handleDelete}>
                삭제
              </button>
            </span>
          )}
        </div>
      </header>
      <Markdown>{post.content}</Markdown>
    </article>
  )
}
