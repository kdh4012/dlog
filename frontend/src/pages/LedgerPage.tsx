import { useState, type FormEvent } from 'react'
import { ledgerApi } from '../api'
import { ApiError } from '../api/client'
import type { LedgerItem, LedgerItemRequest, LedgerKind } from '../api/types'
import { ErrorMessage, Loading } from '../components/Status'
import { formatNumber } from '../utils/format'
import { useAsync } from '../utils/useAsync'

const won = (value: number) => `${formatNumber(value)}원`

interface SectionDef {
  kind: LedgerKind
  title: string
  /** 합계 옆 작은 설명 */
  unit: string
  placeholder: string
  /** 부채는 이름·잔액만 받는다 */
  withMemo: boolean
}

const SECTIONS: SectionDef[] = [
  { kind: 'INCOME', title: '수입', unit: '매달', placeholder: '예) 월급, 배당', withMemo: true },
  { kind: 'FIXED_EXPENSE', title: '고정 지출', unit: '매달', placeholder: '예) 월세, 통신비, 보험', withMemo: true },
  { kind: 'DEBT', title: '부채', unit: '잔액', placeholder: '예) 전세대출, 학자금', withMemo: false },
]

/** 입력 중인 금액 문자열 → 숫자만 남긴 문자열 (쉼표 등 제거) */
const digitsOnly = (v: string) => v.replace(/[^\d]/g, '').replace(/^0+(?=\d)/, '')

/** 항목 추가·수정 폼 (목록 안에서 그 자리에 열린다) */
function ItemForm({
  def,
  item,
  onDone,
}: {
  def: SectionDef
  /** null 이면 새 항목 */
  item: LedgerItem | null
  onDone: (changed: boolean) => void
}) {
  const [form, setForm] = useState<LedgerItemRequest>({
    kind: def.kind,
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
  items,
  total,
  onChanged,
}: {
  def: SectionDef
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
          <span className="muted">{def.unit}</span> {won(total)}
        </span>
        <button type="button" className="link" onClick={() => setEditing('new')} disabled={editing === 'new'}>
          + 추가
        </button>
      </div>
      <ul className="ledger-list">
        {items.map((it) =>
          editing === it.id ? (
            <li key={it.id} className="editing">
              <ItemForm def={def} item={it} onDone={done} />
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
            <ItemForm def={def} item={null} onDone={done} />
          </li>
        )}
        {items.length === 0 && editing !== 'new' && <li className="ledger-empty muted">등록된 {def.title} 항목이 없습니다.</li>}
      </ul>
    </section>
  )
}

/** 가계부: 매달 들어오는 돈(수입)과 나가는 돈(고정 지출), 갚아야 할 돈(부채) */
export default function LedgerPage() {
  const ledger = useAsync(() => ledgerApi.get(), [])
  const data = ledger.data

  const itemsOf = (kind: LedgerKind) =>
    !data ? [] : kind === 'INCOME' ? data.incomes : kind === 'FIXED_EXPENSE' ? data.fixedExpenses : data.debts
  const totalOf = (kind: LedgerKind) =>
    !data ? 0 : kind === 'INCOME' ? data.incomeTotal : kind === 'FIXED_EXPENSE' ? data.fixedExpenseTotal : data.debtTotal

  // 수입 중 고정 지출이 차지하는 비율 (수입이 없으면 표시 안 함)
  const fixedRatio = data && data.incomeTotal > 0 ? (data.fixedExpenseTotal / data.incomeTotal) * 100 : null

  return (
    <div className="ledger">
      <h1>가계부</h1>

      {ledger.loading && !data && <Loading />}
      {ledger.error && <ErrorMessage error={ledger.error} />}

      {data && (
        <>
          <section className="ledger-hero">
            <div className="invest-label">매달 남는 돈</div>
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
              <span className="muted">월 수입</span>
              <strong>{won(data.incomeTotal)}</strong>
            </div>
            <div>
              <span className="muted">월 고정 지출</span>
              <strong>{won(data.fixedExpenseTotal)}</strong>
            </div>
            <div>
              <span className="muted">총 부채</span>
              <strong>{won(data.debtTotal)}</strong>
            </div>
          </div>

          {SECTIONS.map((def) => (
            <LedgerSection key={def.kind} def={def} items={itemsOf(def.kind)} total={totalOf(def.kind)} onChanged={ledger.reload} />
          ))}
        </>
      )}
    </div>
  )
}
