import { useState } from 'react'
import { assetApi } from '../api'
import { ApiError } from '../api/client'
import { ASSET_SOURCES, type SyncStatus } from '../api/types'
import { useAsync } from '../utils/useAsync'
import { ErrorMessage } from './Status'

const formatDateTime = (iso: string) => iso.slice(0, 16).replace('T', ' ')

function StatusLine({ status }: { status: SyncStatus }) {
  if (!status.configured) return <span className="muted">API 키 미설정</span>
  if (!status.lastSyncedAt) return <span className="muted">동기화 대기 중</span>
  if (!status.success) return <span className="error">{status.error}</span>
  return (
    <span className="muted">
      {formatDateTime(status.lastSyncedAt)} · {status.assetCount}건
    </span>
  )
}

/** 업비트·토스증권 동기화 상태와 수동 동기화 버튼 */
export default function AssetSyncPanel({ onSynced }: { onSynced: () => void }) {
  const statuses = useAsync(() => assetApi.syncStatuses(), [])
  const [syncing, setSyncing] = useState(false)
  const [error, setError] = useState('')
  const [latest, setLatest] = useState<SyncStatus[] | null>(null)

  const list = latest ?? statuses.data
  const anyConfigured = list?.some((s) => s.configured) ?? false

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
    <section className="sync-panel">
      <ul className="sync-list">
        {list?.map((s) => (
          <li key={s.source}>
            <span className={`sync-dot ${!s.configured ? 'off' : s.lastSyncedAt && !s.success ? 'fail' : 'on'}`} />
            <strong>{ASSET_SOURCES[s.source]}</strong>
            <StatusLine status={s} />
          </li>
        ))}
      </ul>
      <button type="button" className="secondary" onClick={handleSync} disabled={syncing || !anyConfigured}>
        {syncing ? '동기화 중...' : '지금 동기화'}
      </button>
      {statuses.error && <ErrorMessage error={statuses.error} />}
      {error && <p className="error">{error}</p>}
    </section>
  )
}
