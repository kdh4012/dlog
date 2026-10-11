import { useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router'
import { expenseApi } from '../api'
import { ApiError } from '../api/client'
import {
  EXPENSE_CATEGORIES,
  PAYMENT_METHODS,
  VARIABLE_CATEGORIES,
  type Expense,
  type ExpenseCategory,
  type ExpenseKind,
  type ExpenseRequest,
  type ExpenseSummary,
  type PaymentMethod,
} from '../api/types'
import FixedExpensePanel from '../components/FixedExpensePanel'
import { ErrorMessage, Loading } from '../components/Status'
import { formatRate } from '../utils/format'
import { formatMonthLabel, isValidMonth, pad, shiftMonth, thisMonth, today, won } from '../utils/expense'
import { useAsync } from '../utils/useAsync'

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

/** 2026-10-05 → 10.05 (일) */
const formatDay = (iso: string) => {
  const [y, m, d] = iso.split('-').map(Number)
  return `${pad(m)}.${pad(d)} (${WEEKDAYS[new Date(y, m - 1, d).getDay()]})`
}

const emptyForm = (month: string): ExpenseRequest => ({
  // 지금 보고 있는 달이 이번 달이면 오늘, 다른 달이면 그 달 1일
  spentOn: today().startsWith(month) ? today() : `${month}-01`,
  amount: '',
  category: 'FOOD',
  paymentMethod: 'CARD',
  merchant: '',
  memo: '',
})

const toForm = (e: Expense): ExpenseRequest => ({
  spentOn: e.spentOn,
  amount: String(e.amount),
  category: e.category,
  paymentMethod: e.paymentMethod ?? '',
  merchant: e.merchant ?? '',
  memo: e.memo ?? '',
})

/** 이번 달 합계 · 전월 대비 · 예산 사용률 */
function SummaryCard({ summary, onBudgetSaved }: { summary: ExpenseSummary; onBudgetSaved: () => void }) {
  const [editingBudget, setEditingBudget] = useState(false)
  const [budgetInput, setBudgetInput] = useState('')
  const [error, setError] = useState('')

  const { month, total, fixedTotal, variableTotal, count, previousTotal, changeRate, budget, budgetMonth, budgetUsedRate } = summary
  const fixedWeight = total > 0 ? (fixedTotal / total) * 100 : 0
  const remaining = budget != null ? budget - total : null
  // 이전 달에서 이어받은 예산인지 (해당 월에 직접 설정한 값이 아님)
  const inherited = budgetMonth != null && budgetMonth !== month

  const openBudget = () => {
    setBudgetInput(budget != null && !inherited ? String(budget) : '')
    setEditingBudget(true)
    setError('')
  }

  const saveBudget = async (e: FormEvent) => {
    e.preventDefault()
    try {
      await expenseApi.setBudget(month, Number(budgetInput || 0))
      setEditingBudget(false)
      onBudgetSaved()
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : String(err))
    }
  }

  return (
    <div className="card summary total">
      <div className="muted">{formatMonthLabel(month)} 지출</div>
      <div className="big">{won(total)}</div>
      <div className="muted small">
        {count}건 · 전월 {won(previousTotal)}
        {changeRate != null && (
          // 지출은 늘면 나쁜 신호라 증가를 빨강(up), 감소를 파랑(down)으로 표시
          <span className={changeRate > 0 ? 'up' : changeRate < 0 ? 'down' : ''}> ({formatRate(changeRate)})</span>
        )}
      </div>

      {/* 고정 / 변동 비율 */}
      <div className="kind-split">
        <span className="kind-bar" aria-hidden="true">
          <span className="fixed" style={{ width: `${fixedWeight}%` }} />
        </span>
        <div className="kind-legend">
          <span>
            <i className="dot fixed" />
            고정 {won(fixedTotal)}
          </span>
          <span>
            <i className="dot variable" />
            변동 {won(variableTotal)}
          </span>
        </div>
      </div>

      <div className="budget">
        {budget != null && budgetUsedRate != null ? (
          <>
            <div className="budget-head">
              <span>
                예산 {won(budget)}
                {inherited && <span className="muted small"> ({budgetMonth} 설정 이어서 적용)</span>}
              </span>
              <span className={remaining != null && remaining < 0 ? 'up' : ''}>
                {remaining != null && remaining < 0 ? `${won(-remaining)} 초과` : `${won(remaining ?? 0)} 남음`}
              </span>
            </div>
            <span className={`bar budget-bar${budgetUsedRate > 100 ? ' over' : ''}`}>
              <span style={{ width: `${Math.min(budgetUsedRate, 100)}%` }} />
            </span>
            <div className="muted small">사용률 {budgetUsedRate.toFixed(1)}%</div>
          </>
        ) : (
          <div className="muted small">설정된 예산이 없습니다.</div>
        )}

        {editingBudget ? (
          <form className="budget-form" onSubmit={saveBudget}>
            <input
              type="number"
              inputMode="numeric"
              min="0"
              step="1"
              placeholder="월 예산 (0 또는 비우면 삭제)"
              value={budgetInput}
              onChange={(e) => setBudgetInput(e.target.value)}
              autoFocus
            />
            <button type="submit">저장</button>
            <button type="button" className="secondary" onClick={() => setEditingBudget(false)}>
              취소
            </button>
          </form>
        ) : (
          <button type="button" className="link" onClick={openBudget}>
            {formatMonthLabel(month)} 예산 설정
          </button>
        )}
        {error && <p className="error pre">{error}</p>}
      </div>
    </div>
  )
}

/** 카테고리별 비중 */
function CategoryWeights({ items }: { items: ExpenseSummary['byCategory'] }) {
  if (items.length === 0) return null
  return (
    <div className="card">
      <h2 className="card-title">카테고리별</h2>
      <ul className="weights wide">
        {items.map((c) => (
          <li key={c.category}>
            <span>{EXPENSE_CATEGORIES[c.category]}</span>
            <span className="bar">
              <span style={{ width: `${c.weight}%` }} />
            </span>
            <span className="small num">{won(c.amount)}</span>
            <span className="small muted num">{c.weight.toFixed(1)}%</span>
          </li>
        ))}
      </ul>
    </div>
  )
}

/** 최근 6개월 추이. 막대를 누르면 그 달로 이동 */
function MonthlyTrend({
  trend,
  current,
  onSelect,
}: {
  trend: ExpenseSummary['trend']
  current: string
  onSelect: (month: string) => void
}) {
  const max = Math.max(...trend.map((t) => t.total), 1)
  return (
    <div className="card">
      <h2 className="card-title">최근 6개월</h2>
      <div className="trend">
        {trend.map((t) => (
          <button
            key={t.month}
            type="button"
            className={`trend-item${t.month === current ? ' active' : ''}`}
            onClick={() => onSelect(t.month)}
            title={`${formatMonthLabel(t.month)} ${won(t.total)}`}
          >
            <span className="trend-amount">{t.total > 0 ? `${Math.round(t.total / 10_000).toLocaleString('ko-KR')}만` : '-'}</span>
            <span className="trend-bar">
              <span style={{ height: `${(t.total / max) * 100}%` }} />
            </span>
            <span className="trend-label">{Number(t.month.slice(5))}월</span>
          </button>
        ))}
      </div>
    </div>
  )
}

export default function ExpensePage() {
  const [params, setParams] = useSearchParams()
  const month = isValidMonth(params.get('month')) ? params.get('month')! : thisMonth()
  // 화면: 월별 내역 / 고정지출 관리 (새로고침해도 유지되도록 쿼리에 둔다)
  const view = params.get('view') === 'fixed' ? 'fixed' : 'monthly'
  // 목록 필터: 전체 / 고정 / 변동
  const [kindFilter, setKindFilter] = useState<ExpenseKind | 'ALL'>('ALL')

  const expenses = useAsync(() => expenseApi.list(month), [month])
  const summary = useAsync(() => expenseApi.summary(month), [month])

  // null: 폼 닫힘, 'new': 신규, number: 수정 대상 id
  const [editing, setEditing] = useState<number | 'new' | null>(null)
  const [form, setForm] = useState<ExpenseRequest>(() => emptyForm(month))
  const [error, setError] = useState('')

  const reloadAll = () => {
    expenses.reload()
    summary.reload()
  }

  const goMonth = (next: string) => {
    setParams({ month: next })
    setEditing(null)
  }

  const changeView = (next: 'monthly' | 'fixed') => {
    setParams(next === 'fixed' ? { month, view: 'fixed' } : { month })
    setEditing(null)
  }

  const visible = expenses.data?.filter((e) => kindFilter === 'ALL' || e.kind === kindFilter) ?? []
  const KIND_FILTERS: [ExpenseKind | 'ALL', string][] = [
    ['ALL', '전체'],
    ['FIXED', '고정'],
    ['VARIABLE', '변동'],
  ]

  // 수정 중인 내역이 고정지출 반영분이면 카테고리는 '고정비'로 고정
  const [editingFixed, setEditingFixed] = useState(false)

  const openNew = () => {
    setForm(emptyForm(month))
    setEditing('new')
    setEditingFixed(false)
    setError('')
  }

  const openEdit = (e: Expense) => {
    setForm(toForm(e))
    setEditing(e.id)
    setEditingFixed(e.kind === 'FIXED')
    setError('')
  }

  const set = <K extends keyof ExpenseRequest>(key: K, value: ExpenseRequest[K]) => setForm((f) => ({ ...f, [key]: value }))

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    try {
      if (editing === 'new') {
        await expenseApi.create(form)
        // 연속 입력이 많으므로 폼은 열어 두고 날짜·카테고리·결제수단은 유지한다
        setForm((f) => ({ ...f, amount: '', merchant: '', memo: '' }))
      } else if (typeof editing === 'number') {
        await expenseApi.update(editing, form)
        setEditing(null)
      }
      setError('')
      reloadAll()
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : String(err))
    }
  }

  const handleDelete = async (e: Expense) => {
    if (!confirm(`${formatDay(e.spentOn)} ${won(e.amount)} 지출을 삭제할까요?`)) return
    await expenseApi.remove(e.id)
    if (editing === e.id) setEditing(null)
    reloadAll()
  }

  return (
    <>
      <div className="section-head">
        <h1 className="nowrap">가계부</h1>
        {/* 월 이동·지출 추가는 월별 내역 화면에서만 의미가 있다 */}
        {view === 'monthly' && (
          <div className="head-actions">
            <div className="month-nav" role="group" aria-label="월 이동">
              <button type="button" className="secondary" onClick={() => goMonth(shiftMonth(month, -1))} aria-label="이전 달">
                ‹
              </button>
              <strong>{formatMonthLabel(month)}</strong>
              <button type="button" className="secondary" onClick={() => goMonth(shiftMonth(month, 1))} aria-label="다음 달">
                ›
              </button>
            </div>
            <button type="button" onClick={openNew}>
              지출 추가
            </button>
          </div>
        )}
      </div>

      <div className="tabs view-tabs" role="tablist" aria-label="가계부 화면">
        <button type="button" role="tab" aria-selected={view === 'monthly'} className={view === 'monthly' ? 'active' : ''} onClick={() => changeView('monthly')}>
          월별 내역
        </button>
        <button type="button" role="tab" aria-selected={view === 'fixed'} className={view === 'fixed' ? 'active' : ''} onClick={() => changeView('fixed')}>
          고정지출 관리
        </button>
      </div>

      {view === 'fixed' ? (
        <FixedExpensePanel onChanged={reloadAll} />
      ) : (
        <>
          {summary.error && <ErrorMessage error={summary.error} />}
          {summary.data && <SummaryCard summary={summary.data} onBudgetSaved={summary.reload} />}

          {editing !== null && (
            <form className="form card expense-form" onSubmit={handleSubmit}>
              <h2>{editing === 'new' ? '변동지출 추가' : '지출 수정'}</h2>
              <div className="row">
                <label>
                  날짜
                  <input type="date" value={form.spentOn} onChange={(e) => set('spentOn', e.target.value)} required />
                </label>
                <label>
                  금액 (원)
                  <input
                    type="number"
                    inputMode="numeric"
                    min="1"
                    step="1"
                    value={form.amount}
                    onChange={(e) => set('amount', e.target.value)}
                    autoFocus
                    required
                  />
                </label>
              </div>
              <div className="row">
                <label>
                  카테고리
                  {editingFixed ? (
                    <select value="FIXED_COST" disabled>
                      <option value="FIXED_COST">{EXPENSE_CATEGORIES.FIXED_COST}</option>
                    </select>
                  ) : (
                    <select value={form.category} onChange={(e) => set('category', e.target.value as ExpenseCategory)}>
                      {VARIABLE_CATEGORIES.map(([k, v]) => (
                        <option key={k} value={k}>
                          {v}
                        </option>
                      ))}
                    </select>
                  )}
                </label>
                <label>
                  결제 수단
                  <select value={form.paymentMethod} onChange={(e) => set('paymentMethod', e.target.value as PaymentMethod | '')}>
                <option value="">선택 안 함</option>
                    {Object.entries(PAYMENT_METHODS).map(([k, v]) => (
                      <option key={k} value={k}>
                        {v}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              <div className="row">
                <label>
                  사용처
                  <input value={form.merchant} onChange={(e) => set('merchant', e.target.value)} maxLength={100} placeholder="예) 스타벅스" />
                </label>
                <label>
                  메모
                  <input value={form.memo} onChange={(e) => set('memo', e.target.value)} maxLength={500} />
                </label>
              </div>
              {error && <p className="error pre">{error}</p>}
              <div className="row end">
                <button type="button" className="secondary" onClick={() => setEditing(null)}>
                  {editing === 'new' ? '닫기' : '취소'}
                </button>
                <button type="submit">저장</button>
              </div>
            </form>
          )}

          {summary.data && (
            <div className="expense-charts">
              <CategoryWeights items={summary.data.byCategory} />
              <MonthlyTrend trend={summary.data.trend} current={month} onSelect={goMonth} />
            </div>
          )}

          {expenses.loading && <Loading />}
          {expenses.error && <ErrorMessage error={expenses.error} />}
          <div className="list-head">
            <div className="tabs" role="group" aria-label="지출 구분">
              {KIND_FILTERS.map(([k, label]) => (
                <button key={k} type="button" className={kindFilter === k ? 'active' : ''} aria-pressed={kindFilter === k} onClick={() => setKindFilter(k)}>
                  {label}
                </button>
              ))}
            </div>
            <span className="muted small">
              {visible.length}건 · {won(visible.reduce((sum, e) => sum + e.amount, 0))}
            </span>
          </div>
          <div className="table-wrap">
            <table className="expense-table">
              <thead>
                <tr>
                  <th>날짜</th>
                  <th>카테고리</th>
                  <th>내용</th>
                  <th className="col-pay">결제</th>
                  <th className="num">금액</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {visible.map((e) => (
                  <tr key={e.id}>
                    <td className="nowrap col-date">{formatDay(e.spentOn)}</td>
                    <td className="nowrap col-cat">
                      {EXPENSE_CATEGORIES[e.category]}
                    </td>
                    <td className="col-content">
                      <div>{e.merchant ?? <span className="muted">-</span>}</div>
                      {e.memo && <div className="muted small">{e.memo}</div>}
                      {/* 좁은 화면에서는 결제 칸을 숨기고 여기에 함께 표시 */}
                      {e.paymentMethod && <div className="muted small pay-inline">{PAYMENT_METHODS[e.paymentMethod]}</div>}
                    </td>
                    <td className="muted small nowrap col-pay">{e.paymentMethod ? PAYMENT_METHODS[e.paymentMethod] : '-'}</td>
                    <td className="num nowrap col-amount">{won(e.amount)}</td>
                    <td className="actions">
                      <button type="button" className="link" onClick={() => openEdit(e)}>
                        수정
                      </button>
                      <button type="button" className="link danger" onClick={() => handleDelete(e)}>
                        삭제
                      </button>
                    </td>
                  </tr>
                ))}
                {expenses.data && visible.length === 0 && (
                  <tr>
                    <td colSpan={6} className="muted center">
                      {formatMonthLabel(month)} {kindFilter === 'FIXED' ? '고정' : kindFilter === 'VARIABLE' ? '변동' : ''}지출 내역이 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </>
      )}
    </>
  )
}
