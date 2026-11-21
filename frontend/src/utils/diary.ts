const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

/** 오늘 (한국 시간, yyyy-MM-dd) */
export const todayKst = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Seoul' }).format(new Date())

/** yyyy-MM-dd → 요일 (날짜만 있는 값이라 시간대 영향 없이 UTC 로 계산) */
const weekday = (date: string) => WEEKDAYS[new Date(`${date}T00:00:00Z`).getUTCDay()]

/** 2026-11-21 → 2026년 11월 21일 토요일 */
export const formatDiaryDate = (date: string) =>
  `${date.slice(0, 4)}년 ${Number(date.slice(5, 7))}월 ${Number(date.slice(8, 10))}일 ${weekday(date)}요일`

/** 2026-11-21 → { day: '21', weekday: '토', weekend: 'sat' } (목록의 날짜 칸, 주말은 색으로 구분) */
export const dayParts = (date: string) => {
  const w = weekday(date)
  return { day: String(Number(date.slice(8, 10))), weekday: w, weekend: w === '토' ? 'sat' : w === '일' ? 'sun' : '' }
}

/** 2026-11-21 → 2026년 11월 (목록의 달 묶음 제목) */
export const formatDiaryMonth = (date: string) => `${date.slice(0, 4)}년 ${Number(date.slice(5, 7))}월`
