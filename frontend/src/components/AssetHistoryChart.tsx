import { useMemo, useState, type PointerEvent } from 'react'
import { assetApi } from '../api'
import type { AssetHistoryPoint } from '../api/types'
import { formatNumber, profitClass } from '../utils/format'
import { useAsync } from '../utils/useAsync'

type Period = '1M' | '3M' | '1Y' | 'ALL'

const PERIODS: [Period, string, number | null][] = [
  ['1M', '1개월', 1],
  ['3M', '3개월', 3],
  ['1Y', '1년', 12],
  ['ALL', '전체', null],
]

// SVG 좌표계 (가로는 화면 폭에 맞춰 늘어난다)
const W = 600
const H = 160
const PAD_Y = 12

const won = (v: number) => `${formatNumber(Math.round(v))}원`

/** yyyy-MM-dd 에서 n 개월 전 (같은 형식) */
const monthsBefore = (date: string, n: number) => {
  const d = new Date(`${date}T00:00:00Z`)
  d.setUTCMonth(d.getUTCMonth() - n)
  return d.toISOString().slice(0, 10)
}

/** 2026-11-26 → 11.26 (연도가 다르면 26.11.26) */
const shortDate = (date: string, withYear: boolean) =>
  withYear ? `${date.slice(2, 4)}.${date.slice(5, 7)}.${date.slice(8)}` : `${date.slice(5, 7)}.${date.slice(8)}`

/**
 * 총 자산 추이 (토스·업비트·금 합계, 원화 평가금액). 동기화할 때마다 그날 값이 기록된다.
 * 그래프를 누르거나 마우스를 올리면 그날 값을 보여준다.
 */
export default function AssetHistoryChart() {
  const history = useAsync(() => assetApi.history(), [])
  const [period, setPeriod] = useState<Period>('3M')
  const [hover, setHover] = useState<number | null>(null)

  const points: AssetHistoryPoint[] = useMemo(() => {
    const all = history.data ?? []
    const months = PERIODS.find(([p]) => p === period)![2]
    if (!months || all.length === 0) return all
    const from = monthsBefore(all[all.length - 1].date, months)
    return all.filter((p) => p.date >= from)
  }, [history.data, period])

  if (!history.data) return null

  const header = (
    <div className="hist-head">
      <span className="hist-title">총 자산 추이</span>
      <div className="seg" role="group" aria-label="기간">
        {PERIODS.map(([p, label]) => (
          <button key={p} type="button" className={period === p ? 'on' : ''} aria-pressed={period === p} onClick={() => setPeriod(p)}>
            {label}
          </button>
        ))}
      </div>
    </div>
  )

  if (points.length < 2) {
    return (
      <section className="hist">
        {header}
        <p className="hist-empty muted">총 자산은 매일 자동으로 기록됩니다. 이틀 이상 쌓이면 추이가 보여요.</p>
      </section>
    )
  }

  // 세로 범위: 기간 안 평가금액의 최소~최대 (변화가 잘 보이도록 0 부터 그리지 않는다)
  const values = points.map((p) => p.evalKrw)
  const min = Math.min(...values)
  const max = Math.max(...values)
  const span = max - min || max || 1
  const x = (i: number) => (i / (points.length - 1)) * W
  const y = (v: number) => PAD_Y + (1 - (v - min) / span) * (H - PAD_Y * 2)

  const evalLine = points.map((p, i) => `${i ? 'L' : 'M'}${x(i).toFixed(1)},${y(p.evalKrw).toFixed(1)}`).join(' ')
  const area = `${evalLine} L${W},${H} L0,${H} Z`

  const first = points[0]
  const last = points[points.length - 1]
  const change = last.evalKrw - first.evalKrw
  const changeRate = first.evalKrw ? (change / first.evalKrw) * 100 : 0
  const multiYear = first.date.slice(0, 4) !== last.date.slice(0, 4)

  const shown = hover !== null ? points[hover] : last

  const handleMove = (e: PointerEvent<SVGSVGElement>) => {
    const rect = e.currentTarget.getBoundingClientRect()
    const ratio = Math.min(Math.max((e.clientX - rect.left) / rect.width, 0), 1)
    setHover(Math.round(ratio * (points.length - 1)))
  }

  return (
    <section className="hist">
      {header}

      <div className="hist-summary">
        <span className="hist-value">{won(shown.evalKrw)}</span>
        <span className="muted">{shortDate(shown.date, true)}</span>
        <span className={`hist-change ${profitClass(change)}`}>
          기간 {change > 0 ? '+' : change < 0 ? '-' : ''}
          {won(Math.abs(change))} ({change > 0 ? '+' : change < 0 ? '-' : ''}
          {Math.abs(changeRate).toFixed(1)}%)
        </span>
      </div>

      <div className="hist-chart-wrap">
      <svg
        className="hist-chart"
        viewBox={`0 0 ${W} ${H}`}
        preserveAspectRatio="none"
        onPointerMove={handleMove}
        onPointerDown={handleMove}
        onPointerLeave={() => setHover(null)}
        role="img"
        aria-label={`총 자산 ${shortDate(first.date, true)} ${won(first.evalKrw)} → ${shortDate(last.date, true)} ${won(last.evalKrw)}`}
      >
        <defs>
          <linearGradient id="hist-fill" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stopColor="var(--accent)" stopOpacity="0.22" />
            <stop offset="100%" stopColor="var(--accent)" stopOpacity="0" />
          </linearGradient>
        </defs>
        <path d={area} fill="url(#hist-fill)" />
        <path d={evalLine} className="hist-eval" vectorEffect="non-scaling-stroke" />
        {hover !== null && <line x1={x(hover)} x2={x(hover)} y1={0} y2={H} className="hist-cursor" vectorEffect="non-scaling-stroke" />}
      </svg>
      {/* SVG 는 가로로 늘어나므로 점은 HTML 로 올려 동그랗게 유지한다 */}
      {hover !== null && (
        <span
          className="hist-dot"
          style={{ left: `${(x(hover) / W) * 100}%`, top: `${(y(points[hover].evalKrw) / H) * 100}%` }}
          aria-hidden="true"
        />
      )}
      </div>

      <div className="hist-axis muted">
        <span>{shortDate(first.date, multiYear)}</span>
        <span>{shortDate(last.date, multiYear)}</span>
      </div>
    </section>
  )
}
