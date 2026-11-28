import { useEffect, useRef, useState } from 'react'
import { assetApi } from '../api'
import { ApiError } from '../api/client'
import { useAsync } from '../utils/useAsync'

/**
 * 제목 옆 작은 메모 아이콘. 마우스를 올리면(데스크톱) 메모가 말풍선으로 보이고,
 * 누르면 고정되어 모바일에서도 읽고 고칠 수 있다.
 */
export default function AssetMemo() {
  const memo = useAsync(() => assetApi.memo(), [])
  const [hover, setHover] = useState(false)
  const [pinned, setPinned] = useState(false)
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const boxRef = useRef<HTMLDivElement>(null)

  const content = memo.data?.content ?? ''
  const open = pinned || editing || hover

  // 고정된 말풍선은 바깥을 누르면 닫는다 (편집 중이면 유지)
  useEffect(() => {
    if (!pinned || editing) return
    const close = (e: PointerEvent) => {
      if (boxRef.current && !boxRef.current.contains(e.target as Node)) setPinned(false)
    }
    document.addEventListener('pointerdown', close)
    return () => document.removeEventListener('pointerdown', close)
  }, [pinned, editing])

  const startEdit = () => {
    setDraft(content)
    setError('')
    setEditing(true)
  }

  const save = async () => {
    setSaving(true)
    setError('')
    try {
      await assetApi.saveMemo(draft)
      memo.reload()
      setEditing(false)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : String(err))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="memo" ref={boxRef} onMouseEnter={() => setHover(true)} onMouseLeave={() => setHover(false)}>
      <button
        type="button"
        className={`memo-btn${content ? ' has' : ''}`}
        aria-label="자산 메모"
        aria-expanded={open}
        onClick={() => (content ? setPinned((p) => !p) : startEdit())}
      >
        <svg viewBox="0 0 20 20" aria-hidden="true">
          <path d="M5 3.5h10a1.5 1.5 0 0 1 1.5 1.5v7.4L12.4 16.5H5A1.5 1.5 0 0 1 3.5 15V5A1.5 1.5 0 0 1 5 3.5Z" />
          <path d="M12.4 16.5V13a.5.5 0 0 1 .5-.5h3.6M6.8 7.5h6.4M6.8 10.2h4" />
        </svg>
      </button>

      {open && (
        <div className="memo-pop" role="dialog" aria-label="자산 메모">
          {editing ? (
            <>
              <textarea
                className="memo-input"
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
                placeholder="예) SOXL 1억 되면 절반 팔아서 채권·금 매수"
                maxLength={2000}
                rows={5}
                autoFocus
              />
              {error && <p className="error small">{error}</p>}
              <div className="memo-actions">
                <button type="button" className="link" onClick={() => setEditing(false)} disabled={saving}>
                  취소
                </button>
                <button type="button" onClick={save} disabled={saving}>
                  {saving ? '저장 중...' : '저장'}
                </button>
              </div>
            </>
          ) : (
            <>
              <p className="memo-text">{content || '메모가 없습니다.'}</p>
              <div className="memo-actions">
                <button type="button" className="link" onClick={startEdit}>
                  {content ? '수정' : '작성'}
                </button>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  )
}
