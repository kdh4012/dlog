import { useState, type FormEvent } from 'react'
import { ledgerApi } from '../api'
import { ApiError } from '../api/client'
import type { LedgerItem, LedgerItemRequest, LedgerKind, LedgerMonthTotal } from '../api/types'
import { ErrorMessage, Loading } from '../components/Status'
import { formatNumber } from '../utils/format'
import { useAsync } from '../utils/useAsync'

const won = (value: number) => `${formatNumber(value)}원`

/** 이번 달 (한국 시간, yyyy-MM) */
const thisMonth = () => {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit' }).formatToParts(new Date())
  const get = (t: string) => parts.find((p) => p.type === t)!.value
  return `${get('year')}-${get('month')}`
}

/** yyyy-MM 에 n 달 더하기 */
const addMonths = (ym: string, n: number) => {
  const [y, m] = ym.split('-').map(Number)
  const d = new Date(y, m - 1 + n, 1)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

/** 2026-11 → 2026년 11월 */
const monthLabel = (ym: string) => `${ym.slice(0, 4)}년 ${Number(ym.slice(5))}월`
const monthShort = (ym: string) => `${Number(ym.slice(5))}월`

/** 큰 금액을 막대 위에 짧게: 3,700,000 → 370만 */
const compactWon = (v: number) => (v >= 10_000 ? `${formatNumber(Math.round(v / 10_000))}만` : formatNumber(v))

interface SectionDef {
  kind: LedgerKind
  title: string
  placeholder: string
  /** 부채는 이름·잔액만 받는다 */
  withMemo: boolean
}

const SECTIONS: SectionDef[] = [
  { kind: 'INCOME', title: '수입', placeholder: '예) 월급, 배당', withMemo: true },
  { kind: 'FIXED_EXPENSE', title: '고정 지출', placeholder: '예) 월세, 통신비, 보험', withMemo: true },
  { kind: 'DEBT', title: '부채', placeholder: '예) 전세대출, 학자금', withMemo: false },
]

/** 입력 중인 금액 문자열 → 숫자만 남긴 문자열 (쉼표 등 제거) */
const digitsOnly = (v: string) => v.replace(/[^\d]/g, '').replace(/^0+(?=\d)/, '')

/** 항목 추가·수정 폼 (목록 안에서 그 자리에 열린다) */
function ItemForm({
  def,
  month,
  item,
  onDone,
}: {
  def: SectionDef
  /** 보고 있는 달 (새 수입·고정지출을 이 달에 넣는다) */
  month: string
  /** null 이면 새 항목 */
  item: LedgerItem | null
  onDone: (changed: boolean) => void
}) {
  const [form, setForm] = useState<LedgerItemRequest>({
    kind: def.kind,
    month: def.kind === 'DEBT' ? null : month,
    name: item?.name ?? '',
    amount: item ? String(item.amount) : '',
    memo: item?.memo ?? '',
  })
  const [error, setError] = useState('')

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    try {
      if (item) await ledgerApi.update(item.id, form)
      else await ledgerApi.create(form)
      onDone(true)
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : String(err))
    }
  }

  const handleDelete = async () => {
    if (!item || !confirm(`'${item.name}' 항목을 삭제할까요?`)) return
    await ledgerApi.remove(item.id)
    onDone(true)
  }

  return (
    <form className="ledger-form" onSubmit={handleSubmit}>
      <div className="ledger-form-fields">
        <input
          value={form.name}
          onChange={(e) => setForm({ ...form, name: e.target.value })}
          placeholder={def.placeholder}
          maxLength={100}
          aria-label="이름"
          required
          autoFocus
        />
        <label className="ledger-amount-input">
          <input
            value={form.amount === '' ? '' : formatNumber(Number(form.amount))}
            onChange={(e) => setForm({ ...form, amount: digitsOnly(e.target.value) })}
            inputMode="numeric"
            placeholder={def.kind === 'DEBT' ? '잔액' : '월 금액'}
            aria-label="금액"
            required
          />
          <span>원</span>
        </label>
        {def.withMemo && (
          <input value={form.memo} onChange={(e) => setForm({ ...form, memo: e.target.value })} placeholder="메모 (선택)" maxLength={500} aria-label="메모" />
        )}
      </div>
      {error && <p className="error pre small">{error}</p>}
      <div className="ledger-form-actions">
        {item && (
          <button type="button" className="link danger" onClick={handleDelete}>
            삭제
          </button>
        )}
        <button type="button" className="secondary" onClick={() => onDone(false)}>
          취소
        </button>
        <button type="submit">{item ? '저장' : '추가'}</button>
      </div>
    </form>
  )
}

function LedgerSection({
  def,
  month,
  items,
  total,
  onChanged,
}: {
  def: SectionDef
  month: string
  items: LedgerItem[]
  total: number
  onChanged: () => void
}) {
  // null: 폼 닫힘, 'new': 추가, number: 수정 중인 항목 id
  const [editing, setEditing] = useState<number | 'new' | null>(null)

  const done = (changed: boolean) => {
    setEditing(null)
    if (changed) onChanged()
  }

  return (
    <section className="ledger-section">
      <div className="ledger-section-head">
        <h2>{def.title}</h2>
        <span className="ledger-section-total">
          <span className="muted">{def.kind === 'DEBT' ? '현재 잔액' : monthShort(month)}</span> {won(total)}
        </span>
        <button type="button" className="link" onClick={() => setEditing('new')} disabled={editing === 'new'}>
          + 추가
        </button>
      </div>
      <ul className="ledger-list">
        {items.map((it) =>
          editing === it.id ? (
            <li key={it.id} className="editing">
              <ItemForm def={def} month={month} item={it} onDone={done} />
            </li>
          ) : (
            <li key={it.id}>
              <button type="button" className="ledger-row" onClick={() => setEditing(it.id)} title="눌러서 수정">
                <span className="ledger-name">
                  <span className="name">{it.name}</span>
                  {it.memo && <span className="sub">{it.memo}</span>}
                </span>
                <span className="ledger-amount">{won(it.amount)}</span>
              </button>
            </li>
          ),
        )}
        {editing === 'new' && (
          <li className="editing">
            <ItemForm def={def} month={month} item={null} onDone={done} />
          </li>
        )}
        {items.length === 0 && editing !== 'new' && <li className="ledger-empty muted">등록된 {def.title} 항목이 없습니다.</li>}
      </ul>
    </section>
  )
}

/** 최근 12개월 수입·고정 지출 막대. 막대를 누르면 그 달로 이동 */
function Trend({ trend, month, onSelect }: { trend: LedgerMonthTotal[]; month: string; onSelect: (ym: string) => void }) {
  const max = Math.max(1, ...trend.flatMap((t) => [t.income, t.fixedExpense]))
  return (
    <div className="ledger-trend">
      <div className="ledger-trend-legend">
        <span>
          <i className="swatch income" /> 수입
        </span>
        <span>
          <i className="swatch fixed" /> 고정 지출
        </span>
      </div>
      <div className="ledger-trend-bars">
        {trend.map((t) => (
          <button
            key={t.month}
            type="button"
            className={`ledger-trend-item${t.month === month ? ' active' : ''}`}
            onClick={() => onSelect(t.month)}
            title={`${monthLabel(t.month)} · 수입 ${won(t.income)} · 고정 지출 ${won(t.fixedExpense)} · 남는 돈 ${won(t.income - t.fixedExpense)}`}
          >
            <span className="ledger-trend-value">{t.income > 0 ? compactWon(t.income - t.fixedExpense) : ''}</span>
            <span className="ledger-trend-pair">
              <span className="bar-income" style={{ height: `${(t.income / max) * 100}%` }} />
              <span className="bar-fixed" style={{ height: `${(t.fixedExpense / max) * 100}%` }} />
            </span>
            <span className="ledger-trend-month">{monthShort(t.month)}</span>
          </button>
        ))}
      </div>
    </div>
  )
}

/** 가계부: 달마다 들어오는 돈(수입)과 나가는 돈(고정 지출), 갚아야 할 돈(부채) */
export default function LedgerPage() {
  const current = thisMonth()
  const [month, setMonth] = useState(current)
  const ledger = useAsync(() => ledgerApi.get(month), [month])
  // 달을 바꾸는 중에는 이전 달 데이터를 섞어 보여주지 않는다
  const data = ledger.data?.month === month ? ledger.data : null

  const itemsOf = (kind: LedgerKind) =>
    !data ? [] : kind === 'INCOME' ? data.incomes : kind === 'FIXED_EXPENSE' ? data.fixedExpenses : data.debts
  const totalOf = (kind: LedgerKind) =>
    !data ? 0 : kind === 'INCOME' ? data.incomeTotal : kind === 'FIXED_EXPENSE' ? data.fixedExpenseTotal : data.debtTotal

  // 수입 중 고정 지출이 차지하는 비율 (수입이 없으면 표시 안 함)
  const fixedRatio = data && data.incomeTotal > 0 ? (data.fixedExpenseTotal / data.incomeTotal) * 100 : null
  const m = monthShort(month)

  return (
    <div className="ledger">
      <div className="ledger-head">
        <h1>가계부</h1>
        <div className="ledger-month-nav">
          <button type="button" className="secondary" onClick={() => setMonth(addMonths(month, -1))} aria-label="이전 달">
            ‹
          </button>
          <strong>{monthLabel(month)}</strong>
          {/* 이월은 이번 달까지만 하므로 미래 달로는 넘어가지 않는다 */}
          <button type="button" className="secondary" onClick={() => setMonth(addMonths(month, 1))} disabled={month >= current} aria-label="다음 달">
            ›
          </button>
        </div>
      </div>

      {ledger.loading && !data && <Loading />}
      {ledger.error && <ErrorMessage error={ledger.error} />}

      {data && (
        <>
          <section className="ledger-hero">
            <div className="invest-label">{m}에 남는 돈</div>
            <div className={`invest-amount${data.monthlyBalance < 0 ? ' up' : ''}`}>
              {data.monthlyBalance < 0 ? '-' : ''}
              {won(Math.abs(data.monthlyBalance))}
            </div>
            <div className="muted">
              수입 {won(data.incomeTotal)} − 고정 지출 {won(data.fixedExpenseTotal)}
              {fixedRatio !== null && ` · 수입의 ${fixedRatio.toFixed(1)}%가 고정 지출`}
            </div>
          </section>

          <div className="ledger-stats">
            <div>
              <span className="muted">{m} 수입</span>
              <strong>{won(data.incomeTotal)}</strong>
            </div>
            <div>
              <span className="muted">{m} 고정 지출</span>
              <strong>{won(data.fixedExpenseTotal)}</strong>
            </div>
            <div>
              <span className="muted">총 부채 (현재)</span>
              <strong>{won(data.debtTotal)}</strong>
            </div>
          </div>

          <Trend trend={data.trend} month={month} onSelect={setMonth} />
          <p className="ledger-note muted small">
            새 달을 열면 지난달 수입·고정 지출이 그대로 이어집니다. 항목을 고치면 그 달만 바뀌고, 다음 달부터는 고친 내용이 이어집니다.
          </p>

          {SECTIONS.map((def) => (
            <LedgerSection
              key={`${def.kind}-${month}`}
              def={def}
              month={month}
              items={itemsOf(def.kind)}
              total={totalOf(def.kind)}
              onChanged={ledger.reload}
            />
          ))}
        </>
      )}
    </div>
  )
}
