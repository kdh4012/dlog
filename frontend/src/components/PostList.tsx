import { Link } from 'react-router'
import type { PostSummary } from '../api/types'
import { formatDate } from '../utils/format'

/** 날짜 · 제목 · 요약 · 카테고리로 구성된 글 목록 (홈, 블로그 목록 공용) */
export default function PostList({ posts }: { posts: PostSummary[] }) {
  return (
    <ul className="post-list">
      {posts.map((p) => (
        <li key={p.id}>
          <time className="post-date">{formatDate(p.createdAt)}</time>
          <div>
            <Link to={`/posts/${p.id}`} className="post-title">
              {!p.published && <span className="badge">비공개</span>}
              {p.title}
            </Link>
            {p.summary && <p className="post-summary">{p.summary}</p>}
            {p.category && (
              <Link to={`/posts?category=${encodeURIComponent(p.category)}`} className="tag">
                {p.category}
              </Link>
            )}
          </div>
        </li>
      ))}
    </ul>
  )
}
