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
    byType: { assetType: AssetType; evalAmount: number; weight: number }[]
  }[]
}

export interface ErrorBody {
  code: string
  message: string
  errors: { field: string; reason: string }[]
}
