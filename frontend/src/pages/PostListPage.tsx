import { Link, useSearchParams } from 'react-router'
import { postApi } from '../api'
import { useAuth } from '../auth/AuthContext'
import PostList from '../components/PostList'
import { ErrorMessage, Loading } from '../components/Status'
import { useAsync } from '../utils/useAsync'

export default function PostListPage() {
  const { isAdmin } = useAuth()
  const [params, setParams] = useSearchParams()
  const page = Number(params.get('page') ?? 0)
  const category = params.get('category') ?? undefined

  // isAdmin 이 바뀌면 비공개 글 포함 여부가 달라지므로 재조회
  const { data, error, loading } = useAsync(() => postApi.list(page, category), [page, category, isAdmin])

  const goPage = (p: number) => {
    const next = new URLSearchParams(params)
    next.set('page', String(p))
    setParams(next)
  }

  return (
    <>
      <div className="section-head">
        <h1>블로그{category && <small className="muted"> / {category}</small>}</h1>
        {isAdmin && (
          <Link to="/posts/new" className="button">
            새 글
          </Link>
        )}
      </div>

      {loading && <Loading />}
      {error && <ErrorMessage error={error} />}

      {data && <PostList posts={data.content} />}
      {data?.content.length === 0 && <p className="empty">글이 없습니다.</p>}

      {data && data.totalPages > 1 && (
        <div className="pager">
          <button type="button" disabled={page <= 0} onClick={() => goPage(page - 1)}>
            이전
          </button>
          <span>
            {page + 1} / {data.totalPages}
          </span>
          <button type="button" disabled={page + 1 >= data.totalPages} onClick={() => goPage(page + 1)}>
            다음
          </button>
        </div>
      )}
    </>
  )
}
