import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { diaryApi } from '../api'
import type { DiarySummary } from '../api/types'
import DiaryList from '../components/DiaryList'
import { ErrorMessage, Loading } from '../components/Status'

/** 나만 보는 일기 목록. '더 보기'로 이전 일기를 이어서 불러온다 */
export default function DiaryListPage() {
  const [entries, setEntries] = useState<DiarySummary[]>([])
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<Error | null>(null)

  useEffect(() => {
    let alive = true
    diaryApi
      .list(page)
      .then((res) => {
        if (!alive) return
        // 첫 페이지는 새로 받고, 이후 페이지는 뒤에 붙인다
        setEntries((prev) => (page === 0 ? res.content : [...prev, ...res.content]))
        setHasMore(res.page + 1 < res.totalPages)
      })
      .catch((e: Error) => alive && setError(e))
      .finally(() => alive && setLoading(false))
    return () => {
      alive = false
    }
  }, [page])

  return (
    <>
      <div className="section-head">
        <h1>일기</h1>
        <Link to="/diary/new" className="button">
          일기 쓰기
        </Link>
      </div>

      {error && <ErrorMessage error={error} />}
      {!loading && entries.length === 0 && !error && <p className="empty">아직 쓴 일기가 없습니다. 오늘 하루를 남겨 보세요.</p>}
      <DiaryList entries={entries} />
      {loading && <Loading />}
      {hasMore && !loading && (
        <div className="pager">
          <button
            type="button"
            className="secondary"
            onClick={() => {
              setLoading(true)
              setPage((p) => p + 1)
            }}
          >
            이전 일기 더 보기
          </button>
        </div>
      )}
    </>
  )
}
