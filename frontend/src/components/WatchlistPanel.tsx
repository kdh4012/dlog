import { useState, type FormEvent } from 'react'
import { watchlistApi } from '../api'
import { ApiError } from '../api/client'
import type { ExchangeRate, WatchlistItem, WatchlistRequest } from '../api/types'
import { formatMoney, formatRate, profitClass } from '../utils/format'
import { useAsync } from '../utils/useAsync'
import { ErrorMessage, Loading } from './Status'

const EMPTY: WatchlistRequest = { ticker: '', name: '', targetPrice: '', memo: '' }

const toForm = (w: WatchlistItem): WatchlistRequest => ({
  ticker: w.ticker,
  name: w.name ?? '',
  targetPrice: w.targetPrice == null ? '' : String(w.targetPrice),
  memo: w.memo ?? '',
})

const usd = (v: number) => formatMoney(v, 'USD')

/** 시세 기준 시각 (한국 시간, 예: 10.20 05:00) */
const formatQuotedAt = (iso: string) =>
  new Intl.DateTimeFormat('ko-KR', {
    timeZone: 'Asia/Seoul',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(iso))

/**
 * 자산 포트폴리오 하단의 관심 종목(미국 주식). 보유 자산과 별개로 티커만 담아 두고 시세를 본다.
 * rate 가 있으면 현재가 아래에 원화 환산값을 함께 표시한다.
 */
export default function WatchlistPanel({ rate }: { rate: ExchangeRate | null }) {
  const list = useAsync(() => watchlistApi.list(), [])
  // null: 폼 닫힘, 'new': 추가, number: 수정 대상 id
  const [editing, setEditing] = useState<number | 'new' | null>(null)
  const [form, setForm] = useState<WatchlistRequest>(EMPTY)
  const [error, setError] = useState('')

  const krwPerUsd = rate?.krwPerUnit.USD ?? null
  // 시세를 하나도 못 받았으면 안내 문구를 보여준다
  const noQuotes = !!list.data?.length && list.data.every((w) => w.price == null)

  const openNew = () => {
    setForm(EMPTY)
    setEditing('new')
    setError('')
  }

  const openEdit = (w: WatchlistItem) => {
    setForm(toForm(w))
    setEditing(w.id)
    setError('')
  }

  const set = <K extends keyof WatchlistRequest>(key: K, value: WatchlistRequest[K]) => setForm((f) => ({ ...f, [key]: value }))

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    try {
      if (editing === 'new') {
        await watchlistApi.add(form)
        // 연속으로 담기 쉽게 폼은 열어 둔다
        setForm(EMPTY)
      } else if (typeof editing === 'number') {
        await watchlistApi.update(editing, form)
        setEditing(null)
      }
      setError('')
      list.reload()
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : String(err))
    }
  }

  const handleDelete = async (w: WatchlistItem) => {
    if (!confirm(`'${w.ticker}' 을(를) 관심 종목에서 뺄까요?`)) return
    await watchlistApi.remove(w.id)
    if (editing === w.id) setEditing(null)
    list.reload()
  }

  return (
    <section className="watchlist">
      <div className="section-head">
        <div>
          <h2>관심 종목</h2>
          <p className="muted small">미국 주식 · 시세는 5분마다 갱신 (장중이 아니면 마지막 종가)</p>
        </div>
        <button type="button" onClick={openNew}>
          종목 담기
        </button>
      </div>

      {editing !== null && (
        <form className="form card watchlist-form" onSubmit={handleSubmit}>
          <div className="row">
            <label>
              티커
              <input
                value={form.ticker}
                onChange={(e) => set('ticker', e.target.value.toUpperCase())}
                placeholder="예) AAPL, NVDA, BRK-B"
                maxLength={10}
                disabled={editing !== 'new'}
                autoFocus={editing === 'new'}
                required
              />
            </label>
            <label>
              이름 (비우면 자동)
              <input value={form.name} onChange={(e) => set('name', e.target.value)} maxLength={100} />
            </label>
            <label>
              목표가 (USD, 선택)
              <input
                type="number"
                inputMode="decimal"
                min="0"
                step="any"
                value={form.targetPrice}
                onChange={(e) => set('targetPrice', e.target.value)}
              />
            </label>
          </div>
          <label>
            메모
            <input value={form.memo} onChange={(e) => set('memo', e.target.value)} maxLength={500} />
          </label>
          {error && <p className="error pre">{error}</p>}
          <div className="row end">
            <button type="button" className="secondary" onClick={() => setEditing(null)}>
              {editing === 'new' ? '닫기' : '취소'}
            </button>
            <button type="submit">{editing === 'new' ? '담기' : '저장'}</button>
          </div>
        </form>
      )}

      {list.loading && <Loading />}
      {list.error && <ErrorMessage error={list.error} />}
      {noQuotes && <p className="muted small">지금은 시세를 불러오지 못했습니다. 잠시 후 다시 확인해 주세요.</p>}

      <div className="table-wrap">
        <table className="watchlist-table">
          <thead>
            <tr>
              <th>종목</th>
              <th className="num">현재가</th>
              <th className="num">전일 대비</th>
              <th className="num">목표가</th>
              <th>메모</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {list.data?.map((w) => (
              <tr key={w.id}>
                <td className="col-name">
                  <div className="ticker">{w.ticker}</div>
                  {w.name && <div className="muted small">{w.name}</div>}
                </td>
                <td className="num col-price">
                  {w.price == null ? (
                    <span className="muted">-</span>
                  ) : (
                    <>
                      {usd(w.price)}
                      {krwPerUsd && <div className="muted small">≈ {formatMoney(w.price * krwPerUsd, 'KRW')}</div>}
                      {w.quotedAt && <div className="muted small">{formatQuotedAt(w.quotedAt)}</div>}
                    </>
                  )}
                </td>
                <td className={`num col-change ${profitClass(w.change ?? 0)}`}>
                  {w.change == null ? (
                    <span className="muted">-</span>
                  ) : (
                    <>
                      {w.change > 0 ? '+' : ''}
                      {usd(w.change)}
                      {w.changeRate != null && <div className="small">{formatRate(w.changeRate)}</div>}
                    </>
                  )}
                </td>
                <td className={`num col-target${w.targetPrice == null ? ' empty' : ''}`}>
                  {w.targetPrice == null ? (
                    <span className="muted">-</span>
                  ) : (
                    <>
                      {usd(w.targetPrice)}
                      {w.targetGapRate != null && (
                        <div className="small muted">
                          {w.targetGapRate <= 0 ? '목표가 도달' : `${formatRate(w.targetGapRate)} 남음`}
                        </div>
                      )}
                    </>
                  )}
                </td>
                <td className="muted small col-memo">{w.memo}</td>
                <td className="actions">
                  <button type="button" className="link" onClick={() => openEdit(w)}>
                    수정
                  </button>
                  <button type="button" className="link danger" onClick={() => handleDelete(w)}>
                    삭제
                  </button>
                </td>
              </tr>
            ))}
            {list.data?.length === 0 && (
              <tr>
                <td colSpan={6} className="muted center">
                  담아 둔 관심 종목이 없습니다. '종목 담기'로 티커를 추가하세요.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  )
}
