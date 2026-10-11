import type { ErrorBody } from './types'

const TOKEN_KEY = 'dlog.token'

export const tokenStore = {
  get: (): string | null => {
    try {
      return localStorage.getItem(TOKEN_KEY)
    } catch {
      return null
    }
  },
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

export class ApiError extends Error {
  readonly status: number
  readonly body: ErrorBody | null

  constructor(status: number, body: ErrorBody | null) {
    super(body?.message ?? `요청에 실패했습니다. (${status})`)
    this.status = status
    this.body = body
  }

  /** 필드 검증 오류까지 포함한 사용자 표시용 메시지 */
  get detail(): string {
    const fields = this.body?.errors?.map((e) => `${e.field}: ${e.reason}`) ?? []
    return [this.message, ...fields].join('\n')
  }
}

/** 401 발생 시 AuthContext 가 로그아웃 처리하도록 등록하는 핸들러 */
let unauthorizedHandler: (() => void) | null = null
export const onUnauthorized = (handler: () => void) => {
  unauthorizedHandler = handler
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  const token = tokenStore.get()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')

  const res = await fetch(`/api${path}`, { ...init, headers })

  if (!res.ok) {
    if (res.status === 401 && token) unauthorizedHandler?.()
    const body = (await res.json().catch(() => null)) as ErrorBody | null
    throw new ApiError(res.status, body)
  }
  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}

export const http = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body: unknown) => request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(path: string, body: unknown) => request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
  delete: (path: string) => request<void>(path, { method: 'DELETE' }),
}
