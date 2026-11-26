import { useState } from 'react'
import { assetApi } from '../api'
import { ApiError } from '../api/client'
import { ASSET_SOURCES, type SyncStatus } from '../api/types'
import { useAsync } from '../utils/useAsync'

/** 2026-11-12T17:20:31 → 11.12 17:20 */
const formatDateTime = (iso: string) => `${iso.slice(5, 7)}.${iso.slice(8, 10)} ${iso.slice(11, 16)}`

const statusText = (s: SyncStatus) => {
  if (!s.configured) return s.source === 'GOLD' ? '보유량 미입력' : 'API 키 미설정'
  if (!s.lastSyncedAt) return '동기화 대기 중'
  if (!s.success) return '동기화 실패'
  return formatDateTime(s.lastSyncedAt)
}

/** 화면 하단의 업비트·토스증권 동기화 상태 한 줄 + 새로고침 */
export default function AssetSyncPanel({ onSynced }: { onSynced: () => void }) {
  const statuses = useAsync(() => assetApi.syncStatuses(), [])
  const [syncing, setSyncing] = useState(false)
  const [error, setError] = useState('')
  const [latest, setLatest] = useState<SyncStatus[] | null>(null)

  const list = latest ?? statuses.data
  const anyConfigured = list?.some((s) => s.configured) ?? false
  // 실패 원인(허용 IP 미등록 등)은 길어서 아래에 따로 보여준다
  const failures = list?.filter((s) => s.configured && s.lastSyncedAt && !s.success) ?? []

  const handleSync = async () => {
    setSyncing(true)
    setError('')
    try {
      setLatest(await assetApi.sync())
      onSynced()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : String(err))
    } finally {
      setSyncing(false)
    }
  }

  return (
    <footer className="asset-sync">
      <div className="asset-sync-line">
        <span>
          {list?.map((s) => (
            <span key={s.source} className={`asset-sync-item${failures.includes(s) ? ' fail' : ''}`}>
              {ASSET_SOURCES[s.source]} {statusText(s)}
            </span>
          ))}
        </span>
        <button type="button" className="link" onClick={handleSync} disabled={syncing || !anyConfigured}>
          {syncing ? '동기화 중...' : '지금 동기화'}
        </button>
      </div>
      {failures.map((s) => (
        <p key={s.source} className="error small">
          {ASSET_SOURCES[s.source]}: {s.error}
        </p>
      ))}
      {(error || statuses.error) && <p className="error small">{error || statuses.error?.message}</p>}
    </footer>
  )
}
