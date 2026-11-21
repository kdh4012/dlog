import { useAuth } from '../auth/AuthContext'
import GoalBoard from '../components/GoalBoard'

export default function HomePage() {
  const { isAdmin } = useAuth()

  return (
    <>
      <section className="hero">
        <h1>
          만들고, 고치고,
          <br />
          기록합니다.
        </h1>
        <p className="muted">현장 자동화부터 웹 서비스까지, 개발하며 배운 것들을 남기는 공간입니다.</p>
      </section>

      {/* 목표는 나만 보는 공간이라 로그인했을 때만 */}
      {isAdmin && <GoalBoard />}
    </>
  )
}
