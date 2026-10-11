import { useState, type FormEvent } from 'react'
import { fixedExpenseApi } from '../api'
import { ApiError } from '../api/client'
import { EXPENSE_CATEGORIES, type ExpenseCategory, type FixedExpense, type FixedExpenseRequest } from '../api/types'
import { formatMonthShort, thisMonth, won } from '../utils/expense'
import { useAsync } from '../utils/useAsync'
import { ErrorMessage, Loading } from './Status'

const emptyForm = (): FixedExpenseRequest => ({
  name: '',
  amount: '',
  category: 'HOUSING',
  startMonth: thisMonth(),
  endMonth: '',
  memo: '',
})

const toForm = (f: FixedExpense): FixedExpenseRequest => ({
  name: f.name,
  amount: String(f.amount),
  category: f.category,
  startMonth: f.startMonth,
  endMonth: f.endMonth ?? '',
  memo: f.memo ?? '',
})

const formatPeriod = (f: FixedExpense) =>
  `${formatMonthShort(f.startMonth)} ~ ${f.endMonth ? formatMonthShort(f.endMonth) : '계속'}`

/**
 * 고정지출 관리. 등록해 두면 가계부에서 해당 월을 열 때 매월 1일 날짜로 지출 내역에 자동 반영된다.
 * onChanged: 등록·수정·삭제 후 지출 목록/요약을 다시 불러오게 하는 콜백
 */
export default function FixedExpensePanel({ onChanged }: { onChanged: () => void }) {
  const list = useAsync(() => fixedExpenseApi.list(), [])
  // null: 폼 닫힘, 'new': 신규, number: 수정 대상 id
  const [editing, setEditing] = useState<number | 'new' | null>(null)
  const [form, setForm] = useState<FixedExpenseRequest>(emptyForm)
  const [error, setError] = useState('')

  const active = list.data?.filter((f) => f.active) ?? []
  const monthlyTotal = active.reduce((sum, f) => sum + f.amount, 0)

  const openNew = () => {
    setForm(emptyForm())
    setEditing('new')
    setError('')
  }

  const openEdit = (f: FixedExpense) => {
    setForm(toForm(f))
    setEditing(f.id)
    setError('')
  }

  const set = <K extends keyof FixedExpenseRequest>(key: K, value: FixedExpenseRequest[K]) =>
    setForm((f) => ({ ...f, [key]: value }))

  const changed = () => {
    list.reload()
    onChanged()
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    try {
      if (editing === 'new') await fixedExpenseApi.create(form)
      else if (typeof editing === 'number') await fixedExpenseApi.update(editing, form)
      setEditing(null)
      changed()
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : String(err))
    }
  }

  const handleDelete = async (f: FixedExpense) => {
    if (!confirm(`'${f.name}' 고정지출을 삭제할까요?\n이미 반영된 지난 달 내역은 그대로 남습니다.`)) return
    await fixedExpenseApi.remove(f.id)
    if (editing === f.id) setEditing(null)
    changed()
  }

  return (
    <div className="card fixed-panel">
      <div className="section-head">
        <div>
          <h2 className="card-title">고정지출</h2>
          <p className="muted small">
            매월 1일 날짜로 지출 내역에 자동 반영됩니다. 반영된 내역은 그 달 것만 따로 수정·삭제할 수 있습니다.
          </p>
        </div>
        <button type="button" onClick={openNew}>
          고정지출 등록
        </button>
      </div>

      {list.data && list.data.length > 0 && (
        <div className="fixed-total">
          매월 <strong>{won(monthlyTotal)}</strong> <span className="muted small">(진행 중 {active.length}건)</span>
        </div>
      )}

      {editing !== null && (
        <form className="form fixed-form" onSubmit={handleSubmit}>
          <div className="row">
            <label>
              이름
              <input value={form.name} onChange={(e) => set('name', e.target.value)} maxLength={100} placeholder="예) 월세, 넷플릭스" required autoFocus />
            </label>
            <label>
              금액 (원)
              <input type="number" inputMode="numeric" min="1" step="1" value={form.amount} onChange={(e) => set('amount', e.target.value)} required />
            </label>
            <label>
              카테고리
              <select value={form.category} onChange={(e) => set('category', e.target.value as ExpenseCategory)}>
                {Object.entries(EXPENSE_CATEGORIES).map(([k, v]) => (
                  <option key={k} value={k}>
                    {v}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <div className="row">
            <label>
              시작 월
              <input type="month" value={form.startMonth} onChange={(e) => set('startMonth', e.target.value)} required />
            </label>
            <label>
              종료 월 (비우면 계속)
              <input type="month" value={form.endMonth} min={form.startMonth} onChange={(e) => set('endMonth', e.target.value)} />
            </label>
          </div>
          <label>
            메모
            <input value={form.memo} onChange={(e) => set('memo', e.target.value)} maxLength={500} />
          </label>
          {editing !== 'new' && (
            <p className="muted small">수정 내용은 아직 반영되지 않은 달부터 적용됩니다. 이미 반영된 달은 지출 목록에서 직접 고쳐 주세요.</p>
          )}
          {error && <p className="error pre">{error}</p>}
          <div className="row end">
            <button type="button" className="secondary" onClick={() => setEditing(null)}>
              취소
            </button>
            <button type="submit">저장</button>
          </div>
        </form>
      )}

      {list.loading && <Loading />}
      {list.error && <ErrorMessage error={list.error} />}
      {list.data && list.data.length === 0 && editing === null && (
        <p className="muted small">등록된 고정지출이 없습니다. 월세·관리비·통신비·구독료처럼 매달 나가는 돈을 등록해 두세요.</p>
      )}
      {list.data && list.data.length > 0 && (
        <ul className="fixed-list">
          {list.data.map((f) => (
            <li key={f.id} className={f.active ? '' : 'inactive'}>
              <span className="fixed-main">
                <strong>{f.name}</strong>
                <span className="muted small">
                  {EXPENSE_CATEGORIES[f.category]} · {formatPeriod(f)}
                  {!f.active && ' · 종료'}
                </span>
              </span>
              <span className="num fixed-amount">{won(f.amount)}</span>
              <span className="actions">
                <button type="button" className="link" onClick={() => openEdit(f)}>
                  수정
                </button>
                <button type="button" className="link danger" onClick={() => handleDelete(f)}>
                  삭제
                </button>
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
