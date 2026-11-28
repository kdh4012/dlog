export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/** 홈 화면 다짐. 아직 없으면 content 는 '' */
export interface Resolution {
  content: string
  updatedAt: string | null
}

/** 한 해의 목표 */
export interface Goal {
  id: number
  year: number
  title: string
  /** 달성했는지 */
  done: boolean
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

/** 학습 노트 분류 (화면 표시 순서) */
export const STUDY_CATEGORIES = {
  INTERVIEW: '면접 질문',
  KNOWLEDGE: '기술·업무',
} as const
export type StudyCategory = keyof typeof STUDY_CATEGORIES

/** 기술·업무 카드의 유형 (여러 개 선택, 화면 표시 순서) */
export const STUDY_TYPES = {
  WORK: '업무',
  DOMAIN: '도메인',
  TECH: '기술 개념',
  TROUBLE: '문제 해결',
} as const
export type StudyType = keyof typeof STUDY_TYPES
export const STUDY_TYPE_KEYS = Object.keys(STUDY_TYPES) as StudyType[]

export interface StudyCard {
  id: number
  category: StudyCategory
  /** 기술·업무 카드만. 면접 질문은 빈 배열 */
  types: StudyType[]
  question: string
  answer: string
  sortOrder: number
}

export type StudyCardRequest = Omit<StudyCard, 'id'>

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
  KIS: '한국투자',
  GOLD: '금 시세',
} as const
export type AssetSource = keyof typeof ASSET_SOURCES

/** 하루의 총 자산 (원화, 모든 출처 합계) */
export interface AssetHistoryPoint {
  /** yyyy-MM-dd */
  date: string
  evalKrw: number
  costKrw: number
}

/** 금 현물 보유량 (직접 입력). 시세는 서버가 KRX 금시세로 갱신 */
export interface GoldHolding {
  /** 보유량 (g). 입력 전이면 0 */
  grams: number
  /** 평균 매입 단가 (원/g) */
  avgPrice: number
  /** 평가에 쓰는 시세 (원/g). 시세를 못 받았으면 매입 단가, 입력 전이면 null */
  currentPrice: number | null
  /** 시세 기준일 (yyyy-MM-dd). 받은 적 없으면 null */
  priceDate: string | null
  /** 시세 API 키가 서버에 설정되어 있는지 */
  priceApiReady: boolean
}

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
