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
    site: 'https://158-247-246-133.sslip.io',
  },
  {
    name: '포트폴리오',
    summary: '경력·프로젝트·문제 해결 사례를 정리한 정적 포트폴리오 사이트. 빌드 도구 없이 데이터(data.js)와 렌더링(main.js)을 분리했고, 자격증 PDF는 AES-256-GCM 으로 암호화해 비밀번호를 입력해야 브라우저에서 복호화되도록 구현',
    stack: ['HTML', 'CSS', 'JavaScript', 'Web Crypto API', 'Node.js', 'GitHub Pages'],
    href: 'https://github.com/kdh4012/kdh4012.github.io',
    site: 'https://kdh4012.github.io',
  },
]
