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
  // 서버가 관리자 여부를 판단하므로(로컬 자동 로그인 포함) 확인이 끝날 때까지 대기
  const [ready, setReady] = useState(false)

  const logout = useCallback(() => {
    tokenStore.clear()
    setIsAdmin(false)
  }, [])

  useEffect(() => {
    onUnauthorized(logout)

    // 저장된 토큰이 유효하거나 서버가 로컬 자동 로그인(dlog.admin.auto-login) 중이면 관리자
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
