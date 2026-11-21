import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router'
import { diaryApi } from '../api'
import { ApiError } from '../api/client'
import type { DiaryRequest } from '../api/types'
import Markdown from '../components/Markdown'
import { todayKst } from '../utils/diary'

/** 새 일기 / 일기 수정 겸용. 날짜는 오늘(한국 시간)이 기본 */
export default function DiaryEditPage() {
  const params = useParams()
  const id = params.id ? Number(params.id) : null
  const navigate = useNavigate()

  const [form, setForm] = useState<DiaryRequest>({ entryDate: todayKst(), title: '', content: '' })
  const [preview, setPreview] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (id == null) return
    diaryApi
      .get(id)
      .then((d) => setForm({ entryDate: d.entryDate, title: d.title ?? '', content: d.content }))
      .catch((e: Error) => setError(e.message))
  }, [id])

  const set = <K extends keyof DiaryRequest>(key: K, value: DiaryRequest[K]) => setForm((f) => ({ ...f, [key]: value }))

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    try {
      const saved = id == null ? await diaryApi.create(form) : await diaryApi.update(id, form)
      navigate(`/diary/${saved.id}`, { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : String(err))
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <h1>{id == null ? '일기 쓰기' : '일기 수정'}</h1>
      <div className="row">
        <label>
          날짜
          <input type="date" value={form.entryDate} onChange={(e) => set('entryDate', e.target.value)} required />
        </label>
        <label>
          제목 (선택)
          <input value={form.title} onChange={(e) => set('title', e.target.value)} maxLength={200} />
        </label>
      </div>
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
          placeholder="오늘 있었던 일, 생각, 느낀 점을 적어 보세요. (마크다운 사용 가능)"
          required
          autoFocus={id == null}
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
