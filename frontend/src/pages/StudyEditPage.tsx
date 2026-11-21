import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router'
import { studyApi } from '../api'
import { ApiError } from '../api/client'
import { STUDY_CATEGORIES, STUDY_TYPE_KEYS, STUDY_TYPES, type StudyCategory, type StudyType } from '../api/types'
import Markdown from '../components/Markdown'

interface Form {
  category: StudyCategory
  /** 기술·업무일 때만 의미가 있다 (여러 개) */
  types: StudyType[]
  question: string
  answer: string
  sortOrder: string
}

const EMPTY: Form = { category: 'INTERVIEW', types: [], question: '', answer: '', sortOrder: '0' }

export default function StudyEditPage() {
  const params = useParams()
  const id = params.id ? Number(params.id) : null
  const navigate = useNavigate()
  const [form, setForm] = useState<Form>(EMPTY)
  const [preview, setPreview] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (id == null) return
    studyApi
      .get(id)
      .then((c) =>
        setForm({ category: c.category, types: c.types, question: c.question, answer: c.answer, sortOrder: String(c.sortOrder) }),
      )
      .catch((e: Error) => setError(e.message))
  }, [id])

  const set = <K extends keyof Form>(key: K, value: Form[K]) => setForm((f) => ({ ...f, [key]: value }))

  /** 유형 칩 토글 (선언 순서 유지) */
  const toggleType = (t: StudyType) =>
    set('types', form.types.includes(t) ? form.types.filter((x) => x !== t) : STUDY_TYPE_KEYS.filter((k) => k === t || form.types.includes(k)))

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    // 면접 질문에는 유형을 보내지 않는다
    const body = { ...form, types: form.category === 'KNOWLEDGE' ? form.types : [], sortOrder: Number(form.sortOrder) || 0 }
    try {
      if (id == null) await studyApi.create(body)
      else await studyApi.update(id, body)
      navigate('/study', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : String(err))
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <h1>{id == null ? '학습 카드 추가' : '학습 카드 수정'}</h1>
      <div className="row">
        <label>
          분류
          <select value={form.category} onChange={(e) => set('category', e.target.value as StudyCategory)}>
            {(Object.keys(STUDY_CATEGORIES) as StudyCategory[]).map((k) => (
              <option key={k} value={k}>
                {STUDY_CATEGORIES[k]}
              </option>
            ))}
          </select>
        </label>
        <label>
          순서 (작을수록 위)
          <input type="number" value={form.sortOrder} onChange={(e) => set('sortOrder', e.target.value)} />
        </label>
      </div>
      {form.category === 'KNOWLEDGE' && (
        <div className="study-type-field">
          <span>유형 (여러 개 선택)</span>
          <div className="study-types" role="group" aria-label="유형">
            {STUDY_TYPE_KEYS.map((t) => (
              <button
                key={t}
                type="button"
                className={`chip${form.types.includes(t) ? ' on' : ''}`}
                aria-pressed={form.types.includes(t)}
                onClick={() => toggleType(t)}
              >
                {STUDY_TYPES[t]}
              </button>
            ))}
          </div>
        </div>
      )}
      <label>
        질문
        <input value={form.question} onChange={(e) => set('question', e.target.value)} maxLength={500} required />
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
        <Markdown>{form.answer || '_내용이 없습니다._'}</Markdown>
      ) : (
        <label>
          답 (마크다운)
          <textarea value={form.answer} onChange={(e) => set('answer', e.target.value)} rows={16} required />
        </label>
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
