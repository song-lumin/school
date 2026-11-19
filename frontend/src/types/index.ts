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
  /** 误领次数（后端由 MISCLAIM_COUNT 计次流水统计，积分明细里不展示 0 分记录） */
  misclaimCount?: number
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
  /** 拾取时间的模糊描述，非空时优先展示 */
  foundTimeText?: string | null
  /** 0 精确 / 1 仅日期 / 2 上午 / 3 下午 / 4 傍晚 / 5 晚上 */
  foundTimePrecision?: number
  images: string[] | null
  claimQuestion?: string
  perishable: number
  itemStatus: number
  founderId: number
  founderName?: string
  dropPointId: number | null
  dropPointName?: string
  dropPointAdminName?: string
  dropPointAdminPhone?: string
  publishedAt: string
  claimedAt: string | null
  newUser?: boolean
  forwardedNoticeId?: number | null
  takedownReason?: string | null
  appealStatus?: number | null
}

export interface FoundItemRequest {
  title: string
  category: string
  description?: string
  foundLocation: string
  dropPointId?: number
  foundTime: string
  /** 拾取时间模糊描述（选填，≤60 字） */
  foundTimeText?: string
  /** 0 精确 / 1 仅日期 / 2 上午 / 3 下午 / 4 傍晚 / 5 晚上 */
  foundTimePrecision?: number
  images?: string[]
  claimQuestion: string
  referenceAnswer?: string
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
  adminPhone?: string
  adminEmail?: string
  officeHours?: string
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
  dropPointId?: number
  dropPointName?: string
  dropPointLocation?: string
  dropPointAdminName?: string
  dropPointAdminPhone?: string
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
  matchedItemId?: number | null
  takedownReason?: string | null
  appealStatus?: number | null
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
  handleResult?: string | null
  handlerId?: number
  handledAt?: string
  createdAt: string
  /** 站点现场核验陈述（只写事实，不写结论） */
  pointCheckNote?: string | null
  pointCheckerId?: number | null
  pointCheckerName?: string | null
  pointCheckedAt?: string | null
  /** 关联投放点 */
  dropPointId?: number | null
  dropPointName?: string | null
  dropPointAdminName?: string | null
  dropPointAdminPhone?: string | null
  /** 待处理工单已等待小时数（站点不在岗的兜底依据） */
  pendingHours?: number | null
  /** 现场核验是否由系统管理员代填 */
  pointCheckStandIn?: boolean | null
  /** 最近一次催办时间（站点超 24h 未回填） */
  pointEscalatedAt?: string | null
  /** 已催办次数 */
  pointRemindCount?: number | null
  /** 定责时站点仍未回填，1 = 站点未响应 */
  siteUnresponsive?: number | null
  /** 站点未响应标记时间 */
  siteUnresponsiveAt?: string | null
  /** 已超过 24h SLA 且仍未回填 */
  pointSlaOverdue?: boolean | null
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
  /** 旧版按类型预设结论，前端已不再传 */
  outcome?: string
  /** 手动回滚项：PUBLISH_CHECK / PUBLISH_PICKUP / FORWARD / CLAIMER；传数组即走手动定责 */
  rollbackItems?: string[]
  /** 物品处置：NONE / VOIDED / LOST / RETURNING / PUBLIC */
  itemDispose?: string
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
  ITEM_DAMAGED: { text: '物品损坏', type: 'warning' },
  ITEM_NOT_FOUND: { text: '找不到物品', type: 'info' },
  OTHER: { text: '其他纠纷', type: 'warning' },
  FALSE_CLAIM: { text: '物品被冒领', type: 'primary' }
}

export const DISPUTE_STATUS_MAP: Record<number, { text: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  0: { text: '未处理', type: 'warning' },
  1: { text: '已处理 · 通过', type: 'success' },
  2: { text: '已处理 · 驳回', type: 'danger' },
  3: { text: '处理中', type: 'primary' }
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
  targetType?: string
  itemId?: number
  noticeId?: number
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
  5: { text: '已过期', type: 'info' },
  6: { text: '待投放未公开', type: 'warning' },
  7: { text: '待归还', type: 'danger' },
  8: { text: '丢失待查', type: 'danger' }
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

export interface SysMessage {
  id: number
  userId: number
  title: string
  content: string
  bizType: string | null
  relatedItemId: number | null
  relatedApplyId: number | null
  isRead: number
  createdAt: string
}

export const MESSAGE_BIZ_MAP: Record<string, { text: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  CREDIT: { text: '积分变动', type: 'warning' },
  DISPUTE: { text: '申诉处理', type: 'danger' },
  RETURN: { text: '归还入库', type: 'success' },
  CLAIM: { text: '认领进展', type: 'primary' },
  ITEM_NOT_FOUND: { text: '找不到物品', type: 'info' },
  CHAT: { text: '沟通留言', type: 'warning' }
}
