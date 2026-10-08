export interface User {
  id: number
  username: string
  realName: string
  studentId: string
  phone: string
  email: string
  role: 'USER' | 'POINT_ADMIN' | 'SYS_ADMIN'
  creditScore: number
  status: number
  allowLeaderboard: number
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  realName: string
  studentId: string
  phone: string
  email?: string
}

export interface LoginResponse {
  token: string
  user: User
}

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface FoundItem {
  id: number
  title: string
  category: string
  description: string
  foundLocation: string
  foundTime: string
  images: string[] | null
  claimQuestion?: string
  perishable: number
  itemStatus: number
  founderId: number
  founderName?: string
  dropPointId: number | null
  dropPointName?: string
  publishedAt: string
  claimedAt: string | null
  newUser?: boolean
}

export interface FoundItemRequest {
  title: string
  category: string
  description?: string
  foundLocation: string
  dropPointId?: number
  foundTime: string
  images?: string[]
  claimQuestion: string
  perishable?: number
  actualFounderId?: number
}

export interface ItemQueryRequest {
  keyword?: string
  category?: string
  dropPointId?: number
  itemStatus?: number
  page?: number
  size?: number
}

export interface DropPoint {
  id: number
  name: string
  location: string
  adminId: number | null
  adminName?: string
  hasCamera: number
  cameraInfo?: string
  status: number
}

export interface ClaimApply {
  id: number
  itemId: number
  sourceNoticeId?: number | null
  itemTitle?: string
  claimQuestion?: string
  claimerId: number
  claimerName?: string
  answer: string
  applyStatus: number
  rejectReason?: string
  rejectCount: number
  confidenceScore?: number
  lowConfidence?: number
  confidenceReason?: string
  pickupTime?: string | null
  pickupPhoto?: string | null
  pickupSignature?: string | null
  createdAt: string
}

export interface ClaimApplyRequest {
  itemId: number
  answer: string
}

export interface CreditLog {
  id: number
  userId: number
  operationType: string
  changeAmount: number
  relatedItemId: number | null
  relatedApplyId: number | null
  reason: string
  createdAt: string
}

export interface LostNotice {
  id: number
  title: string
  category: string
  description: string
  lostLocation: string
  lostTime: string
  contactInfo: string
  images: string[] | null
  status: number
  publisherId: number
  publisherName?: string
  createdAt: string
}

export interface LostNoticeRequest {
  title: string
  category: string
  description?: string
  lostLocation?: string
  lostTime?: string
  contactInfo: string
  images?: string[]
}

export interface LeaderboardEntry {
  userId: number
  realName: string
  creditScore: number
}

export interface LeaderboardVO {
  entries: LeaderboardEntry[]
  myRank: number | null
  myScore: number | null
}

export interface CertificateRecord {
  changeAmount: number
  operationType: string
  relatedItemId: number | null
  reason: string
  createdAt: string
}

export interface CertificateVO {
  realName: string
  studentId: string
  totalScore: number
  statsByType: Record<string, number>
  records: CertificateRecord[]
  issuedAt: string
}

export interface DisputeVO {
  id: number
  applicantId: number
  applicantName?: string
  applyId: number | null
  itemId: number | null
  itemTitle?: string
  disputeType: string
  description: string
  evidenceImages: string[] | null
  status: number
  handlerNote?: string
  handlerId?: number
  handledAt?: string
  createdAt: string
}

export interface DisputeCreateRequest {
  disputeType: string
  applyId?: number
  itemId?: number
  description: string
  evidenceImages?: string[]
}

export interface DisputeHandleRequest {
  approved: boolean
  handlerNote: string
}

export interface RiskWarningVO {
  warningType: string
  userIds: number[]
  userNames: string[]
  detail: string
  count: number
  itemIds?: number[]
  similarity?: number
}

export interface DashboardVO {
  totalItems: number
  publicItems: number
  pickedUpItems: number
  totalUsers: number
  totalClaims: number
  successClaims: number
  itemsByCategory: Record<string, number>
  claimsByDay: Record<string, number>
}

export const DISPUTE_TYPE_MAP: Record<string, { text: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  ITEM_MISMATCH: { text: '物品不符', type: 'danger' },
  OTHER: { text: '其他纠纷', type: 'warning' },
  FALSE_CLAIM: { text: '物品被冒领', type: 'primary' }
}

export const DISPUTE_STATUS_MAP: Record<number, { text: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  0: { text: '待处理', type: 'warning' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已驳回', type: 'danger' }
}

export interface ReportVO {
  id: number
  reporterId: number
  reporterName?: string
  itemId: number
  itemTitle?: string
  reportType: string
  description: string
  evidenceImages: string[] | null
  status: number
  handlerNote?: string
  handlerId?: number
  handledAt?: string
  createdAt: string
}

export interface ReportCreateRequest {
  reportType: string
  itemId: number
  description: string
  evidenceImages?: string[]
}

export interface ReportHandleRequest {
  valid: boolean
  handlerNote: string
  banPublisher?: boolean
}

export const REPORT_TYPE_MAP: Record<string, { text: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  FAKE_PUBLISH: { text: '虚假投放', type: 'danger' },
  DESC_MISMATCH: { text: '描述不符', type: 'warning' },
  OTHER: { text: '其他', type: 'info' }
}

export const REPORT_STATUS_MAP: Record<number, { text: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  0: { text: '待处理', type: 'warning' },
  1: { text: '举报成立', type: 'danger' },
  2: { text: '不成立', type: 'info' }
}

export const ITEM_STATUS_MAP: Record<number, { text: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  0: { text: '已作废', type: 'info' },
  1: { text: '公开待认领', type: 'success' },
  2: { text: '认领中', type: 'warning' },
  3: { text: '已取件', type: 'primary' },
  4: { text: '已归档', type: 'info' },
  5: { text: '已过期', type: 'info' }
}

export const CLAIM_STATUS_MAP: Record<number, { text: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '已完成', type: 'success' },
  2: { text: '已拒绝', type: 'danger' },
  3: { text: '已锁定', type: 'info' },
  4: { text: '待取件', type: 'primary' }
}

export const HAND_IN_STATUS_MAP: Record<number, { text: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  1: { text: '已交物待核对', type: 'warning' },
  2: { text: '已核对', type: 'success' }
}

export const NOTICE_STATUS_MAP: Record<number, { text: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  0: { text: '进行中', type: 'success' },
  1: { text: '已找到', type: 'primary' },
  2: { text: '已关闭', type: 'info' }
}

export const ITEM_CATEGORIES = ['电子产品', '证件卡类', '钥匙', '书籍资料', '衣物', '饰品', '生活用品', '其他']
