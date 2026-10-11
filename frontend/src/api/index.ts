import { http } from './client'
import type {
  Asset,
  AssetRequest,
  AssetSummary,
  Expense,
  ExpenseRequest,
  ExpenseSummary,
  FixedExpense,
  FixedExpenseRequest,
  PageResponse,
  Post,
  PostRequest,
  PostSummary,
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

export const expenseApi = {
  list: (month: string) => http.get<Expense[]>(`/expenses?month=${month}`),
  summary: (month: string) => http.get<ExpenseSummary>(`/expenses/summary?month=${month}`),
  create: (body: ExpenseRequest) => http.post<Expense>('/expenses', body),
  update: (id: number, body: ExpenseRequest) => http.put<Expense>(`/expenses/${id}`, body),
  remove: (id: number) => http.delete(`/expenses/${id}`),
  /** amount 0 이면 해당 월 예산 삭제 (이전 달 예산을 이어서 사용) */
  setBudget: (month: string, amount: number) => http.put<void>('/expenses/budget', { month, amount }),
}

export const fixedExpenseApi = {
  list: () => http.get<FixedExpense[]>('/fixed-expenses'),
  /** 빈 종료 월은 null(계속)로 보낸다 */
  create: (body: FixedExpenseRequest) => http.post<FixedExpense>('/fixed-expenses', { ...body, endMonth: body.endMonth || null }),
  update: (id: number, body: FixedExpenseRequest) =>
    http.put<FixedExpense>(`/fixed-expenses/${id}`, { ...body, endMonth: body.endMonth || null }),
  remove: (id: number) => http.delete(`/fixed-expenses/${id}`),
}
