import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router'
import { workLogApi } from '../api'
import { ApiError } from '../api/client'

interface Form {
  title: string
  organization: string
  role: string
  startDate: string
  endDate: string
  techStack: string
  content: string
}

const EMPTY: Form = { title: '', organization: '', role: '', startDate: '', endDate: '', techStack: '', content: '' }

export default function WorkLogEditPage() {
  const params = useParams()
  const id = params.id ? Number(params.id) : null
  const navigate = useNavigate()
  const [form, setForm] = useState<Form>(EMPTY)
  const [error, setError] = useState('')

  useEffect(() => {
    if (id == null) return
    workLogApi
      .get(id)
      .then((w) =>
        setForm({
          title: w.title,
          organization: w.organization ?? '',
          role: w.role ?? '',
          startDate: w.startDate,
          endDate: w.endDate ?? '',
          techStack: w.techStack ?? '',
          content: w.content ?? '',
        }),
      )
      .catch((e: Error) => setError(e.message))
  }, [id])

  const set = (key: keyof Form, value: string) => setForm((f) => ({ ...f, [key]: value }))

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    // 빈 종료일은 '진행 중'(null)으로 전송
    const body = { ...form, endDate: form.endDate || null }
    try {
      if (id == null) await workLogApi.create(body)
      else await workLogApi.update(id, body)
      navigate('/work-logs', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : String(err))
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <h1>{id == null ? '작업 기록 추가' : '작업 기록 수정'}</h1>
      <label>
        프로젝트명
        <input value={form.title} onChange={(e) => set('title', e.target.value)} maxLength={200} required />
      </label>
      <div className="row">
        <label>
          회사 / 소속
          <input value={form.organization} onChange={(e) => set('organization', e.target.value)} maxLength={100} />
        </label>
        <label>
          역할
          <input value={form.role} onChange={(e) => set('role', e.target.value)} maxLength={100} />
        </label>
      </div>
      <div className="row">
        <label>
          시작일
          <input type="date" value={form.startDate} onChange={(e) => set('startDate', e.target.value)} required />
        </label>
        <label>
          종료일 (비우면 진행 중)
          <input type="date" value={form.endDate} onChange={(e) => set('endDate', e.target.value)} />
        </label>
      </div>
      <label>
        기술 스택 (쉼표 구분)
        <input
          value={form.techStack}
          onChange={(e) => set('techStack', e.target.value)}
          placeholder="C++, MFC, Spring Boot"
          maxLength={500}
        />
      </label>
      <label>
        상세 내용 (마크다운)
        <textarea value={form.content} onChange={(e) => set('content', e.target.value)} rows={12} />
      </label>
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
