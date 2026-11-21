import { http } from './client'
import type {
  Asset,
  AssetRequest,
  AssetSummary,
  Ledger,
  LedgerItem,
  LedgerItemRequest,
  PageResponse,
  Post,
  PostRequest,
  PostSummary,
  StudyCard,
  StudyCardRequest,
  StudyCategory,
  SyncStatus,
  WorkLog,
  WorkLogRequest,
} from './types'

export const authApi = {
  login: (username: string, password: string) =>
    http.post<{ accessToken: string; expiresAt: string }>('/auth/login', { username, password }),
  me: () => http.get<{ username: string }>('/auth/me'),
}

export const postApi = {
  list: (page = 0, category?: string) => {
    const q = new URLSearchParams({ page: String(page) })
    if (category) q.set('category', category)
    return http.get<PageResponse<PostSummary>>(`/posts?${q}`)
  },
  get: (id: number) => http.get<Post>(`/posts/${id}`),
  create: (body: PostRequest) => http.post<Post>('/posts', body),
  update: (id: number, body: PostRequest) => http.put<Post>(`/posts/${id}`, body),
  remove: (id: number) => http.delete(`/posts/${id}`),
}

export const workLogApi = {
  list: () => http.get<WorkLog[]>('/work-logs'),
  get: (id: number) => http.get<WorkLog>(`/work-logs/${id}`),
  create: (body: WorkLogRequest) => http.post<WorkLog>('/work-logs', body),
  update: (id: number, body: WorkLogRequest) => http.put<WorkLog>(`/work-logs/${id}`, body),
  remove: (id: number) => http.delete(`/work-logs/${id}`),
}

export const assetApi = {
  list: () => http.get<Asset[]>('/assets'),
  summary: () => http.get<AssetSummary>('/assets/summary'),
  create: (body: AssetRequest) => http.post<Asset>('/assets', body),
  update: (id: number, body: AssetRequest) => http.put<Asset>(`/assets/${id}`, body),
  remove: (id: number) => http.delete(`/assets/${id}`),
  syncStatuses: () => http.get<SyncStatus[]>('/assets/sync'),
  sync: () => http.post<SyncStatus[]>('/assets/sync', {}),
}

export const ledgerApi = {
  /** 빈 달이면 서버가 이전 달 항목을 이월한 뒤 돌려준다 */
  get: (month: string) => http.get<Ledger>(`/ledger?month=${month}`),
  create: (body: LedgerItemRequest) => http.post<LedgerItem>('/ledger', body),
  update: (id: number, body: LedgerItemRequest) => http.put<LedgerItem>(`/ledger/${id}`, body),
  remove: (id: number) => http.delete(`/ledger/${id}`),
}


export const studyApi = {
  list: (category?: StudyCategory) => http.get<StudyCard[]>(category ? `/study-cards?category=${category}` : '/study-cards'),
  get: (id: number) => http.get<StudyCard>(`/study-cards/${id}`),
  create: (body: StudyCardRequest) => http.post<StudyCard>('/study-cards', body),
  update: (id: number, body: StudyCardRequest) => http.put<StudyCard>(`/study-cards/${id}`, body),
  setMemorized: (id: number, memorized: boolean) =>
    http.patch<StudyCard>(`/study-cards/${id}/memorized`, { memorized }),
  remove: (id: number) => http.delete(`/study-cards/${id}`),
  importAll: (cards: StudyCardRequest[]) =>
    http.post<{ imported: number }>('/study-cards/import', { cards }),
}
