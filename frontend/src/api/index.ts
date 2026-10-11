import { http } from './client'
import type {
  Asset,
  AssetRequest,
  AssetSummary,
  PageResponse,
  Post,
  PostRequest,
  PostSummary,
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
}
