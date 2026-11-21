import { useAuth } from '../auth/AuthContext'
import GoalBoard from '../components/GoalBoard'
import ResolutionCard from '../components/ResolutionCard'

/** 홈: 나의 다짐 + 연도별 목표 (둘 다 나만 보기라 로그인했을 때만 보인다) */
export default function HomePage() {
  const { isAdmin } = useAuth()
  if (!isAdmin) return null

  return (
    <>
      <ResolutionCard />
      <GoalBoard />
    </>
  )
}
