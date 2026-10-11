import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { authApi } from '../api'
import { onUnauthorized, tokenStore } from '../api/client'

interface AuthState {
  isAdmin: boolean
  /** 저장된 토큰 검증이 끝났는지 여부 (새로고침 직후 깜빡임 방지) */
  ready: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAdmin, setIsAdmin] = useState(false)
  // 저장된 토큰이 없으면 검증할 것이 없으므로 바로 ready
  const [ready, setReady] = useState(() => !tokenStore.get())

  const logout = useCallback(() => {
    tokenStore.clear()
    setIsAdmin(false)
  }, [])

  useEffect(() => {
    onUnauthorized(logout)

    // 새로고침 시 저장된 토큰이 아직 유효한지 서버에 확인
    if (!tokenStore.get()) return
    authApi
      .me()
      .then(() => setIsAdmin(true))
      .catch(() => tokenStore.clear())
      .finally(() => setReady(true))
  }, [logout])

  const login = useCallback(async (username: string, password: string) => {
    const { accessToken } = await authApi.login(username, password)
    tokenStore.set(accessToken)
    setIsAdmin(true)
  }, [])

  const value = useMemo(() => ({ isAdmin, ready, login, logout }), [isAdmin, ready, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// oxlint-disable-next-line react/only-export-components -- Provider 와 훅을 한 파일에서 관리
export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth 는 AuthProvider 내부에서만 사용할 수 있습니다.')
  return ctx
}
