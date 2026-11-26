import { useState } from 'react'
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
  return [`https://financialmodelingprep.com/image-stock/${t}.png`, `https://assets.parqet.com/logos/symbol/${t}?format=png`]
}

/** 글자 배지 배경색: 같은 종목은 항상 같은 색 */
const BADGE_COLORS = ['#3451d1', '#0f9d58', '#f59e0b', '#7c3aed', '#e11d48', '#0891b2', '#475569']
const badgeColor = (key: string) => BADGE_COLORS[[...key].reduce((h, c) => h + c.charCodeAt(0), 0) % BADGE_COLORS.length]

export default function StockLogo({ asset }: { asset: Asset }) {
  // 금 현물은 로고 대신 금색 배지
  if (asset.source === 'GOLD') {
    return (
      <span className="asset-logo fallback" style={{ background: '#e0a800' }} aria-hidden="true">
        금
      </span>
    )
  }
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
