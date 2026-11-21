import { useMemo, useRef, useState, type ChangeEvent } from 'react'
import { Link } from 'react-router'
import { studyApi } from '../api'
import { ApiError } from '../api/client'
import { STUDY_CATEGORIES, type StudyCard, type StudyCardRequest, type StudyCategory } from '../api/types'
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

/** 가져오기 파일: 카드 배열 또는 { cards: [...] } 둘 다 받는다 */
function parseImportFile(text: string): StudyCardRequest[] {
  const json: unknown = JSON.parse(text)
  const list = Array.isArray(json) ? json : (json as { cards?: unknown }).cards
  if (!Array.isArray(list)) throw new Error('카드 배열을 찾을 수 없습니다.')
  return list.map((c: Partial<StudyCardRequest>, i) => {
    if (!c.category || !(c.category in STUDY_CATEGORIES) || !c.question || !c.answer) {
      throw new Error(`${i + 1}번째 카드의 category, question, answer 를 확인해 주세요.`)
    }
    return { category: c.category, question: c.question, answer: c.answer, sortOrder: Number(c.sortOrder) || 0 }
  })
}

/** 현재 카드를 가져오기와 같은 형식의 JSON 파일로 저장 (백업용) */
function downloadCards(cards: StudyCard[]) {
  const body = {
    cards: cards.map(({ category, question, answer, sortOrder }) => ({ category, question, answer, sortOrder })),
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
  const [onlyUnmemorized, setOnlyUnmemorized] = useState(false)
  const [showAll, setShowAll] = useState(false)
  // 카드별로 답을 펼쳤는지 (showAll 이 켜지면 전부 펼친 것으로 본다)
  const [opened, setOpened] = useState<Set<number>>(new Set())
  // 섞기를 누르면 id 순서를 기억해 두고 그 순서대로 보여 준다
  const [shuffleOrder, setShuffleOrder] = useState<number[] | null>(null)
  // 외움 표시는 서버 응답을 기다리지 않고 화면에 먼저 반영한다
  const [memorizedOverride, setMemorizedOverride] = useState<Map<number, boolean>>(new Map())

  const cards = useMemo(() => {
    const all = (data ?? []).map((c) =>
      memorizedOverride.has(c.id) ? { ...c, memorized: memorizedOverride.get(c.id)! } : c,
    )
    let list = all
      .filter((c) => (filter === 'ALL' || c.category === filter) && (!onlyUnmemorized || !c.memorized))
      .sort(byCategoryThenOrder)
    if (shuffleOrder) {
      const rank = new Map(shuffleOrder.map((id, i) => [id, i]))
      list = [...list].sort((a, b) => (rank.get(a.id) ?? 0) - (rank.get(b.id) ?? 0))
    }
    return list
  }, [data, filter, onlyUnmemorized, shuffleOrder, memorizedOverride])

  const total = (data ?? []).filter((c) => filter === 'ALL' || c.category === filter)
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
    setOpened(new Set())
  }

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
                <span className="tag">{STUDY_CATEGORIES[c.category]}</span>
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
