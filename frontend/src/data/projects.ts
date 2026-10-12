/**
 * 개인 프로젝트 카드 목록 (프로젝트 화면 상단). 순서대로 표시된다.
 * href: 카드를 누르면 열리는 주소(GitHub 저장소), site: 별도 '사이트' 링크 (선택)
 */
export interface Project {
  name: string
  summary: string
  stack: string[]
  href: string
  site?: string
}

export const PROJECTS: Project[] = [
  {
    name: '복순',
    summary: '둘이 함께 쓰는 운동일지 · 일정 · 할 일 · 독서 기록 앱. 반복 일정과 푸시 알림, 책 표지 검색·글귀 인식을 지원하는 PWA',
    stack: ['Spring Boot', 'React', 'PostgreSQL', 'PWA'],
    href: 'https://github.com/kim401233-ui/boksoon',
  },
  {
    name: '포트폴리오',
    summary: '경력·프로젝트·자격증을 정리한 정적 포트폴리오 사이트. 빌드 도구 없이 HTML/CSS/JS 로 만들고 GitHub Pages 로 배포',
    stack: ['HTML', 'CSS', 'JavaScript', 'GitHub Pages'],
    href: 'https://github.com/kdh4012/kdh4012.github.io',
    site: 'https://kdh4012.github.io',
  },
]
