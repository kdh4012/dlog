import { NavLink, Outlet, useNavigate } from 'react-router'
import { useAuth } from '../auth/AuthContext'

const CURRENT_YEAR = new Date().getFullYear()

const EXTERNAL_LINKS = [
  { label: 'GitHub', href: 'https://github.com/kdh4012' },
  { label: '복순앱', href: 'https://github.com/kim401233-ui/boksoon' },
]

function GitHubIcon() {
  return (
    <svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true" fill="currentColor">
      <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z" />
    </svg>
  )
}

function ExternalLinks() {
  return (
    <ul className="links">
      {EXTERNAL_LINKS.map((l) => (
        <li key={l.href}>
          <a href={l.href} target="_blank" rel="noopener noreferrer">
            <GitHubIcon />
            {l.label}
          </a>
        </li>
      ))}
    </ul>
  )
}

export default function Layout() {
  const { isAdmin, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <NavLink to="/" className="brand">
          <span className="avatar">D</span>
          <span className="brand-text">
            <strong>DLog</strong>
            <small>동현의 개발 기록</small>
          </span>
        </NavLink>
        <p className="bio">C++/MFC 공장 자동화와 Java/Spring 웹 개발을 합니다.</p>
        <ExternalLinks />
        <nav className="nav">
          <NavLink to="/" end>
            홈
          </NavLink>
          <NavLink to="/posts">블로그</NavLink>
          <NavLink to="/work-logs">프로젝트</NavLink>
          {isAdmin && <NavLink to="/assets">자산 포트폴리오</NavLink>}
          {isAdmin && <NavLink to="/expenses">가계부</NavLink>}
        </nav>
        <div className="sidebar-foot">
          {isAdmin ? (
            <button type="button" className="link" onClick={handleLogout}>
              로그아웃
            </button>
          ) : (
            <NavLink to="/login">로그인</NavLink>
          )}
          <span>© {CURRENT_YEAR} 동현</span>
        </div>
      </aside>
      <main className="main">
        <Outlet />
        {/* 모바일에서는 사이드바 링크가 숨겨지므로 본문 하단에 노출 */}
        <footer className="mobile-foot">
          <ExternalLinks />
        </footer>
      </main>
    </div>
  )
}
