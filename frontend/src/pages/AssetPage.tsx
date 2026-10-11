import { useState, type FormEvent } from 'react'
import { assetApi } from '../api'
import { ApiError } from '../api/client'
import { ASSET_TYPES, CURRENCIES, type Asset, type AssetRequest, type AssetType, type Currency } from '../api/types'
import { ErrorMessage, Loading } from '../components/Status'
import { formatDate, formatMoney, formatNumber, formatRate, profitClass } from '../utils/format'
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
        <button type="button" onClick={openNew}>
          자산 추가
        </button>
      </div>

      {/* 통화별 요약 */}
      {summary.error && <ErrorMessage error={summary.error} />}
      <div className="summary-grid">
        {summary.data?.currencies.map((s) => (
          <div key={s.currency} className="card summary">
            <div className="muted">{s.currency} 평가금액</div>
            <div className="big">{formatMoney(s.evalAmount, s.currency)}</div>
            <div className={profitClass(s.profitAmount)}>
              {formatMoney(s.profitAmount, s.currency)} ({formatRate(s.profitRate)})
            </div>
            <div className="muted small">매입 {formatMoney(s.costAmount, s.currency)}</div>
            <ul className="weights">
              {s.byType.map((t) => (
                <li key={t.assetType}>
                  <span>{ASSET_TYPES[t.assetType]}</span>
                  <span className="bar">
                    <span style={{ width: `${t.weight}%` }} />
                  </span>
                  <span className="small">{t.weight.toFixed(1)}%</span>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>

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
                <td className="num">{formatMoney(a.avgPrice, a.currency)}</td>
                <td className="num">{formatMoney(a.currentPrice, a.currency)}</td>
                <td className="num">{formatMoney(a.evalAmount, a.currency)}</td>
                <td className={`num ${profitClass(a.profitAmount)}`}>
                  {formatMoney(a.profitAmount, a.currency)}
                  <div className="small">{formatRate(a.profitRate)}</div>
                </td>
                <td className="muted small">{formatDate(a.updatedAt)}</td>
                <td className="actions">
                  <button type="button" className="link" onClick={() => openEdit(a)}>
                    수정
                  </button>
                  <button type="button" className="link danger" onClick={() => handleDelete(a)}>
                    삭제
                  </button>
                </td>
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
