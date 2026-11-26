import type { Asset, ExchangeRate } from '../api/types'
import { formatNumber } from '../utils/format'

interface AssetClass {
  key: string
  label: string
  color: string
  match: (a: Asset) => boolean
}

// 금: 토스·업비트 데이터에 '금' 구분이 없어 종목명·티커로 가려낸다 ('금융' 등이 걸리지 않도록 '금' 한 글자는 쓰지 않는다)
const GOLD_NAME = /골드|금현물|금선물|krx\s*금|gold/i
const GOLD_TICKERS = new Set(['GLD', 'IAU', 'IAUM', 'GLDM', 'SGOL', 'BAR', '132030', '411060', '319640'])

/** 위에서부터 먼저 맞는 분류로 들어간다 (금 ETF 가 주식으로 잡히지 않도록 금을 먼저) */
const CLASSES: AssetClass[] = [
  { key: 'gold', label: '금', color: '#e0a800', match: (a) => GOLD_NAME.test(a.name) || GOLD_TICKERS.has((a.ticker ?? '').toUpperCase()) },
  { key: 'btc', label: '비트코인', color: '#f7931a', match: (a) => a.assetType === 'CRYPTO' && (a.ticker ?? '').toUpperCase() === 'BTC' },
  { key: 'coin', label: '기타 코인', color: '#8b5cf6', match: (a) => a.assetType === 'CRYPTO' },
  { key: 'stock', label: '주식', color: '#3182f6', match: (a) => a.assetType === 'STOCK' || a.assetType === 'ETF' || a.assetType === 'FUND' },
  { key: 'cash', label: '현금·예금', color: '#22a06b', match: (a) => a.assetType === 'CASH' || a.assetType === 'DEPOSIT' },
  { key: 'etc', label: '기타', color: '#9ca3af', match: () => true },
]

/** 평가금을 원화로. 환율이 없으면 null (비중을 낼 수 없음) */
const toKrw = (a: Asset, rate: ExchangeRate | null) => {
  if (a.currency === 'KRW') return a.evalAmount
  const r = rate?.krwPerUnit[a.currency]
  return r ? a.evalAmount * r : null
}

/**
 * 비중 표시: 소수 첫째 자리까지.
 * 다른 자산이 있는데 반올림으로 100.0 이 되거나, 아주 작은 비중이 0.0 이 되는 일이 없도록 맞춘다.
 */
const formatShare = (share: number, only: boolean) => {
  if (only) return '100'
  if (share > 0 && share < 0.1) return '<0.1'
  return (Math.min(Math.round(share * 10) / 10, 99.9)).toFixed(1)
}

/** 1억 이상은 '1.8억', 만 단위는 '3,250만' 처럼 짧게 */
const shortKrw = (v: number) => {
  if (v >= 100_000_000) return `${(v / 100_000_000).toFixed(v >= 1_000_000_000 ? 0 : 1)}억`
  if (v >= 10_000) return `${formatNumber(Math.round(v / 10_000))}만`
  return `${formatNumber(Math.round(v))}`
}

/**
 * 자산 종류별(주식·비트코인·금 …) 비중 카드.
 * 원화로 환산해 합친 뒤 비중이 큰 순으로 보여주고, 위에 전체를 한 줄 막대로 나눠 그린다.
 */
export default function AssetAllocation({ assets, rate }: { assets: Asset[]; rate: ExchangeRate | null }) {
  const sums = new Map<string, number>()
  for (const a of assets) {
    const krw = toKrw(a, rate)
    if (krw === null) return null // 환율이 없어 합칠 수 없으면 카드를 보여주지 않는다
    const cls = CLASSES.find((c) => c.match(a))!
    sums.set(cls.key, (sums.get(cls.key) ?? 0) + krw)
  }
  const total = [...sums.values()].reduce((s, v) => s + v, 0)
  if (total <= 0) return null

  const items = CLASSES.filter((c) => (sums.get(c.key) ?? 0) > 0)
    .map((c) => ({ ...c, value: sums.get(c.key)!, share: (sums.get(c.key)! / total) * 100 }))
    .sort((x, y) => y.value - x.value)

  return (
    <section className="alloc" aria-label="자산 비중">
      <div className="alloc-bar" aria-hidden="true">
        {items.map((c) => (
          <span key={c.key} style={{ width: `${c.share}%`, background: c.color }} />
        ))}
      </div>
      <div className="alloc-cards">
        {items.map((c) => (
          <div key={c.key} className="alloc-card">
            <div className="alloc-name">
              <i style={{ background: c.color }} />
              {c.label}
            </div>
            <div className="alloc-share">
              {formatShare(c.share, items.length === 1)}
              <small>%</small>
            </div>
            <div className="alloc-value">{shortKrw(c.value)}원</div>
          </div>
        ))}
      </div>
    </section>
  )
}
