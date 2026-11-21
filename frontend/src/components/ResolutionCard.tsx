import { useState } from 'react'
import { resolutionApi } from '../api'
import { ApiError } from '../api/client'
import { useAsync } from '../utils/useAsync'
import { ErrorMessage } from './Status'

/** 홈 화면 맨 위의 다짐. 눌러서 바로 고치고 저장한다 (비우고 저장하면 지움) */
export default function ResolutionCard() {
  const resolution = useAsync(() => resolutionApi.get(), [])
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const content = resolution.data?.content ?? ''

  const startEdit = () => {
    setDraft(content)
    setError('')
    setEditing(true)
  }

  const save = async () => {
    setSaving(true)
    try {
      await resolutionApi.save(draft)
      setEditing(false)
      resolution.reload()
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : String(err))
    } finally {
      setSaving(false)
    }
  }

  if (resolution.error) return <ErrorMessage error={resolution.error} />
  if (!resolution.data) return null

  return (
    <section className="resolution">
      <div className="resolution-label">나의 다짐</div>
      {editing ? (
        <>
          <textarea
            className="resolution-input"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => {
              // Ctrl(⌘)+Enter 로 저장, Esc 로 취소 (그냥 Enter 는 줄바꿈)
              if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) void save()
              if (e.key === 'Escape') setEditing(false)
            }}
            placeholder="스스로에게 하는 약속을 적어 보세요."
            maxLength={1000}
            rows={3}
            autoFocus
          />
          {error && <p className="error small">{error}</p>}
          <div className="resolution-actions">
            <button type="button" className="secondary" onClick={() => setEditing(false)}>
              취소
            </button>
            <button type="button" onClick={save} disabled={saving}>
              저장
            </button>
          </div>
        </>
      ) : content ? (
        <button type="button" className="resolution-text" onClick={startEdit} title="눌러서 수정">
          {content}
        </button>
      ) : (
        <button type="button" className="resolution-text empty" onClick={startEdit}>
          + 나만의 다짐을 적어 보세요
        </button>
      )}
    </section>
  )
}
