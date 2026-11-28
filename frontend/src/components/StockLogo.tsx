import { useId, useState } from 'react'
import type { Asset } from '../api/types'

/**
 * 종목 로고 후보 (키 없이 쓰는 공개 이미지). 토스증권·업비트 Open API 는 로고를 주지 않는다.
 * 앞에서부터 시도하고, 모두 실패하면 이름 첫 글자 배지로 대신한다.
 */
const logoUrls = (asset: Asset): string[] => {
  const t = encodeURIComponent((asset.ticker ?? '').toUpperCase())
  if (!t) return []
  if (asset.assetType === 'CRYPTO') return [`https://static.upbit.com/logos/${t}.png`]
  // 국내 주식은 6자리 종목코드
  if (/^\d{6}$/.test(t)) return [`https://file.alphasquare.co.kr/media/images/stock_logo/kr/${t}.png`]
  // financialmodelingprep 은 ETF 등 로고가 없을 때 404 대신 흐릿한 기본 그림을 줘서 글자 배지로 넘어가지 못한다.
  // 그래서 없는 종목은 404 를 주는 parqet 만 쓰고, 실패하면 글자 배지로 대신한다
  return [`https://assets.parqet.com/logos/symbol/${t}?format=png`]
}

/** 글자 배지 배경색: 같은 종목은 항상 같은 색 */
const BADGE_COLORS = ['#3451d1', '#0f9d58', '#f59e0b', '#7c3aed', '#e11d48', '#0891b2', '#475569']
const badgeColor = (key: string) => BADGE_COLORS[[...key].reduce((h, c) => h + c.charCodeAt(0), 0) % BADGE_COLORS.length]

export default function StockLogo({ asset }: { asset: Asset }) {
  // 금 현물은 금괴 아이콘
  if (asset.source === 'GOLD') return <GoldIcon />
  // 비트코인은 주황 원 + 기울인 B 아이콘
  if (asset.assetType === 'CRYPTO' && (asset.ticker ?? '').toUpperCase() === 'BTC') return <BitcoinIcon />
  return <RemoteLogo asset={asset} />
}

/** 공개 로고 이미지를 차례로 시도하고, 모두 실패하면 이름 첫 글자 배지 */
function RemoteLogo({ asset }: { asset: Asset }) {
  // 몇 번째 후보 URL 을 보여주는 중인지. 후보를 다 쓰면 글자 배지
  const [index, setIndex] = useState(0)
  const urls = logoUrls(asset)

  if (index >= urls.length) {
    return (
      <span className="asset-logo fallback" style={{ background: badgeColor(asset.ticker ?? asset.name) }} aria-hidden="true">
        {asset.name.charAt(0)}
      </span>
    )
  }
  return (
    <img
      key={urls[index]}
      className="asset-logo"
      src={urls[index]}
      alt=""
      loading="lazy"
      referrerPolicy="no-referrer"
      onError={() => setIndex((i) => i + 1)}
    />
  )
}

/** 금: 금색 원 위에 같은 톤의 금괴 하나 (윗면은 밝게, 앞면은 아래로 갈수록 진하게) */
function GoldIcon() {
  const id = useId()
  const bg = `${id}-bg`
  const top = `${id}-top`
  const front = `${id}-front`
  return (
    <svg className="asset-logo gold-icon" viewBox="0 0 44 44" aria-hidden="true">
      <defs>
        <linearGradient id={bg} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#f3cf72" />
          <stop offset="100%" stopColor="#c18f2c" />
        </linearGradient>
        <linearGradient id={top} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#fff6d6" />
          <stop offset="100%" stopColor="#f9dc8a" />
        </linearGradient>
        <linearGradient id={front} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#f6d27a" />
          <stop offset="100%" stopColor="#d9a43e" />
        </linearGradient>
      </defs>
      <circle cx="22" cy="22" r="22" fill={`url(#${bg})`} />
      {/* 앞면 → 윗면 → 모서리 하이라이트 순으로 겹친다 */}
      <path d="M13.5 20.5H30.5L35 29.5H9Z" fill={`url(#${front})`} />
      <path d="M15.5 14.5H28.5L30.5 20.5H13.5Z" fill={`url(#${top})`} />
      <path d="M13.5 20.5H30.5" stroke="#fff3c9" strokeWidth="0.8" />
    </svg>
  )
}

/** 비트코인: 주황 그라데이션 원 + 안쪽 테두리(동전 느낌) + 같은 톤의 밝은 ₿ */
function BitcoinIcon() {
  const id = useId()
  const bg = `${id}-bg`
  const glyph = `${id}-glyph`
  return (
    <svg className="asset-logo gold-icon" viewBox="0 0 44 44" aria-hidden="true">
      <defs>
        <linearGradient id={bg} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#ffb347" />
          <stop offset="100%" stopColor="#e2780a" />
        </linearGradient>
        <linearGradient id={glyph} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#fff8ec" />
          <stop offset="100%" stopColor="#ffe2b8" />
        </linearGradient>
      </defs>
      <circle cx="22" cy="22" r="22" fill={`url(#${bg})`} />
      <circle cx="22" cy="22" r="18.5" fill="none" stroke="rgba(255,240,220,0.55)" strokeWidth="1" />
      <g transform="rotate(14 22 22)" fill={`url(#${glyph})`}>
        {/* 위아래로 튀어나온 세로줄 두 개 */}
        <rect x="18" y="9" width="2.4" height="26" rx="1" />
        <rect x="22.4" y="9" width="2.4" height="26" rx="1" />
        {/* B: 바깥 윤곽에서 안쪽 두 구멍을 뺀다 (evenodd) */}
        <path
          fillRule="evenodd"
          d="M14 12.5h11.5a5 5 0 0 1 2.6 9.3 5.3 5.3 0 0 1-2.1 10.2H14v-3h2.2V15.5H14z
             M19.6 15.6v5.1h5.6a2.55 2.55 0 0 0 0-5.1z
             M19.6 23.6v5.3h6.1a2.65 2.65 0 0 0 0-5.3z"
        />
      </g>
    </svg>
  )
}
