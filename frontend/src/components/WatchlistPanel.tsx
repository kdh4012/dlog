import { useState, type FormEvent } from 'react'
import { watchlistApi } from '../api'
import { ApiError } from '../api/client'
import type { ExchangeRate, WatchlistItem, WatchlistRequest } from '../api/types'
import { formatMoney, formatRate, profitClass } from '../utils/format'
import { useAsync } from '../utils/useAsync'
import { ErrorMessage, Loading } from './Status'
import StockLogo from './StockLogo'

const EMPTY: WatchlistRequest = { ticker: '', name: '', memo: '' }

const toForm = (w: WatchlistItem): WatchlistRequest => ({
  ticker: w.ticker,
  name: w.name ?? '',
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
  // 순서 변경 직후 서버 응답(또는 낙관적 반영) 목록. null 이면 조회 결과를 그대로 쓴다
  const [ordered, setOrdered] = useState<WatchlistItem[] | null>(null)
  const [dragId, setDragId] = useState<number | null>(null)
  const [overId, setOverId] = useState<number | null>(null)
  const [orderError, setOrderError] = useState('')

  const items = ordered ?? list.data
  const reload = () => {
    setOrdered(null)
    list.reload()
  }

  /** 새 순서를 바로 화면에 반영하고 저장. 실패하면 서버 목록으로 되돌린다 */
  const saveOrder = async (next: WatchlistItem[]) => {
    setOrdered(next)
    setOrderError('')
    try {
      setOrdered(await watchlistApi.reorder(next.map((w) => w.id)))
    } catch (err) {
      setOrderError(err instanceof ApiError ? err.detail : String(err))
      reload()
    }
  }

  /** ▲▼ 버튼: 한 칸 이동 */
  const move = (index: number, delta: -1 | 1) => {
    if (!items) return
    const target = index + delta
    if (target < 0 || target >= items.length) return
    const next = [...items]
    ;[next[index], next[target]] = [next[target], next[index]]
    void saveOrder(next)
  }

  /** 드래그: 끌던 종목을 놓은 행 자리로 옮긴다 */
  const dropOn = (targetId: number) => {
    const sourceId = dragId
    setDragId(null)
    setOverId(null)
    if (!items || sourceId === null || sourceId === targetId) return
    const next = items.filter((w) => w.id !== sourceId)
    const source = items.find((w) => w.id === sourceId)
    const targetIndex = items.findIndex((w) => w.id === targetId)
    if (!source || targetIndex < 0) return
    next.splice(targetIndex, 0, source)
    void saveOrder(next)
  }

  const krwPerUsd = rate?.krwPerUnit.USD ?? null
  // 시세를 하나도 못 받았으면 안내 문구를 보여준다
  const noQuotes = !!items?.length && items.every((w) => w.price == null)

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
      reload()
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : String(err))
    }
  }

  const handleDelete = async (w: WatchlistItem) => {
    if (!confirm(`'${w.ticker}' 을(를) 관심 종목에서 뺄까요?`)) return
    await watchlistApi.remove(w.id)
    if (editing === w.id) setEditing(null)
    reload()
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
      {orderError && <p className="error">{orderError}</p>}
      {noQuotes && <p className="muted small">지금은 시세를 불러오지 못했습니다. 잠시 후 다시 확인해 주세요.</p>}

      <div className="table-wrap">
        <table className="watchlist-table">
          <thead>
            <tr>
              <th className="col-handle" aria-label="순서" />
              <th>종목</th>
              <th className="num">현재가</th>
              <th className="num">전일 대비</th>
              <th>메모</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {items?.map((w, i) => (
              <tr
                key={w.id}
                // 표에서는 행을 끌어 순서를 바꾼다 (모바일은 ▲▼ 버튼)
                draggable={items.length > 1}
                onDragStart={(e) => {
                  setDragId(w.id)
                  e.dataTransfer.effectAllowed = 'move'
                }}
                onDragOver={(e) => {
                  if (dragId === null) return
                  e.preventDefault()
                  setOverId(w.id)
                }}
                onDrop={(e) => {
                  e.preventDefault()
                  dropOn(w.id)
                }}
                onDragEnd={() => {
                  setDragId(null)
                  setOverId(null)
                }}
                className={[dragId === w.id ? 'dragging' : '', overId === w.id && dragId !== w.id ? 'drag-over' : ''].join(' ').trim() || undefined}
              >
                <td className="col-handle" title="끌어서 순서 변경">
                  ⠿
                </td>
                <td className="col-name">
                  <div className="stock-cell">
                    <StockLogo ticker={w.ticker} />
                    <div>
                      <div className="ticker">{w.ticker}</div>
                      {w.name && <div className="muted small">{w.name}</div>}
                    </div>
                  </div>
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
                <td className="muted small col-memo">{w.memo}</td>
                <td className="actions">
                  <button type="button" className="link order-btn" onClick={() => move(i, -1)} disabled={i === 0} aria-label="위로">
                    ▲
                  </button>
                  <button
                    type="button"
                    className="link order-btn"
                    onClick={() => move(i, 1)}
                    disabled={i === items.length - 1}
                    aria-label="아래로"
                  >
                    ▼
                  </button>
                  <button type="button" className="link" onClick={() => openEdit(w)}>
                    수정
                  </button>
                  <button type="button" className="link danger" onClick={() => handleDelete(w)}>
                    삭제
                  </button>
                </td>
              </tr>
            ))}
            {items?.length === 0 && (
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
