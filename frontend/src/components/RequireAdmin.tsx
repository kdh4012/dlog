import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from '../auth/AuthContext'

/** 관리자 전용 라우트 가드. 미로그인 시 로그인 후 원래 페이지로 복귀 */
export default function RequireAdmin() {
  const { isAdmin, ready } = useAuth()
  const location = useLocation()

  if (!ready) return null
  if (!isAdmin) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}
