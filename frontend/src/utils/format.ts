import type { Currency, ExchangeRate } from '../api/types'

const FRACTION_DIGITS: Record<Currency, number> = { KRW: 0, JPY: 0, USD: 2, EUR: 2 }

/** 1 미만 금액(소액 코인 단가, 원화→달러 환산 단가 등)이 0 으로 뭉개지지 않도록 소수 자릿수를 늘린다 */
const SMALL_VALUE_FRACTION_DIGITS = 6

export const formatMoney = (value: number, currency: Currency) =>
  new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency,
    maximumFractionDigits:
      value !== 0 && Math.abs(value) < 1 ? SMALL_VALUE_FRACTION_DIGITS : FRACTION_DIGITS[currency],
  }).format(value)

export const formatNumber = (value: number) => new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 8 }).format(value)

export const formatRate = (value: number) => `${value > 0 ? '+' : ''}${value.toFixed(2)}%`

export const formatDate = (iso: string) => iso.slice(0, 10)

/** 2024-05-01 → 2024.05 */
export const formatYearMonth = (iso: string) => iso.slice(0, 7).replace('-', '.')

/** 자산 화면의 금액 표시 통화 (원화/달러 전환) */
export type DisplayCurrency = Extract<Currency, 'KRW' | 'USD'>

/** 금액을 표시 통화로 환산해 포맷. 같은 통화이거나 환율이 없으면 원래 통화 그대로 */
export const formatIn = (value: number, currency: Currency, display: DisplayCurrency, rate: ExchangeRate | null) => {
  const from = rate?.krwPerUnit[currency]
  const to = rate?.krwPerUnit[display]
  if (currency === display || !from || !to) return formatMoney(value, currency)
  return formatMoney((value * from) / to, display)
}

export const profitClass = (value: number) => (value > 0 ? 'up' : value < 0 ? 'down' : '')
