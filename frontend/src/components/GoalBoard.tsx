import { useState, type FormEvent, type KeyboardEvent } from 'react'
import { goalApi } from '../api'
import { ApiError } from '../api/client'
import type { Goal } from '../api/types'
import { ErrorMessage, Loading } from './Status'
import { useAsync } from '../utils/useAsync'

/** 올해 (한국 시간) */
const thisYear = () => Number(new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul', year: 'numeric' }).format(new Date()))

/** 한 줄: 제목을 누르면 그 자리에서 고치고(Enter 저장, Esc 취소), 오른쪽 × 로 삭제 */
function GoalRow({ goal, index, onChanged }: { goal: Goal; index: number; onChanged: () => void }) {
  const [editing, setEditing] = useState(false)
  const [title, setTitle] = useState(goal.title)

  const save = async () => {
    const next = title.trim()
    setEditing(false)
    if (!next || next === goal.title) {
      setTitle(goal.title)
      return
    }
    try {
      await goalApi.update(goal.id, next)
      onChanged()
    } catch (err) {
      setTitle(goal.title)
      alert(err instanceof ApiError ? err.detail : String(err))
    }
  }

  const handleKey = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') e.currentTarget.blur() // blur 에서 저장
    if (e.key === 'Escape') {
      setTitle(goal.title)
      setEditing(false)
    }
  }

  const handleDelete = async () => {
    if (!confirm(`'${goal.title}' 목표를 삭제할까요?`)) return
    await goalApi.remove(goal.id)
    onChanged()
  }

  return (
    <li className="goal-row">
      <span className="goal-no">{index + 1}</span>
      {editing ? (
        <input
          className="goal-edit"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          onBlur={save}
          onKeyDown={handleKey}
          maxLength={200}
          autoFocus
          aria-label="목표 수정"
        />
      ) : (
        <button type="button" className="goal-title" onClick={() => setEditing(true)} title="눌러서 수정">
          {goal.title}
        </button>
      )}
      <button type="button" className="goal-delete" onClick={handleDelete} aria-label="삭제" title="삭제">
        ×
      </button>
    </li>
  )
}

/** 홈 화면의 연도별 목표 목록. 연도를 넘겨 지난 목표도 본다 (내년까지) */
export default function GoalBoard() {
  const current = thisYear()
  const [year, setYear] = useState(current)
  const goals = useAsync(() => goalApi.list(year), [year])
  const [draft, setDraft] = useState('')
  const [error, setError] = useState('')

  const handleAdd = async (e: FormEvent) => {
    e.preventDefault()
    const title = draft.trim()
    if (!title) return
    try {
      await goalApi.create(year, title)
      setDraft('')
      setError('')
      goals.reload()
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : String(err))
    }
  }

  const list = goals.data ?? []

  return (
    <section className="goal-board">
      <div className="goal-head">
        <h2>
          {year}년 목표 {list.length > 0 && <span className="goal-count">{list.length}</span>}
        </h2>
        <div className="goal-year-nav">
          <button type="button" className="secondary" onClick={() => setYear((y) => y - 1)} aria-label="이전 해">
            ‹
          </button>
          {year !== current && (
            <button type="button" className="link" onClick={() => setYear(current)}>
              올해
            </button>
          )}
          <button type="button" className="secondary" onClick={() => setYear((y) => y + 1)} disabled={year > current} aria-label="다음 해">
            ›
          </button>
        </div>
      </div>

      {goals.loading && !goals.data && <Loading />}
      {goals.error && <ErrorMessage error={goals.error} />}

      <ol className="goal-list">
        {list.map((g, i) => (
          <GoalRow key={g.id} goal={g} index={i} onChanged={goals.reload} />
        ))}
      </ol>
      {goals.data && list.length === 0 && <p className="goal-empty">아직 등록한 목표가 없습니다.</p>}

      <form className="goal-add" onSubmit={handleAdd}>
        <input value={draft} onChange={(e) => setDraft(e.target.value)} placeholder="새 목표 입력 후 Enter" maxLength={200} aria-label="새 목표" />
        <button type="submit" disabled={!draft.trim()}>
          추가
        </button>
      </form>
      {error && <p className="error small">{error}</p>}
    </section>
  )
}
