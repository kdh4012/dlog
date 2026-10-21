import { useState } from 'react'

/**
 * 미국 주식 로고 후보 (키 없이 쓰는 공개 이미지). 토스증권 Open API 는 로고를 주지 않는다.
 * 앞에서부터 시도하고, 모두 실패하면 티커 첫 글자 배지로 대신한다.
 */
const logoUrls = (ticker: string) => {
  const t = encodeURIComponent(ticker.toUpperCase())
  return [`https://financialmodelingprep.com/image-stock/${t}.png`, `https://assets.parqet.com/logos/symbol/${t}?format=png`]
}

export default function StockLogo({ ticker }: { ticker: string }) {
  // 몇 번째 후보 URL 을 보여주는 중인지. 후보를 다 쓰면 글자 배지
  const [index, setIndex] = useState(0)
  const urls = logoUrls(ticker)

  if (index >= urls.length) {
    return (
      <span className="stock-logo fallback" aria-hidden="true">
        {ticker.charAt(0)}
      </span>
    )
  }
  return (
    <img
      key={urls[index]}
      className="stock-logo"
      src={urls[index]}
      alt=""
      loading="lazy"
      referrerPolicy="no-referrer"
      onError={() => setIndex((i) => i + 1)}
    />
  )
}
