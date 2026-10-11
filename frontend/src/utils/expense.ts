import { formatMoney } from './format'

export const pad = (n: number) => String(n).padStart(2, '0')

/** 브라우저(한국) 기준 오늘 yyyy-MM-dd. toISOString 은 UTC 라 자정 무렵 날짜가 밀리므로 쓰지 않는다 */
export const today = () => {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export const thisMonth = () => today().slice(0, 7)

/** yyyy-MM 에 delta 개월 더하기 */
export const shiftMonth = (month: string, delta: number) => {
  const [y, m] = month.split('-').map(Number)
  const d = new Date(y, m - 1 + delta, 1)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`
}

export const isValidMonth = (v: string | null): v is string => !!v && /^\d{4}-(0[1-9]|1[0-2])$/.test(v)

export const won = (v: number) => formatMoney(v, 'KRW')

/** 2026-10 → 2026년 10월 */
export const formatMonthLabel = (month: string) => {
  const [y, m] = month.split('-')
  return `${y}년 ${Number(m)}월`
}

/** 2026-10 → 2026.10 */
export const formatMonthShort = (month: string) => month.replace('-', '.')
