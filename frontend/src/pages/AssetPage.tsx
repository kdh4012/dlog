import { useState } from 'react'
import { assetApi } from '../api'
import type { Asset, Currency, ExchangeRate } from '../api/types'
import AssetAllocation from '../components/AssetAllocation'
import AssetSyncPanel from '../components/AssetSyncPanel'
import GoldEditor from '../components/GoldEditor'
import { ErrorMessage, Loading } from '../components/Status'
import { formatNumber, profitClass, type DisplayCurrency } from '../utils/format'
import { useAsync } from '../utils/useAsync'

/** 금액 칸에 보여줄 값: 평가금(보유 총액) 또는 현재가(1주 가격) */
type ViewMode = 'eval' | 'price'

/** 보기 설정은 브라우저별 편의 설정이라 localStorage 에 둔다. 저장소 접근이 막혀도 기본값으로 동작 */
const load = <T extends string>(key: string, allowed: readonly T[], fallback: T): T => {
  try {
    const v = localStorage.getItem(key)
    return allowed.includes(v as T) ? (v as T) : fallback
  } catch {
    return fallback
  }
}

const save = (key: string, value: string) => {
  try {
    localStorage.setItem(key, value)
  } catch {
    // 시크릿 모드 등 저장 불가 환경: 이번 화면에서만 유지
  }
}

const DISPLAY_KEY = 'dlog.asset.display'
const VIEW_KEY = 'dlog.asset.view'

/** 토스처럼 원화는 '원' 접미사, 달러는 '$' 접두사 */
const formatAmount = (value: number, currency: DisplayCurrency | Currency, signed = false) => {
  const sign = signed && value > 0 ? '+' : value < 0 ? '-' : ''
  const abs = Math.abs(value)
  if (currency === 'KRW') return `${sign}${formatNumber(Math.round(abs))}원`
  const digits = abs !== 0 && abs < 1 ? 6 : 2
  const n = abs.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: digits })
  return currency === 'USD' ? `${sign}$${n}` : `${sign}${n} ${currency}`
}

/** 수익률은 부호 없이 색으로만 구분 (예: 35.0%) */
const formatRateAbs = (rate: number) => `${Math.abs(rate).toFixed(1)}%`

/** 금액을 표시 통화로 환산. 환율이 없으면 원래 통화 그대로 */
const convert = (value: number, from: Currency, to: DisplayCurrency, rate: ExchangeRate | null) => {
  if (from === to) return { value, currency: to as Currency }
  const f = rate?.krwPerUnit[from]
  const t = rate?.krwPerUnit[to]
  if (!f || !t) return { value, currency: from }
  return { value: (value * f) / t, currency: to as Currency }
}

interface Group {
  key: string
  label: string
  items: Asset[]
}

/** 증권사(토스·한국투자) 주식은 해외·국내로 합쳐 묶고, 업비트 가상자산·금 순. 각 묶음은 평가금 큰 순 */
const isBroker = (a: Asset) => a.source === 'TOSS' || a.source === 'KIS'

const groupAssets = (assets: Asset[]): Group[] => {
  const defs: [string, string, (a: Asset) => boolean][] = [
    ['stock-us', '해외주식', (a) => isBroker(a) && a.currency !== 'KRW'],
    ['stock-kr', '국내주식', (a) => isBroker(a) && a.currency === 'KRW'],
    ['upbit', '가상자산', (a) => a.source === 'UPBIT'],
    ['gold', '금', (a) => a.source === 'GOLD'],
    ['etc', '기타', (a) => a.source === 'MANUAL'],
  ]
  return defs
    .map(([key, label, match]) => ({
      key,
      label,
      items: assets.filter(match).sort((x, y) => y.evalAmount - x.evalAmount),
    }))
    .filter((g) => g.items.length > 0)
}

/** 주식은 '259주', 코인은 '0.015 BTC', 금은 '12.5g' */
const quantityText = (a: Asset) => {
  if (a.source === 'GOLD') return `${formatNumber(a.quantity)}g`
  return a.assetType === 'CRYPTO' ? `${formatNumber(a.quantity)} ${a.ticker ?? ''}`.trim() : `${formatNumber(a.quantity)}주`
}

export default function AssetPage() {
  const assets = useAsync(() => assetApi.list(), [])
  const summary = useAsync(() => assetApi.summary(), [])
  const rate = summary.data?.exchangeRate ?? null
  const total = summary.data?.total ?? null

  const [display, setDisplay] = useState<DisplayCurrency>(() => load(DISPLAY_KEY, ['KRW', 'USD'] as const, 'KRW'))
  const [view, setView] = useState<ViewMode>(() => load(VIEW_KEY, ['eval', 'price'] as const, 'eval'))
  const [collapsed, setCollapsed] = useState<Set<string>>(new Set())

  const changeDisplay = (next: DisplayCurrency) => {
    setDisplay(next)
    save(DISPLAY_KEY, next)
  }
  const changeView = (next: ViewMode) => {
    setView(next)
    save(VIEW_KEY, next)
  }
  const toggleGroup = (key: string) =>
    setCollapsed((prev) => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })

  const money = (value: number, currency: Currency, signed = false) => {
    const c = convert(value, currency, display, rate)
    return formatAmount(c.value, c.currency, signed)
  }

  // 금 보유량을 고치면 동기화 상태 줄도 새로 읽도록 다시 그린다
  const [syncPanelKey, setSyncPanelKey] = useState(0)

  const reloadAll = () => {
    assets.reload()
    summary.reload()
  }

  const groups = groupAssets(assets.data ?? [])

  return (
    <div className="invest">
      <h1>자산 포트폴리오</h1>

      {/* 맨 위: 자산 종류별 비중 카드 */}
      {assets.data && <AssetAllocation assets={assets.data} rate={rate} />}

      {/* 내 투자 합계 */}
      <section className="invest-total">
        <div className="invest-label">내 투자</div>
        {total ? (
          <>
            <div className="invest-amount">
              {display === 'KRW' ? formatAmount(total.evalKrw, 'KRW') : formatAmount(total.evalUsd, 'USD')}
            </div>
            <div className={`invest-profit ${profitClass(total.profitKrw)}`}>
              {display === 'KRW' ? formatAmount(total.profitKrw, 'KRW', true) : formatAmount(total.profitUsd, 'USD', true)} (
              {formatRateAbs(total.profitRate)})
            </div>
          </>
        ) : (
          // 환율을 못 가져와 합산할 수 없으면 통화별로 보여준다
          summary.data?.currencies.map((c) => (
            <div key={c.currency}>
              <div className="invest-amount">{formatAmount(c.evalAmount, c.currency)}</div>
              <div className={`invest-profit ${profitClass(c.profitAmount)}`}>
                {formatAmount(c.profitAmount, c.currency, true)} ({formatRateAbs(c.profitRate)})
              </div>
            </div>
          ))
        )}
        {summary.error && <ErrorMessage error={summary.error} />}
      </section>

      {/* 보기 설정: 현재가/평가금, 달러/원 */}
      <div className="invest-controls">
        <div className="seg" role="group" aria-label="금액 보기">
          <button type="button" className={view === 'price' ? 'on' : ''} aria-pressed={view === 'price'} onClick={() => changeView('price')}>
            현재가
          </button>
          <button type="button" className={view === 'eval' ? 'on' : ''} aria-pressed={view === 'eval'} onClick={() => changeView('eval')}>
            평가금
          </button>
        </div>
        {/* 환율이 없으면 환산할 수 없으므로 통화 전환은 숨긴다 */}
        {rate && (
          <div className="seg" role="group" aria-label="표시 통화">
            <button type="button" className={display === 'USD' ? 'on' : ''} aria-pressed={display === 'USD'} onClick={() => changeDisplay('USD')}>
              $
            </button>
            <button type="button" className={display === 'KRW' ? 'on' : ''} aria-pressed={display === 'KRW'} onClick={() => changeDisplay('KRW')}>
              원
            </button>
          </div>
        )}
      </div>

      {assets.loading && <Loading />}
      {assets.error && <ErrorMessage error={assets.error} />}
      {assets.data?.length === 0 && <p className="invest-empty muted">보유 자산이 없습니다. 토스증권·업비트 API 키를 설정하면 자동으로 채워집니다.</p>}

      {groups.map((g) => {
        // 묶음은 통화가 하나라 원래 통화로 합산한 뒤 표시 통화로 바꾼다
        const currency = g.items[0].currency
        const cost = g.items.reduce((s, a) => s + a.costAmount, 0)
        const profit = g.items.reduce((s, a) => s + a.profitAmount, 0)
        const groupRate = cost === 0 ? 0 : (profit / cost) * 100
        const open = !collapsed.has(g.key)
        return (
          <section key={g.key} className="invest-group">
            <button type="button" className="invest-group-head" aria-expanded={open} onClick={() => toggleGroup(g.key)}>
              <span className="invest-group-title">{g.label}</span>
              <span className={profitClass(profit)} title={money(profit, currency, true)}>
                {profit > 0 ? '+' : profit < 0 ? '-' : ''}
                {formatRateAbs(groupRate)}
              </span>
              <span className={`chevron${open ? ' open' : ''}`} aria-hidden="true" />
            </button>
            {open && (
              <ul className="invest-list">
                {g.items.map((a) => (
                  <li key={a.id}>
                    <div className="invest-name">
                      <div className="name">{a.name}</div>
                      <div className="sub">{quantityText(a)}</div>
                    </div>
                    <div className="invest-value">
                      <div className="value">{view === 'eval' ? money(a.evalAmount, a.currency) : money(a.currentPrice, a.currency)}</div>
                      {view === 'eval' ? (
                        <div className={`sub ${profitClass(a.profitAmount)}`}>
                          {money(a.profitAmount, a.currency, true)} ({formatRateAbs(a.profitRate)})
                        </div>
                      ) : (
                        <div className="sub">
                          평균 {money(a.avgPrice, a.currency)}{' '}
                          <span className={profitClass(a.profitAmount)}>
                            {a.profitRate > 0 ? '+' : a.profitRate < 0 ? '-' : ''}
                            {formatRateAbs(a.profitRate)}
                          </span>
                        </div>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </section>
        )
      })}

      <GoldEditor
        onSaved={() => {
          reloadAll()
          setSyncPanelKey((k) => k + 1)
        }}
      />

      <AssetSyncPanel key={syncPanelKey} onSynced={reloadAll} />
    </div>
  )
}
