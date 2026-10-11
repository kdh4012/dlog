import { BrowserRouter, Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthContext'
import Layout from './components/Layout'
import RequireAdmin from './components/RequireAdmin'
import AssetPage from './pages/AssetPage'
import ExpensePage from './pages/ExpensePage'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import NotFoundPage from './pages/NotFoundPage'
import PostDetailPage from './pages/PostDetailPage'
import PostEditPage from './pages/PostEditPage'
import PostListPage from './pages/PostListPage'
import WorkLogEditPage from './pages/WorkLogEditPage'
import WorkLogPage from './pages/WorkLogPage'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<HomePage />} />
            <Route path="login" element={<LoginPage />} />
            <Route path="posts" element={<PostListPage />} />
            <Route path="posts/:id" element={<PostDetailPage />} />
            <Route path="work-logs" element={<WorkLogPage />} />

            {/* 관리자 전용 */}
            <Route element={<RequireAdmin />}>
              <Route path="posts/new" element={<PostEditPage />} />
              <Route path="posts/:id/edit" element={<PostEditPage />} />
              <Route path="work-logs/new" element={<WorkLogEditPage />} />
              <Route path="work-logs/:id/edit" element={<WorkLogEditPage />} />
              <Route path="assets" element={<AssetPage />} />
              <Route path="expenses" element={<ExpensePage />} />
            </Route>

            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
