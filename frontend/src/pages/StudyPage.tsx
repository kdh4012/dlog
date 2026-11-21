import { useCallback, useMemo, useRef, useState, type ChangeEvent } from 'react'
import { Link } from 'react-router'
import { studyApi } from '../api'
import { ApiError } from '../api/client'
import {
  STUDY_CATEGORIES,
  STUDY_TYPE_KEYS,
  STUDY_TYPES,
  type StudyCard,
  type StudyCardRequest,
  type StudyCategory,
  type StudyType,
} from '../api/types'
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

/** 예전 분류(내보내기 파일)를 지금의 분류·유형으로 바꾼다 */
const LEGACY_CATEGORIES: Record<string, { category: StudyCategory; types: StudyType[] }> = {
  INTRO: { category: 'INTERVIEW', types: [] },
  DOMAIN: { category: 'KNOWLEDGE', types: ['WORK', 'DOMAIN'] },
  TECH: { category: 'KNOWLEDGE', types: ['TECH'] },
  TROUBLE: { category: 'KNOWLEDGE', types: ['TROUBLE'] },
}

/** 가져오기 파일: 카드 배열 또는 { cards: [...] } 둘 다 받는다 */
function parseImportFile(text: string): StudyCardRequest[] {
  const json: unknown = JSON.parse(text)
  const list = Array.isArray(json) ? json : (json as { cards?: unknown }).cards
  if (!Array.isArray(list)) throw new Error('카드 배열을 찾을 수 없습니다.')
  return list.map((raw: Partial<Omit<StudyCardRequest, 'category' | 'types'>> & { category?: string; types?: string[] }, i) => {
    const legacy = raw.category ? LEGACY_CATEGORIES[raw.category] : undefined
    const category = (legacy?.category ?? raw.category) as StudyCategory | undefined
    const types = (legacy?.types ?? raw.types ?? []).filter((t): t is StudyType => t in STUDY_TYPES)
    if (!category || !(category in STUDY_CATEGORIES) || !raw.question || !raw.answer) {
      throw new Error(`${i + 1}번째 카드의 category, question, answer 를 확인해 주세요.`)
    }
    return { category, types, question: raw.question, answer: raw.answer, sortOrder: Number(raw.sortOrder) || 0 }
  })
}

/** 현재 카드를 가져오기와 같은 형식의 JSON 파일로 저장 (백업용) */
function downloadCards(cards: StudyCard[]) {
  const body = {
    cards: cards.map(({ category, types, question, answer, sortOrder }) => ({ category, types, question, answer, sortOrder })),
  }
  const url = URL.createObjectURL(new Blob([JSON.stringify(body, null, 2)], { type: 'application/json' }))
  const a = document.createElement('a')
  a.href = url
  a.download = `study-cards-${new Date().toISOString().slice(0, 10)}.json`
  a.click()
  URL.revokeObjectURL(url)
}

export default function StudyPage() {
  const { data, error, loading, reload } = useAsync(() => studyApi.list(), [])
  const [filter, setFilter] = useState<Filter>('ALL')
  // 기술·업무 탭의 유형 필터 (여러 개 선택, 비어 있으면 전체). 고른 유형 중 하나라도 있으면 보여준다
  const [typeFilter, setTypeFilter] = useState<Set<StudyType>>(new Set())
  const [onlyUnmemorized, setOnlyUnmemorized] = useState(false)
  const [showAll, setShowAll] = useState(false)
  // 카드별로 답을 펼쳤는지 (showAll 이 켜지면 전부 펼친 것으로 본다)
  const [opened, setOpened] = useState<Set<number>>(new Set())
  // 섞기를 누르면 id 순서를 기억해 두고 그 순서대로 보여 준다
  const [shuffleOrder, setShuffleOrder] = useState<number[] | null>(null)
  // 외움 표시는 서버 응답을 기다리지 않고 화면에 먼저 반영한다
  const [memorizedOverride, setMemorizedOverride] = useState<Map<number, boolean>>(new Map())

  const matches = useCallback(
    (c: StudyCard) =>
      (filter === 'ALL' || c.category === filter) &&
      (filter !== 'KNOWLEDGE' || typeFilter.size === 0 || c.types.some((t) => typeFilter.has(t))),
    [filter, typeFilter],
  )

  const cards = useMemo(() => {
    const all = (data ?? []).map((c) =>
      memorizedOverride.has(c.id) ? { ...c, memorized: memorizedOverride.get(c.id)! } : c,
    )
    let list = all
      .filter((c) => matches(c) && (!onlyUnmemorized || !c.memorized))
      .sort(byCategoryThenOrder)
    if (shuffleOrder) {
      const rank = new Map(shuffleOrder.map((id, i) => [id, i]))
      list = [...list].sort((a, b) => (rank.get(a.id) ?? 0) - (rank.get(b.id) ?? 0))
    }
    return list
  }, [data, matches, onlyUnmemorized, shuffleOrder, memorizedOverride])

  const total = (data ?? []).filter(matches)
  const memorizedCount = total.filter((c) => memorizedOverride.get(c.id) ?? c.memorized).length

  const toggleOpen = (id: number) =>
    setOpened((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })

  const changeFilter = (f: Filter) => {
    setFilter(f)
    setTypeFilter(new Set())
    setOpened(new Set())
  }

  const toggleType = (t: StudyType) =>
    setTypeFilter((prev) => {
      const next = new Set(prev)
      if (next.has(t)) next.delete(t)
      else next.add(t)
      return next
    })

  const handleShuffle = () => {
    setShuffleOrder(shuffled((data ?? []).map((c) => c.id)))
    setOpened(new Set())
  }

  const handleMemorized = async (card: StudyCard, memorized: boolean) => {
    setMemorizedOverride((m) => new Map(m).set(card.id, memorized))
    try {
      await studyApi.setMemorized(card.id, memorized)
    } catch (e) {
      // 실패하면 화면 표시를 되돌린다
      setMemorizedOverride((m) => {
        const next = new Map(m)
        next.delete(card.id)
        return next
      })
      alert(e instanceof Error ? e.message : String(e))
    }
  }

  const fileInput = useRef<HTMLInputElement>(null)

  const handleImport = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    e.target.value = '' // 같은 파일을 다시 골라도 change 가 발생하도록
    if (!file) return
    try {
      const cards = parseImportFile(await file.text())
      if (!confirm(`카드 ${cards.length}장을 추가할까요? (기존 카드는 그대로 둡니다)`)) return
      const { imported } = await studyApi.importAll(cards)
      alert(`${imported}장을 추가했습니다.`)
      reload()
    } catch (err) {
      alert(err instanceof ApiError ? err.detail : err instanceof Error ? err.message : String(err))
    }
  }

  const handleDelete = async (id: number) => {
    if (!confirm('이 카드를 삭제할까요?')) return
    await studyApi.remove(id)
    reload()
  }

  return (
    <>
      <div className="section-head">
        <h1>학습 노트</h1>
        <div className="head-actions">
          <button type="button" className="secondary" onClick={() => fileInput.current?.click()}>
            가져오기
          </button>
          <button
            type="button"
            className="secondary"
            disabled={!data?.length}
            onClick={() => data && downloadCards([...data].sort(byCategoryThenOrder))}
          >
            내보내기
          </button>
          <Link to="/study/new" className="button">
            추가
          </Link>
          <input ref={fileInput} type="file" accept="application/json,.json" hidden onChange={handleImport} />
        </div>
      </div>

      <div className="study-toolbar">
        <div className="tabs study-tabs">
          <button type="button" className={filter === 'ALL' ? 'active' : ''} onClick={() => changeFilter('ALL')}>
            전체
          </button>
          {CATEGORY_KEYS.map((k) => (
            <button key={k} type="button" className={filter === k ? 'active' : ''} onClick={() => changeFilter(k)}>
              {STUDY_CATEGORIES[k]}
            </button>
          ))}
        </div>
        <div className="study-options">
          <label className="study-check">
            <input type="checkbox" checked={showAll} onChange={(e) => setShowAll(e.target.checked)} />
            답 모두 보기
          </label>
          <label className="study-check">
            <input type="checkbox" checked={onlyUnmemorized} onChange={(e) => setOnlyUnmemorized(e.target.checked)} />
            안 외운 것만
          </label>
          <button type="button" className="secondary small" onClick={handleShuffle}>
            섞기
          </button>
          {shuffleOrder && (
            <button type="button" className="link small" onClick={() => setShuffleOrder(null)}>
              원래 순서
            </button>
          )}
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
              onClick={() => toggleType(t)}
            >
              {STUDY_TYPES[t]}
            </button>
          ))}
          {typeFilter.size > 0 && (
            <button type="button" className="link small" onClick={() => setTypeFilter(new Set())}>
              유형 선택 해제
            </button>
          )}
        </div>
      )}

      {data && (
        <p className="muted study-progress">
          외움 {memorizedCount} / {total.length}
        </p>
      )}

      {loading && <Loading />}
      {error && <ErrorMessage error={error} />}

      <ul className="study-list">
        {cards.map((c) => {
          const open = showAll || opened.has(c.id)
          return (
            <li key={c.id} className={`study-card${c.memorized ? ' done' : ''}`}>
              <button type="button" className="study-question" onClick={() => toggleOpen(c.id)} aria-expanded={open}>
                {/* 기술·업무 카드는 분류 대신 유형을 보여준다 */}
                {c.category === 'KNOWLEDGE' && c.types.length > 0 ? (
                  c.types.map((t) => (
                    <span key={t} className="tag">
                      {STUDY_TYPES[t]}
                    </span>
                  ))
                ) : (
                  <span className="tag">{STUDY_CATEGORIES[c.category]}</span>
                )}
                <span className="study-question-text">{c.question}</span>
                <span className="study-toggle" aria-hidden="true">
                  {open ? '접기' : '답 보기'}
                </span>
              </button>
              {open && (
                <div className="study-answer">
                  <Markdown>{c.answer}</Markdown>
                </div>
              )}
              <div className="actions study-actions">
                <label className="study-check">
                  <input
                    type="checkbox"
                    checked={c.memorized}
                    onChange={(e) => handleMemorized(c, e.target.checked)}
                  />
                  외움
                </label>
                <Link to={`/study/${c.id}/edit`}>수정</Link>
                <button type="button" className="link danger" onClick={() => handleDelete(c.id)}>
                  삭제
                </button>
              </div>
            </li>
          )
        })}
      </ul>
      {data && cards.length === 0 && <p className="muted">표시할 카드가 없습니다.</p>}
    </>
  )
}
