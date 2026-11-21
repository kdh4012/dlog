import { Link } from 'react-router'
import type { DiarySummary } from '../api/types'
import { dayParts, formatDiaryMonth } from '../utils/diary'

/** 달별로 묶은 일기 목록 (홈, 일기 목록 공용). 목록은 최근 날짜 순으로 받는다 */
export default function DiaryList({ entries, groupByMonth = true }: { entries: DiarySummary[]; groupByMonth?: boolean }) {
  // 연속된 같은 달끼리 묶는다 (이미 날짜순이라 순서 유지)
  const groups: { month: string; items: DiarySummary[] }[] = []
  for (const e of entries) {
    const month = formatDiaryMonth(e.entryDate)
    const last = groups[groups.length - 1]
    if (groupByMonth && last?.month === month) last.items.push(e)
    else if (!groupByMonth && last) last.items.push(e)
    else groups.push({ month, items: [e] })
  }

  return (
    <div className="diary-list">
      {groups.map((g) => (
        <section key={g.month + g.items[0].id}>
          {groupByMonth && <h2 className="diary-month">{g.month}</h2>}
          <ul>
            {g.items.map((e) => {
              const { day, weekday } = dayParts(e.entryDate)
              return (
                <li key={e.id}>
                  <Link to={`/diary/${e.id}`} className="diary-item">
                    <span className="diary-day">
                      <strong>{day}</strong>
                      <span>{weekday}</span>
                    </span>
                    <span className="diary-text">
                      {e.title && <span className="diary-title">{e.title}</span>}
                      <span className={e.title ? 'diary-preview' : 'diary-preview only'}>{e.preview}</span>
                    </span>
                  </Link>
                </li>
              )
            })}
          </ul>
        </section>
      ))}
    </div>
  )
}
