import { NavLink, Outlet, useNavigate } from 'react-router'
import { useAuth } from '../auth/AuthContext'

const CURRENT_YEAR = new Date().getFullYear()

export default function Layout() {
  const { isAdmin, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <div className="layout">
      <header className="header">
        <NavLink to="/" className="logo">
          dlog
        </NavLink>
        <nav className="nav">
          <NavLink to="/posts">블로그</NavLink>
          <NavLink to="/work-logs">작업 기록</NavLink>
          {isAdmin && <NavLink to="/assets">자산</NavLink>}
          {isAdmin ? (
            <button type="button" className="link" onClick={handleLogout}>
              로그아웃
            </button>
          ) : (
            <NavLink to="/login">로그인</NavLink>
          )}
        </nav>
      </header>
      <main className="main">
        <Outlet />
      </main>
      <footer className="footer">© {CURRENT_YEAR} 동현</footer>
    </div>
  )
}
