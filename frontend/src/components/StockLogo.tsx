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

/** 금: 금속 질감의 원 위에 원소기호 Au. 장식 없이 단정하게 */
function GoldIcon() {
  const id = useId()
  const bg = `${id}-bg`
  return (
    <svg className="asset-logo gold-icon" viewBox="0 0 44 44" aria-hidden="true">
      <defs>
        <linearGradient id={bg} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#e9c46a" />
          <stop offset="100%" stopColor="#b8862b" />
        </linearGradient>
      </defs>
      <circle cx="22" cy="22" r="22" fill={`url(#${bg})`} />
      <text x="22" y="27.5" textAnchor="middle" fill="#fff" fontSize="15" fontWeight="700" letterSpacing="-0.3"
        fontFamily="system-ui, -apple-system, 'Segoe UI', sans-serif">
        Au
      </text>
    </svg>
  )
}

/** 비트코인 아이콘: 주황 원 위에 세로줄이 위아래로 튀어나온 B 를 오른쪽으로 살짝 기울인다 */
function BitcoinIcon() {
  return (
    <svg className="asset-logo gold-icon" viewBox="0 0 44 44" aria-hidden="true">
      <circle cx="22" cy="22" r="22" fill="#f7931a" />
      <g transform="rotate(14 22 22)" fill="#fff">
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
