import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { AuthProvider } from './auth/AuthContext'
import Layout from './components/Layout'
import RequireAdmin from './components/RequireAdmin'
import AssetPage from './pages/AssetPage'
import DiaryDetailPage from './pages/DiaryDetailPage'
import DiaryEditPage from './pages/DiaryEditPage'
import DiaryListPage from './pages/DiaryListPage'
import HomePage from './pages/HomePage'
import LedgerPage from './pages/LedgerPage'
import LoginPage from './pages/LoginPage'
import NotFoundPage from './pages/NotFoundPage'
import StudyEditPage from './pages/StudyEditPage'
import StudyPage from './pages/StudyPage'
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
            {/* 블로그는 일기로 바뀌었다: 예전 주소는 홈으로 */}
            <Route path="posts/*" element={<Navigate to="/" replace />} />
            <Route path="work-logs" element={<WorkLogPage />} />

            {/* 관리자 전용 */}
            <Route element={<RequireAdmin />}>
              <Route path="diary" element={<DiaryListPage />} />
              <Route path="diary/new" element={<DiaryEditPage />} />
              <Route path="diary/:id" element={<DiaryDetailPage />} />
              <Route path="diary/:id/edit" element={<DiaryEditPage />} />
              <Route path="work-logs/new" element={<WorkLogEditPage />} />
              <Route path="work-logs/:id/edit" element={<WorkLogEditPage />} />
              <Route path="assets" element={<AssetPage />} />
              <Route path="ledger" element={<LedgerPage />} />
              <Route path="study" element={<StudyPage />} />
              <Route path="study/new" element={<StudyEditPage />} />
              <Route path="study/:id/edit" element={<StudyEditPage />} />
              {/* 예전 가계부 주소 */}
              <Route path="expenses" element={<Navigate to="/ledger" replace />} />
            </Route>

            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
