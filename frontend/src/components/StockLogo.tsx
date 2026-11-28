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

/** 쌓인 금괴 3개 아이콘. 여러 개 그려도 그라데이션 id 가 겹치지 않도록 useId 를 쓴다 */
function GoldIcon() {
  const id = useId()
  const bg = `${id}-bg`
  const bar = `${id}-bar`

  // (x, y) 에서 시작하는 금괴 한 개: 앞면 사다리꼴 + 윗면 하이라이트
  const ingot = (x: number, y: number) => (
    <g key={`${x}-${y}`}>
      <path d={`M${x + 3} ${y}H${x + 15}L${x + 18} ${y + 10}H${x}Z`} fill={`url(#${bar})`} />
      <path d={`M${x + 3} ${y}H${x + 15}L${x + 15.8} ${y + 2.6}H${x + 2.2}Z`} fill="#fff4c2" opacity="0.85" />
    </g>
  )

  return (
    <svg className="asset-logo gold-icon" viewBox="0 0 44 44" aria-hidden="true">
      <defs>
        <linearGradient id={bg} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#fff8e1" />
          <stop offset="100%" stopColor="#fde7a6" />
        </linearGradient>
        <linearGradient id={bar} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#ffd84d" />
          <stop offset="55%" stopColor="#f0b400" />
          <stop offset="100%" stopColor="#c48a00" />
        </linearGradient>
      </defs>
      <rect width="44" height="44" rx="14" fill={`url(#${bg})`} />
      {ingot(3.5, 25)}
      {ingot(22.5, 25)}
      {ingot(13, 14)}
      {/* 반짝임 */}
      <path d="M34 6.5l1 2.5 2.5 1-2.5 1-1 2.5-1-2.5-2.5-1 2.5-1z" fill="#fff" opacity="0.95" />
    </svg>
  )
}
