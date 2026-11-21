import { useCallback, useMemo, useState } from 'react'
import { Link } from 'react-router'
import { studyApi } from '../api'
import { STUDY_CATEGORIES, STUDY_TYPE_KEYS, STUDY_TYPES, type StudyCard, type StudyCategory, type StudyType } from '../api/types'
import Markdown from '../components/Markdown'
import { ErrorMessage, Loading } from '../components/Status'
import { useAsync } from '../utils/useAsync'

type Filter = StudyCategory | 'ALL'

const CATEGORY_KEYS = Object.keys(STUDY_CATEGORIES) as StudyCategory[]
// 서버는 분류 이름(문자열) 순으로 주므로, 화면에서는 탭 순서대로 다시 정렬한다
const CATEGORY_RANK = new Map(CATEGORY_KEYS.map((k, i) => [k, i]))
const byCategoryThenOrder = (a: StudyCard, b: StudyCard) =>
  (CATEGORY_RANK.get(a.category) ?? 0) - (CATEGORY_RANK.get(b.category) ?? 0) || a.sortOrder - b.sortOrder || a.id - b.id

/** Fisher-Yates 셔플: 원본 배열은 건드리지 않는다 */
function shuffled<T>(items: T[]): T[] {
  const copy = [...items]
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[copy[i], copy[j]] = [copy[j], copy[i]]
  }
  return copy
}

/** 카드 아래 작은 설명: 면접 질문 / 기술·업무 · 업무, 도메인 */
const cardLabel = (c: StudyCard) =>
  c.category === 'KNOWLEDGE' && c.types.length > 0
    ? `${STUDY_CATEGORIES[c.category]} · ${c.types.map((t) => STUDY_TYPES[t]).join(', ')}`
    : STUDY_CATEGORIES[c.category]

export default function StudyPage() {
  const { data, error, loading, reload } = useAsync(() => studyApi.list(), [])
  const [filter, setFilter] = useState<Filter>('ALL')
  // 기술·업무 탭의 유형 필터 (여러 개 선택, 비어 있으면 전체). 고른 유형 중 하나라도 있으면 보여준다
  const [typeFilter, setTypeFilter] = useState<Set<StudyType>>(new Set())
  // 답을 펼친 카드
  const [opened, setOpened] = useState<Set<number>>(new Set())
  // 섞기를 누르면 id 순서를 기억해 두고 그 순서대로 보여 준다
  const [shuffleOrder, setShuffleOrder] = useState<number[] | null>(null)

  const matches = useCallback(
    (c: StudyCard) =>
      (filter === 'ALL' || c.category === filter) &&
      (filter !== 'KNOWLEDGE' || typeFilter.size === 0 || c.types.some((t) => typeFilter.has(t))),
    [filter, typeFilter],
  )

  const cards = useMemo(() => {
    let list = (data ?? []).filter(matches).sort(byCategoryThenOrder)
    if (shuffleOrder) {
      const rank = new Map(shuffleOrder.map((id, i) => [id, i]))
      list = [...list].sort((a, b) => (rank.get(a.id) ?? 0) - (rank.get(b.id) ?? 0))
    }
    return list
  }, [data, matches, shuffleOrder])

  const countOf = (f: Filter) => (data ?? []).filter((c) => f === 'ALL' || c.category === f).length

  const toggle = <T,>(set: Set<T>, value: T) => {
    const next = new Set(set)
    if (next.has(value)) next.delete(value)
    else next.add(value)
    return next
  }

  const changeFilter = (f: Filter) => {
    setFilter(f)
    setTypeFilter(new Set())
    setOpened(new Set())
  }

  const handleShuffle = () => {
    setShuffleOrder(shuffled((data ?? []).map((c) => c.id)))
    setOpened(new Set())
  }

  const handleDelete = async (id: number) => {
    if (!confirm('이 카드를 삭제할까요?')) return
    await studyApi.remove(id)
    reload()
  }

  return (
    <div className="study">
      <div className="section-head">
        <h1>학습 노트</h1>
        <Link to="/study/new" className="button">
          카드 추가
        </Link>
      </div>

      <div className="study-bar">
        <div className="seg" role="group" aria-label="분류">
          {(['ALL', ...CATEGORY_KEYS] as Filter[]).map((f) => (
            <button key={f} type="button" className={filter === f ? 'on' : ''} aria-pressed={filter === f} onClick={() => changeFilter(f)}>
              {f === 'ALL' ? '전체' : STUDY_CATEGORIES[f]} <span className="seg-count">{countOf(f)}</span>
            </button>
          ))}
        </div>
        <div className="study-shuffle">
          {shuffleOrder && (
            <button type="button" className="link" onClick={() => setShuffleOrder(null)}>
              원래 순서
            </button>
          )}
          <button type="button" className="link" onClick={handleShuffle}>
            섞기
          </button>
        </div>
      </div>

      {filter === 'KNOWLEDGE' && (
        <div className="study-types" role="group" aria-label="유형 (여러 개 선택)">
          {STUDY_TYPE_KEYS.map((t) => (
            <button
              key={t}
              type="button"
              className={`chip${typeFilter.has(t) ? ' on' : ''}`}
              aria-pressed={typeFilter.has(t)}
              onClick={() => setTypeFilter((prev) => toggle(prev, t))}
            >
              {STUDY_TYPES[t]}
            </button>
          ))}
        </div>
      )}

      {loading && <Loading />}
      {error && <ErrorMessage error={error} />}

      <ul className="study-list">
        {cards.map((c) => {
          const open = opened.has(c.id)
          return (
            <li key={c.id} className={open ? 'open' : undefined}>
              <button type="button" className="study-q" onClick={() => setOpened((prev) => toggle(prev, c.id))} aria-expanded={open}>
                <span className="study-q-text">
                  <span className="study-q-title">{c.question}</span>
                  <span className="study-q-label">{cardLabel(c)}</span>
                </span>
                <span className="study-chevron" aria-hidden="true" />
              </button>
              {open && (
                <div className="study-a">
                  <Markdown>{c.answer}</Markdown>
                  <div className="study-a-actions">
                    <Link to={`/study/${c.id}/edit`}>수정</Link>
                    <button type="button" className="link danger" onClick={() => handleDelete(c.id)}>
                      삭제
                    </button>
                  </div>
                </div>
              )}
            </li>
          )
        })}
      </ul>
      {data && cards.length === 0 && <p className="empty">표시할 카드가 없습니다.</p>}
    </div>
  )
}
