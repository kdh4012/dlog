import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router'
import { postApi } from '../api'
import { ApiError } from '../api/client'
import type { PostRequest } from '../api/types'
import Markdown from '../components/Markdown'

const EMPTY: PostRequest = { title: '', summary: '', content: '', category: '', published: false }

/** 새 글 작성 / 기존 글 수정 겸용 */
export default function PostEditPage() {
  const params = useParams()
  const id = params.id ? Number(params.id) : null
  const navigate = useNavigate()

  const [form, setForm] = useState<PostRequest>(EMPTY)
  const [preview, setPreview] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (id == null) return
    postApi
      .get(id)
      .then((p) =>
        setForm({
          title: p.title,
          summary: p.summary ?? '',
          content: p.content,
          category: p.category ?? '',
          published: p.published,
        }),
      )
      .catch((e: Error) => setError(e.message))
  }, [id])

  const set = <K extends keyof PostRequest>(key: K, value: PostRequest[K]) => setForm((f) => ({ ...f, [key]: value }))

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    try {
      const saved = id == null ? await postApi.create(form) : await postApi.update(id, form)
      navigate(`/posts/${saved.id}`, { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : String(err))
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <h1>{id == null ? '새 글' : '글 수정'}</h1>
      <label>
        제목
        <input value={form.title} onChange={(e) => set('title', e.target.value)} maxLength={200} required />
      </label>
      <div className="row">
        <label>
          카테고리
          <input value={form.category} onChange={(e) => set('category', e.target.value)} maxLength={50} />
        </label>
        <label className="checkbox">
          <input type="checkbox" checked={form.published} onChange={(e) => set('published', e.target.checked)} />
          공개
        </label>
      </div>
      <label>
        요약
        <input value={form.summary} onChange={(e) => set('summary', e.target.value)} maxLength={500} />
      </label>
      <div className="tabs">
        <button type="button" className={preview ? '' : 'active'} onClick={() => setPreview(false)}>
          작성
        </button>
        <button type="button" className={preview ? 'active' : ''} onClick={() => setPreview(true)}>
          미리보기
        </button>
      </div>
      {preview ? (
        <Markdown>{form.content}</Markdown>
      ) : (
        <textarea
          value={form.content}
          onChange={(e) => set('content', e.target.value)}
          rows={20}
          placeholder="마크다운으로 작성하세요."
          required
        />
      )}
      {error && <p className="error pre">{error}</p>}
      <div className="row end">
        <button type="button" className="secondary" onClick={() => navigate(-1)}>
          취소
        </button>
        <button type="submit">저장</button>
      </div>
    </form>
  )
}
