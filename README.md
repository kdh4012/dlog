# dlog

동현의 개인 홈페이지입니다. 블로그, 작업 기록, 자산 포트폴리오를 관리합니다.

| 메뉴 | 공개 범위 |
|---|---|
| 블로그 (마크다운) | 공개 글은 누구나, 비공개 글은 관리자만 |
| 작업 기록 (프로젝트 이력 타임라인) | 누구나 조회, 수정은 관리자만 |
| 자산 포트폴리오 (통화별 평가금액·수익률·유형별 비중) | **관리자만** |

## 기술 스택

- **Backend**: Java 21, Spring Boot 4, Spring Data JPA, Spring Security + JWT, Flyway
- **Frontend**: React 19, TypeScript, Vite, React Router
- **DB**: PostgreSQL 16

## 구조

```
dlog/
├── backend/            Spring Boot (com.dlog)
│   ├── auth/           로그인, JWT 발급
│   ├── post/           블로그
│   ├── worklog/        작업 기록
│   ├── asset/          자산 포트폴리오
│   ├── global/         보안, 예외 처리, 공통 엔티티, SPA 라우팅
│   └── resources/db/migration   Flyway 스키마
├── frontend/           React SPA
├── Dockerfile          프론트 + 백엔드를 단일 jar 이미지로 빌드
└── docker-compose.yml  PostgreSQL (+ 운영용 app)
```

## 로컬 개발

```bash
# 1. DB
docker compose up -d db

# 2. Backend (http://localhost:8080, local 프로파일 자동 적용)
cd backend
./gradlew bootRun          # Windows: gradlew.bat bootRun

# 3. Frontend (http://localhost:5173, /api 는 8080 으로 프록시)
cd frontend
npm install
npm run dev
```

local 프로파일 기본 관리자 계정: `admin` / `dlog1234`

## 관리자 계정

1인 블로그라서 관리자 계정은 DB 대신 환경 변수로 관리합니다.

```bash
cd backend
./gradlew -q hashPassword -Ppw='내비밀번호'   # 출력된 BCrypt 해시를 ADMIN_PASSWORD_HASH 로 사용
```

## 운영 배포 (Docker)

```bash
cp .env.example .env    # 값 채우기
docker compose --profile app up -d --build
```

| 환경 변수 | 설명 |
|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL 접속 정보 |
| `JWT_SECRET` | 32자 이상 랜덤 문자열 |
| `JWT_EXPIRATION_MINUTES` | 토큰 만료 시간 (기본 720분) |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD_HASH` | 관리자 계정 |

> `.env` 에 넣는 BCrypt 해시의 `$` 는 `$$` 로 이스케이프해야 합니다.

## 테스트

```bash
cd backend && ./gradlew test         # H2 (PostgreSQL 모드) 기반 API 테스트
cd frontend && npm run lint && npm run build
```

## API

| Method | Path | 권한 |
|---|---|---|
| POST | `/api/auth/login` | 공개 |
| GET | `/api/auth/me` | 관리자 |
| GET | `/api/posts?page=&category=`, `/api/posts/{id}` | 공개 |
| POST/PUT/DELETE | `/api/posts[/{id}]` | 관리자 |
| GET | `/api/work-logs`, `/api/work-logs/{id}` | 공개 |
| POST/PUT/DELETE | `/api/work-logs[/{id}]` | 관리자 |
| GET | `/api/assets`, `/api/assets/summary`, `/api/assets/{id}` | 관리자 |
| POST/PUT/DELETE | `/api/assets[/{id}]` | 관리자 |
