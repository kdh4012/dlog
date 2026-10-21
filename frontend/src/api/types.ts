export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface PostSummary {
  id: number
  title: string
  summary: string | null
  category: string | null
  published: boolean
  createdAt: string
}

export interface Post extends PostSummary {
  content: string
  updatedAt: string
}

export interface PostRequest {
  title: string
  summary: string
  content: string
  category: string
  published: boolean
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

export const EXPENSE_CATEGORIES = {
  FIXED_COST: '고정비', // 고정지출 전용 (변동지출 입력에서는 고를 수 없음)
  FOOD: '식비',
  CAFE: '카페·간식',
  LIVING: '생활·마트',
  TRANSPORT: '교통·차량',
  HOUSING: '주거·관리비',
  COMMUNICATION: '통신',
  SUBSCRIPTION: '구독',
  MEDICAL: '의료·건강',
  SHOPPING: '쇼핑',
  CULTURE: '문화·여가',
  TRAVEL: '여행',
  EDUCATION: '교육',
  GIFT: '경조사·선물',
  INSURANCE: '보험',
  ETC: '기타',
} as const
export type ExpenseCategory = keyof typeof EXPENSE_CATEGORIES

/** 변동지출 입력에서 고를 수 있는 카테고리 ('고정비' 제외) */
export const VARIABLE_CATEGORIES = Object.entries(EXPENSE_CATEGORIES).filter(([k]) => k !== 'FIXED_COST') as [
  ExpenseCategory,
  string,
][]

export const PAYMENT_METHODS = {
  CARD: '카드',
  CASH: '현금',
  TRANSFER: '계좌이체',
  ETC: '기타',
} as const
export type PaymentMethod = keyof typeof PAYMENT_METHODS

export type ExpenseKind = 'FIXED' | 'VARIABLE'

export interface Expense {
  id: number
  spentOn: string
  amount: number
  category: ExpenseCategory
  /** 고정지출에서 반영된 내역은 비어 있을 수 있다 */
  paymentMethod: PaymentMethod | null
  merchant: string | null
  memo: string | null
  /** FIXED: 고정지출에서 매월 반영된 내역, VARIABLE: 직접 입력 */
  kind: ExpenseKind
  fixedExpenseId: number | null
}

export interface ExpenseRequest {
  spentOn: string
  amount: string
  category: ExpenseCategory
  /** '' 이면 선택 안 함 (null 로 전송) */
  paymentMethod: PaymentMethod | ''
  merchant: string
  memo: string
}

export interface ExpenseSummary {
  month: string
  total: number
  fixedTotal: number
  variableTotal: number
  count: number
  previousTotal: number
  changeRate: number | null
  budget: number | null
  budgetMonth: string | null
  budgetUsedRate: number | null
  byCategory: { category: ExpenseCategory; amount: number; count: number; weight: number }[]
  trend: { month: string; total: number }[]
}

export interface FixedExpense {
  id: number
  name: string
  amount: number
  startMonth: string
  endMonth: string | null
  memo: string | null
  /** 이번 달 이후에도 반영되는지 */
  active: boolean
}

export interface FixedExpenseRequest {
  name: string
  amount: string
  startMonth: string
  endMonth: string
  memo: string
}

/** 관심 종목(미국 주식). 시세를 못 구하면 price 이하가 null */
export interface WatchlistItem {
  id: number
  ticker: string
  name: string | null
  targetPrice: number | null
  memo: string | null
  price: number | null
  change: number | null
  /** 전일 대비 등락률(%) */
  changeRate: number | null
  currency: string | null
  quotedAt: string | null
  /** 목표가까지 남은 비율(%). 음수면 목표가 도달 */
  targetGapRate: number | null
}

export interface WatchlistRequest {
  ticker: string
  name: string
  targetPrice: string
  memo: string
}
