import { useState, type FormEvent } from 'react'
import { assetApi } from '../api'
import { ApiError } from '../api/client'
import { formatNumber } from '../utils/format'
import { useAsync } from '../utils/useAsync'

/** 입력 중인 숫자 문자열: 숫자와 소수점 하나만 남긴다 */
const decimalOnly = (v: string) => v.replace(/[^\d.]/g, '').replace(/(\..*)\./g, '$1')

/**
 * 금 현물 보유량 입력. 보유량(g)·평균 매입 단가(원/g)만 넣으면
 * 서버가 KRX 금 시세로 평가해 '금' 묶음과 비중 카드에 반영한다.
 */
export default function GoldEditor({ onSaved }: { onSaved: () => void }) {
  const gold = useAsync(() => assetApi.gold(), [])
  const [open, setOpen] = useState(false)
  const [grams, setGrams] = useState('')
  const [avgPrice, setAvgPrice] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const data = gold.data
  const has = !!data && data.grams > 0

  const start = () => {
    setGrams(has ? String(data.grams) : '')
    setAvgPrice(has ? String(data.avgPrice) : '')
    setError('')
    setOpen(true)
  }

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setSaving(true)
    try {
      await assetApi.saveGold(Number(grams || 0), Number(avgPrice || 0))
      setOpen(false)
      gold.reload()
      onSaved()
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : String(err))
    } finally {
      setSaving(false)
    }
  }

  if (!data) return null

  // 시세 안내: 기준일이 있으면 받은 시세, 없으면 매입 단가로 평가 중
  const priceNote = !data.priceApiReady
    ? '시세 API 키가 아직 없어 매입 단가로 평가합니다.'
    : data.priceDate
      ? `KRX 금 시세 ${formatNumber(data.currentPrice ?? 0)}원/g (${data.priceDate.slice(5).replace('-', '.')} 기준)`
      : '시세를 아직 받지 못해 매입 단가로 평가합니다.'

  if (!open) {
    return (
      <div className="gold-entry">
        <button type="button" className="link" onClick={start}>
          {has ? '금 보유량 수정' : '+ 금 보유량 입력'}
        </button>
        {has && <span className="muted">{priceNote}</span>}
      </div>
    )
  }

  return (
    <form className="gold-form" onSubmit={submit}>
      <div className="gold-form-title">금 현물 (한국투자증권 금계좌 등)</div>
      <div className="gold-form-fields">
        <label>
          보유량
          <span className="gold-input">
            <input value={grams} onChange={(e) => setGrams(decimalOnly(e.target.value))} inputMode="decimal" placeholder="0" autoFocus />
            <span>g</span>
          </span>
        </label>
        <label>
          평균 매입 단가
          <span className="gold-input">
            <input value={avgPrice} onChange={(e) => setAvgPrice(decimalOnly(e.target.value))} inputMode="decimal" placeholder="0" />
            <span>원/g</span>
          </span>
        </label>
      </div>
      <p className="muted small">{priceNote} 보유량을 0으로 저장하면 금 자산이 지워집니다.</p>
      {error && <p className="error small">{error}</p>}
      <div className="gold-form-actions">
        <button type="button" className="secondary" onClick={() => setOpen(false)}>
          취소
        </button>
        <button type="submit" disabled={saving}>
          저장
        </button>
      </div>
    </form>
  )
}
