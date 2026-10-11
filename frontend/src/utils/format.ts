import type { Currency } from '../api/types'

const FRACTION_DIGITS: Record<Currency, number> = { KRW: 0, JPY: 0, USD: 2, EUR: 2 }

export const formatMoney = (value: number, currency: Currency) =>
  new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency,
    maximumFractionDigits: FRACTION_DIGITS[currency],
  }).format(value)

export const formatNumber = (value: number) => new Intl.NumberFormat('ko-KR', { maximumFractionDigits: 8 }).format(value)

export const formatRate = (value: number) => `${value > 0 ? '+' : ''}${value.toFixed(2)}%`

export const formatDate = (iso: string) => iso.slice(0, 10)

/** 2024-05-01 → 2024.05 */
export const formatYearMonth = (iso: string) => iso.slice(0, 7).replace('-', '.')

export const profitClass = (value: number) => (value > 0 ? 'up' : value < 0 ? 'down' : '')
