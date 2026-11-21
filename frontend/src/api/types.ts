export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface DiarySummary {
  id: number
  /** yyyy-MM-dd */
  entryDate: string
  title: string | null
  /** 본문 앞부분 (마크다운 기호 제거, 최대 120자) */
  preview: string
}

export interface Diary {
  id: number
  entryDate: string
  title: string | null
  content: string
  createdAt: string
  updatedAt: string
}

export interface DiaryRequest {
  entryDate: string
  title: string
  content: string
}

export interface WorkLog {
  id: number
  title: string
  organization: string | null
  role: string | null
  startDate: string
  endDate: string | null
  techStack: string | null
  content: string | null
}

export type WorkLogRequest = Omit<WorkLog, 'id'>

/** 학습 노트 분류 (화면 표시 순서) */
export const STUDY_CATEGORIES = {
  INTRO: '자기소개·지원동기',
  INTERVIEW: '면접 질문',
  DOMAIN: '업무·도메인',
  TROUBLE: '문제 해결 사례',
  TECH: '기술 개념',
} as const
export type StudyCategory = keyof typeof STUDY_CATEGORIES

export interface StudyCard {
  id: number
  category: StudyCategory
  question: string
  answer: string
  sortOrder: number
  memorized: boolean
}

export type StudyCardRequest = Omit<StudyCard, 'id' | 'memorized'>

export const ASSET_TYPES = {
  STOCK: '주식',
  ETF: 'ETF',
  FUND: '펀드',
  BOND: '채권',
  CRYPTO: '가상자산',
  DEPOSIT: '예적금',
  CASH: '현금',
  REAL_ESTATE: '부동산',
  PENSION: '연금',
  ETC: '기타',
} as const
export type AssetType = keyof typeof ASSET_TYPES

export const CURRENCIES = ['KRW', 'USD', 'JPY', 'EUR'] as const
export type Currency = (typeof CURRENCIES)[number]

/** MANUAL 외에는 외부 API 동기화 자산 (수정·삭제 불가) */
export const ASSET_SOURCES = {
  MANUAL: '직접 입력',
  UPBIT: '업비트',
  TOSS: '토스증권',
} as const
export type AssetSource = keyof typeof ASSET_SOURCES

export interface SyncStatus {
  source: Exclude<AssetSource, 'MANUAL'>
  /** API 키 설정 여부 */
  configured: boolean
  /** 마지막 시도 시각. 서버 재시작 후 첫 동기화 전이면 null */
  lastSyncedAt: string | null
  success: boolean
  assetCount: number | null
  error: string | null
}

export interface AssetRequest {
  assetType: AssetType
  name: string
  ticker: string
  account: string
  quantity: string
  avgPrice: string
  currentPrice: string
  currency: Currency
  memo: string
}

export interface Asset {
  id: number
  assetType: AssetType
  name: string
  ticker: string | null
  account: string | null
  quantity: number
  avgPrice: number
  currentPrice: number
  currency: Currency
  memo: string | null
  source: AssetSource
  costAmount: number
  evalAmount: number
  profitAmount: number
  profitRate: number
  updatedAt: string
}

export interface AssetSummary {
  currencies: {
    currency: Currency
    costAmount: number
    evalAmount: number
    profitAmount: number
    profitRate: number
    byType: TypeWeight[]
  }[]
  /** 전체 자산 원화·달러 환산 합계. 환율을 못 가져왔으면 null */
  total: {
    costKrw: number
    evalKrw: number
    profitKrw: number
    costUsd: number
    evalUsd: number
    profitUsd: number
    profitRate: number
    /** 유형별 원화 환산 평가금액과 전체 중 비중(%) */
    byType: TypeWeight[]
  } | null
  exchangeRate: ExchangeRate | null
}

export interface ExchangeRate {
  date: string
  source: string
  /** 1 단위당 원화 */
  krwPerUnit: Partial<Record<Currency, number>>
}

export interface ErrorBody {
  code: string
  message: string
  errors: { field: string; reason: string }[]
}

export interface TypeWeight {
  assetType: AssetType
  evalAmount: number
  /** 비중(%) */
  weight: number
}

/** 가계부 항목 구분: 월 수입, 월 고정지출, 부채 잔액 */
export type LedgerKind = 'INCOME' | 'FIXED_EXPENSE' | 'DEBT'

export interface LedgerItem {
  id: number
  kind: LedgerKind
  /** yyyy-MM. 부채는 null */
  month: string | null
  name: string
  /** 원 단위. 수입·고정지출은 월 금액, 부채는 잔액 */
  amount: number
  memo: string | null
}

export interface LedgerItemRequest {
  kind: LedgerKind
  /** yyyy-MM. 수입·고정지출만 (등록 시) */
  month: string | null
  name: string
  amount: string
  memo: string
}

export interface LedgerMonthTotal {
  month: string
  income: number
  fixedExpense: number
}

/** 한 달의 가계부. 부채는 달과 무관한 현재 잔액 */
export interface Ledger {
  month: string
  incomeTotal: number
  fixedExpenseTotal: number
  /** 월 수입 - 월 고정지출 (음수면 적자) */
  monthlyBalance: number
  debtTotal: number
  incomes: LedgerItem[]
  fixedExpenses: LedgerItem[]
  debts: LedgerItem[]
  /** 그 달까지 최근 12개월 (오래된 달부터) */
  trend: LedgerMonthTotal[]
}
