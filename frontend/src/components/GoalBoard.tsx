import { useState, type DragEvent, type FormEvent, type KeyboardEvent } from 'react'
import { goalApi } from '../api'
import { ApiError } from '../api/client'
import type { Goal } from '../api/types'
import { useAsync } from '../utils/useAsync'
import { ErrorMessage, Loading } from './Status'

/** 올해 (한국 시간) */
const thisYear = () => Number(new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul', year: 'numeric' }).format(new Date()))

const errorText = (err: unknown) => (err instanceof ApiError ? err.detail : String(err))

interface RowProps {
  goal: Goal
  index: number
  isFirst: boolean
  isLast: boolean
  dragging: boolean
  dragOver: boolean
  onToggleDone: () => void
  onMove: (delta: -1 | 1) => void
  onChanged: () => void
  // 드래그 (데스크톱)
  onDragStart: () => void
  onDragOver: (e: DragEvent) => void
  onDrop: () => void
  onDragEnd: () => void
}

/**
 * 목표 한 줄.
 * - 동그라미를 누르면 달성 표시 (번호 → ✓)
 * - 제목을 누르면 그 자리에서 수정 (Enter 저장, Esc 취소)
 * - 데스크톱은 끌어서, 모바일은 ▲▼ 로 순서 변경
 */
function GoalRow(p: RowProps) {
  const { goal } = p
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
      p.onChanged()
    } catch (err) {
      setTitle(goal.title)
      alert(errorText(err))
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
    p.onChanged()
  }

  const classes = ['goal-row', goal.done && 'done', p.dragging && 'dragging', p.dragOver && 'drag-over'].filter(Boolean).join(' ')

  return (
    <li
      className={classes}
      draggable={!editing}
      onDragStart={(e) => {
        e.dataTransfer.effectAllowed = 'move'
        p.onDragStart()
      }}
      onDragOver={p.onDragOver}
      onDrop={(e) => {
        e.preventDefault()
        p.onDrop()
      }}
      onDragEnd={p.onDragEnd}
    >
      <span className="goal-handle" aria-hidden="true" title="끌어서 순서 변경">
        ⠿
      </span>
      <button
        type="button"
        className="goal-check"
        onClick={p.onToggleDone}
        aria-pressed={goal.done}
        aria-label={goal.done ? '달성 취소' : '달성 표시'}
        title={goal.done ? '달성 취소' : '달성 표시'}
      >
        {goal.done ? '✓' : p.index + 1}
      </button>
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
      <span className="goal-move">
        <button type="button" onClick={() => p.onMove(-1)} disabled={p.isFirst} aria-label="위로">
          ▲
        </button>
        <button type="button" onClick={() => p.onMove(1)} disabled={p.isLast} aria-label="아래로">
          ▼
        </button>
      </span>
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
  // 달성 체크·순서 변경을 서버 응답 전에 먼저 보여주기 위한 목록. null 이면 조회 결과 그대로
  const [local, setLocal] = useState<{ year: number; items: Goal[] } | null>(null)
  const [dragId, setDragId] = useState<number | null>(null)
  const [overId, setOverId] = useState<number | null>(null)
  const [draft, setDraft] = useState('')
  const [error, setError] = useState('')

  const list = local?.year === year ? local.items : (goals.data ?? [])
  const doneCount = list.filter((g) => g.done).length

  const reload = () => {
    setLocal(null)
    goals.reload()
  }

  const handleAdd = async (e: FormEvent) => {
    e.preventDefault()
    const title = draft.trim()
    if (!title) return
    try {
      await goalApi.create(year, title)
      setDraft('')
      setError('')
      reload()
    } catch (err) {
      setError(errorText(err))
    }
  }

  const toggleDone = async (goal: Goal) => {
    setLocal({ year, items: list.map((g) => (g.id === goal.id ? { ...g, done: !g.done } : g)) })
    try {
      await goalApi.setDone(goal.id, !goal.done)
    } catch (err) {
      setError(errorText(err))
      reload()
    }
  }

  /** 새 순서를 바로 보여주고 저장. 실패하면 서버 목록으로 되돌린다 */
  const saveOrder = async (next: Goal[]) => {
    setLocal({ year, items: next })
    try {
      setLocal({ year, items: await goalApi.reorder(year, next.map((g) => g.id)) })
    } catch (err) {
      setError(errorText(err))
      reload()
    }
  }

  const move = (index: number, delta: -1 | 1) => {
    const target = index + delta
    if (target < 0 || target >= list.length) return
    const next = [...list]
    ;[next[index], next[target]] = [next[target], next[index]]
    void saveOrder(next)
  }

  /** 끌던 목표를 놓은 줄 자리로 옮긴다 */
  const dropOn = (targetId: number) => {
    const sourceId = dragId
    setDragId(null)
    setOverId(null)
    if (sourceId === null || sourceId === targetId) return
    const source = list.find((g) => g.id === sourceId)
    const targetIndex = list.findIndex((g) => g.id === targetId)
    if (!source || targetIndex < 0) return
    const next = list.filter((g) => g.id !== sourceId)
    next.splice(targetIndex, 0, source)
    void saveOrder(next)
  }

  const changeYear = (next: number) => {
    setLocal(null)
    setYear(next)
  }

  return (
    <section className="goal-board">
      <div className="goal-head">
        <h2>{year}년 목표</h2>
        <div className="goal-year-nav">
          <button type="button" className="secondary" onClick={() => changeYear(year - 1)} aria-label="이전 해">
            ‹
          </button>
          {year !== current && (
            <button type="button" className="link" onClick={() => changeYear(current)}>
              올해
            </button>
          )}
          <button type="button" className="secondary" onClick={() => changeYear(year + 1)} disabled={year > current} aria-label="다음 해">
            ›
          </button>
        </div>
      </div>

      {list.length > 0 && (
        <div className="goal-progress">
          <span>
            <strong>{doneCount}</strong> / {list.length} 달성
          </span>
          <span className="goal-progress-bar" aria-hidden="true">
            <span style={{ width: `${(doneCount / list.length) * 100}%` }} />
          </span>
        </div>
      )}

      {goals.loading && !goals.data && <Loading />}
      {goals.error && <ErrorMessage error={goals.error} />}

      <ol className="goal-list">
        {list.map((g, i) => (
          <GoalRow
            key={g.id}
            goal={g}
            index={i}
            isFirst={i === 0}
            isLast={i === list.length - 1}
            dragging={dragId === g.id}
            dragOver={overId === g.id && dragId !== g.id}
            onToggleDone={() => toggleDone(g)}
            onMove={(d) => move(i, d)}
            onChanged={reload}
            onDragStart={() => setDragId(g.id)}
            onDragOver={(e) => {
              if (dragId === null) return
              e.preventDefault()
              setOverId(g.id)
            }}
            onDrop={() => dropOn(g.id)}
            onDragEnd={() => {
              setDragId(null)
              setOverId(null)
            }}
          />
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
