import { http } from './client'
import type {
  Asset,
  AssetRequest,
  AssetSummary,
  Diary,
  DiaryRequest,
  DiarySummary,
  Goal,
  GoldHolding,
  Ledger,
  LedgerItem,
  LedgerItemRequest,
  PageResponse,
  Resolution,
  StudyCard,
  StudyCardRequest,
  StudyCategory,
  SyncStatus,
} from './types'

export const authApi = {
  login: (username: string, password: string) =>
    http.post<{ accessToken: string; expiresAt: string }>('/auth/login', { username, password }),
  me: () => http.get<{ username: string }>('/auth/me'),
}

export const resolutionApi = {
  get: () => http.get<Resolution>('/resolution'),
  /** 빈 문자열이면 지운다 */
  save: (content: string) => http.put<Resolution>('/resolution', { content }),
}

export const goalApi = {
  list: (year: number) => http.get<Goal[]>(`/goals?year=${year}`),
  create: (year: number, title: string) => http.post<Goal>('/goals', { year, title }),
  update: (id: number, title: string) => http.put<Goal>(`/goals/${id}`, { title }),
  setDone: (id: number, done: boolean) => http.patch<Goal>(`/goals/${id}/done`, { done }),
  /** 그 해 목표 id 전체를 보여줄 순서대로 */
  reorder: (year: number, ids: number[]) => http.put<Goal[]>('/goals/order', { year, ids }),
  remove: (id: number) => http.delete(`/goals/${id}`),
}

export const diaryApi = {
  /** 최근 날짜 순 */
  list: (page: number, size = 20) => http.get<PageResponse<DiarySummary>>(`/diary?page=${page}&size=${size}`),
  get: (id: number) => http.get<Diary>(`/diary/${id}`),
  create: (body: DiaryRequest) => http.post<Diary>('/diary', body),
  update: (id: number, body: DiaryRequest) => http.put<Diary>(`/diary/${id}`, body),
  remove: (id: number) => http.delete(`/diary/${id}`),
}

export const assetApi = {
  list: () => http.get<Asset[]>('/assets'),
  summary: () => http.get<AssetSummary>('/assets/summary'),
  create: (body: AssetRequest) => http.post<Asset>('/assets', body),
  update: (id: number, body: AssetRequest) => http.put<Asset>(`/assets/${id}`, body),
  remove: (id: number) => http.delete(`/assets/${id}`),
  syncStatuses: () => http.get<SyncStatus[]>('/assets/sync'),
  sync: () => http.post<SyncStatus[]>('/assets/sync', {}),
  gold: () => http.get<GoldHolding>('/assets/gold'),
  /** 보유량 0 이면 금 자산을 지운다 */
  saveGold: (grams: number, avgPrice: number) => http.put<GoldHolding>('/assets/gold', { grams, avgPrice }),
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
  remove: (id: number) => http.delete(`/study-cards/${id}`),
}
