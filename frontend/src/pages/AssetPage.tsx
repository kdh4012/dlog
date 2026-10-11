import { useState, type FormEvent } from 'react'
import { assetApi } from '../api'
import { ApiError } from '../api/client'
import { ASSET_TYPES, CURRENCIES, type Asset, type AssetRequest, type AssetType, type Currency, type TypeWeight } from '../api/types'
import AssetSyncPanel from '../components/AssetSyncPanel'
import { ErrorMessage, Loading } from '../components/Status'
import { formatDate, formatIn, formatMoney, formatNumber, formatRate, profitClass, type DisplayCurrency } from '../utils/format'
import { useAsync } from '../utils/useAsync'

const EMPTY: AssetRequest = {
  assetType: 'STOCK',
  name: '',
  ticker: '',
  account: '',
  quantity: '',
  avgPrice: '',
  currentPrice: '',
  currency: 'KRW',
  memo: '',
}

const DISPLAY_CURRENCIES: [DisplayCurrency, string][] = [
  ['KRW', '원화'],
  ['USD', '달러'],
]

const CURRENCY_LABELS: Record<Currency, string> = { KRW: '원화', USD: '달러', JPY: '엔화', EUR: '유로' }

const DISPLAY_KEY = 'dlog.asset.display'

/** 표시 통화는 브라우저별 편의 설정이라 localStorage 에 둔다. 저장소 접근이 막혀도 원화로 동작 */
const loadDisplayCurrency = (): DisplayCurrency => {
  try {
    return localStorage.getItem(DISPLAY_KEY) === 'USD' ? 'USD' : 'KRW'
  } catch {
    return 'KRW'
  }
}

const saveDisplayCurrency = (value: DisplayCurrency) => {
  try {
    localStorage.setItem(DISPLAY_KEY, value)
  } catch {
    // 시크릿 모드 등 저장 불가 환경: 이번 화면에서만 유지
  }
}

/** 자산 유형별 비중 막대 */
function TypeWeights({ items }: { items: TypeWeight[] }) {
  return (
    <ul className="weights">
      {items.map((t) => (
        <li key={t.assetType}>
          <span>{ASSET_TYPES[t.assetType]}</span>
          <span className="bar">
            <span style={{ width: `${t.weight}%` }} />
          </span>
          <span className="small">{t.weight.toFixed(1)}%</span>
        </li>
      ))}
    </ul>
  )
}

const toForm = (a: Asset): AssetRequest => ({
  assetType: a.assetType,
  name: a.name,
  ticker: a.ticker ?? '',
  account: a.account ?? '',
  quantity: String(a.quantity),
  avgPrice: String(a.avgPrice),
  currentPrice: String(a.currentPrice),
  currency: a.currency,
  memo: a.memo ?? '',
})

export default function AssetPage() {
  const assets = useAsync(() => assetApi.list(), [])
  const summary = useAsync(() => assetApi.summary(), [])
  // 자산이 없으면 0원 합계 카드 대신 아무것도 보여주지 않는다
  const total = summary.data?.currencies.length ? summary.data.total : null
  const rate = summary.data?.exchangeRate ?? null
  const [display, setDisplay] = useState<DisplayCurrency>(loadDisplayCurrency)
  const money = (value: number, currency: Currency) => formatIn(value, currency, display, rate)

  const changeDisplay = (next: DisplayCurrency) => {
    setDisplay(next)
    saveDisplayCurrency(next)
  }

  // null: 폼 닫힘, 'new': 신규, number: 수정 대상 id
  const [editing, setEditing] = useState<number | 'new' | null>(null)
  const [form, setForm] = useState<AssetRequest>(EMPTY)
  const [error, setError] = useState('')

  const reloadAll = () => {
    assets.reload()
    summary.reload()
  }

  const openNew = () => {
    setForm(EMPTY)
    setEditing('new')
    setError('')
  }

  const openEdit = (a: Asset) => {
    setForm(toForm(a))
    setEditing(a.id)
    setError('')
  }

  const set = <K extends keyof AssetRequest>(key: K, value: AssetRequest[K]) => setForm((f) => ({ ...f, [key]: value }))

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    try {
      if (editing === 'new') await assetApi.create(form)
      else if (typeof editing === 'number') await assetApi.update(editing, form)
      setEditing(null)
      reloadAll()
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : String(err))
    }
  }

  const handleDelete = async (a: Asset) => {
    if (!confirm(`'${a.name}' 자산을 삭제할까요?`)) return
    await assetApi.remove(a.id)
    reloadAll()
  }

  return (
    <>
      <div className="section-head">
        <h1>자산 포트폴리오</h1>
        <div className="head-actions">
          {/* 환율이 없으면 환산할 수 없으므로 전환 버튼을 숨기고 통화별 원래 금액만 보여준다 */}
          {rate && (
            <div className="tabs" role="group" aria-label="표시 통화">
              {DISPLAY_CURRENCIES.map(([c, label]) => (
                <button
                  key={c}
                  type="button"
                  className={display === c ? 'active' : ''}
                  aria-pressed={display === c}
                  onClick={() => changeDisplay(c)}
                >
                  {label}
                </button>
              ))}
            </div>
          )}
          <button type="button" onClick={openNew}>
            자산 추가
          </button>
        </div>
      </div>

      <AssetSyncPanel onSynced={reloadAll} />

      {summary.error && <ErrorMessage error={summary.error} />}
      {total && (
        <div className="card summary total">
          <div className="muted">총 자산</div>
          <div className="big">
            {display === 'KRW' ? formatMoney(total.evalKrw, 'KRW') : formatMoney(total.evalUsd, 'USD')}
          </div>
          <div className={profitClass(total.profitKrw)}>
            {display === 'KRW' ? formatMoney(total.profitKrw, 'KRW') : formatMoney(total.profitUsd, 'USD')} (
            {formatRate(total.profitRate)})
          </div>
          <div className="muted small">
            매입 {display === 'KRW' ? formatMoney(total.costKrw, 'KRW') : formatMoney(total.costUsd, 'USD')}
          </div>
          {rate?.krwPerUnit.USD && (
            <div className="muted small">
              환율 1 USD = {formatNumber(rate.krwPerUnit.USD)}원 ({rate.source}, {rate.date} 기준)
            </div>
          )}
          <TypeWeights items={total.byType} />
        </div>
      )}

      {/* 환율을 못 가져와 합산할 수 없을 때만 통화별 요약으로 대신한다 */}
      {!total && summary.data && summary.data.currencies.length > 0 && (
        <div className="summary-grid">
          {summary.data.currencies.map((s) => (
            <div key={s.currency} className="card summary">
              <div className="muted">{CURRENCY_LABELS[s.currency]} 자산</div>
              <div className="big">{formatMoney(s.evalAmount, s.currency)}</div>
              <div className={profitClass(s.profitAmount)}>
                {formatMoney(s.profitAmount, s.currency)} ({formatRate(s.profitRate)})
              </div>
              <div className="muted small">매입 {formatMoney(s.costAmount, s.currency)}</div>
              <TypeWeights items={s.byType} />
            </div>
          ))}
        </div>
      )}

      {editing !== null && (
        <form className="form card" onSubmit={handleSubmit}>
          <h2>{editing === 'new' ? '자산 추가' : '자산 수정'}</h2>
          <div className="row">
            <label>
              유형
              <select value={form.assetType} onChange={(e) => set('assetType', e.target.value as AssetType)}>
                {Object.entries(ASSET_TYPES).map(([k, v]) => (
                  <option key={k} value={k}>
                    {v}
                  </option>
                ))}
              </select>
            </label>
            <label>
              통화
              <select value={form.currency} onChange={(e) => set('currency', e.target.value as Currency)}>
                {CURRENCIES.map((c) => (
                  <option key={c}>{c}</option>
                ))}
              </select>
            </label>
          </div>
          <div className="row">
            <label>
              종목명
              <input value={form.name} onChange={(e) => set('name', e.target.value)} maxLength={100} required />
            </label>
            <label>
              티커
              <input value={form.ticker} onChange={(e) => set('ticker', e.target.value)} maxLength={30} />
            </label>
            <label>
              계좌 / 증권사
              <input value={form.account} onChange={(e) => set('account', e.target.value)} maxLength={100} />
            </label>
          </div>
          <div className="row">
            <label>
              수량
              <input type="number" step="any" min="0" value={form.quantity} onChange={(e) => set('quantity', e.target.value)} required />
            </label>
            <label>
              평균 단가
              <input type="number" step="any" min="0" value={form.avgPrice} onChange={(e) => set('avgPrice', e.target.value)} required />
            </label>
            <label>
              현재가
              <input type="number" step="any" min="0" value={form.currentPrice} onChange={(e) => set('currentPrice', e.target.value)} required />
            </label>
          </div>
          <label>
            메모
            <input value={form.memo} onChange={(e) => set('memo', e.target.value)} maxLength={500} />
          </label>
          {error && <p className="error pre">{error}</p>}
          <div className="row end">
            <button type="button" className="secondary" onClick={() => setEditing(null)}>
              취소
            </button>
            <button type="submit">저장</button>
          </div>
        </form>
      )}

      {assets.loading && <Loading />}
      {assets.error && <ErrorMessage error={assets.error} />}
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>유형</th>
              <th>종목</th>
              <th className="num">수량</th>
              <th className="num">평균단가</th>
              <th className="num">현재가</th>
              <th className="num">평가금액</th>
              <th className="num">손익</th>
              <th>수정일</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {assets.data?.map((a) => (
              <tr key={a.id}>
                <td>{ASSET_TYPES[a.assetType]}</td>
                <td>
                  <div>{a.name}</div>
                  <div className="muted small">{[a.ticker, a.account].filter(Boolean).join(' · ')}</div>
                </td>
                <td className="num">{formatNumber(a.quantity)}</td>
                <td className="num">{money(a.avgPrice, a.currency)}</td>
                <td className="num">{money(a.currentPrice, a.currency)}</td>
                <td className="num">{money(a.evalAmount, a.currency)}</td>
                <td className={`num ${profitClass(a.profitAmount)}`}>
                  {money(a.profitAmount, a.currency)}
                  <div className="small">{formatRate(a.profitRate)}</div>
                </td>
                <td className="muted small">{formatDate(a.updatedAt)}</td>
                {/* 동기화 자산은 다음 동기화 때 덮어써지므로 서버에서도 수정·삭제를 막는다 */}
                {a.source === 'MANUAL' ? (
                  <td className="actions">
                    <button type="button" className="link" onClick={() => openEdit(a)}>
                      수정
                    </button>
                    <button type="button" className="link danger" onClick={() => handleDelete(a)}>
                      삭제
                    </button>
                  </td>
                ) : (
                  <td className="muted small">자동 동기화</td>
                )}
              </tr>
            ))}
            {assets.data?.length === 0 && (
              <tr>
                <td colSpan={9} className="muted center">
                  등록된 자산이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </>
  )
}
