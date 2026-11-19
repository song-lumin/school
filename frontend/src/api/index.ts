// api/index.ts —— 后端接口统一封装层（API 模块）
// 功能：把所有对后端 REST 接口的调用集中封装成一个个“Api 对象”，按业务域分组导出。
// 在系统中的作用：页面组件不直接写 axios 请求，而是调用这里的函数，从而：
//   1) URL、请求方式、参数结构集中管理，后端接口变动时只改这一处；
//   2) 配合 types/index.ts 的类型定义，调用方有完整的 TypeScript 类型提示。
// 每个函数返回 Promise，成功时 resolve 的是后端统一响应体 ApiResponse<T>（见 request.ts 拦截器）。
import request from '@/utils/request'
import type {
  LoginRequest,
  RegisterRequest,
  LoginResponse,
  User,
  ApiResponse,
  PageResult,
  FoundItem,
  FoundItemRequest,
  ItemQueryRequest,
  DropPoint,
  ClaimApply,
  ClaimApplyRequest,
  CreditLog,
  LostNotice,
  LostNoticeRequest,
  LeaderboardVO,
  DisputeVO,
  DisputeCreateRequest,
  DisputeHandleRequest,
  RiskWarningVO,
  DashboardVO,
  ReportVO,
  ReportCreateRequest,
  ReportHandleRequest,
  CertificateVO
} from '@/types'

// ==================== 认证模块：注册 / 登录 / 登出 / 个人资料 ====================
export const authApi = {
  // 注册新账号，成功后后端直接返回登录态（token + 用户信息）
  register(data: RegisterRequest) {
    return request.post<any, ApiResponse<LoginResponse>>('/auth/register', data)
  },

  // 账号密码登录，返回 token 与用户信息
  login(data: LoginRequest) {
    return request.post<any, ApiResponse<LoginResponse>>('/auth/login', data)
  },

  // 退出登录，后端使当前 token 失效
  logout() {
    return request.post<any, ApiResponse<void>>('/auth/logout')
  },

  // 获取当前登录用户的最新信息（用于刷新 store 中的用户数据）
  getCurrentUser() {
    return request.get<any, ApiResponse<User>>('/auth/me')
  },

  // 修改个人资料（真实姓名 / 手机号 / 邮箱）
  updateProfile(data: { realName?: string; phone?: string; email?: string }) {
    return request.put<any, ApiResponse<User>>('/auth/profile', data)
  },

  // 修改密码，需提供旧密码校验身份
  updatePassword(data: { oldPassword: string; newPassword: string }) {
    return request.put<any, ApiResponse<void>>('/auth/password', data)
  }
}

// ==================== 文件上传模块：图片上传 ====================
export const uploadApi = {
  // 上传图片文件：必须用 FormData（multipart/form-data）承载二进制文件，
  // 后端返回图片的访问 URL 字符串，页面再把它存入表单字段
  uploadImage(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<any, ApiResponse<string>>('/upload/image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}

// ==================== 失物招领物品模块（捡到的物品） ====================
export const foundItemApi = {
  // 发布一条“捡到物品”信息
  publish(data: FoundItemRequest) {
    return request.post<any, ApiResponse<FoundItem>>('/found-items', data)
  },

  // 分页查询物品列表（支持关键词、分类、状态等筛选条件）
  list(params?: ItemQueryRequest) {
    return request.get<any, ApiResponse<PageResult<FoundItem>>>('/found-items', { params })
  },

  // 分页查询“我发布的”物品
  listMy(params?: ItemQueryRequest) {
    return request.get<any, ApiResponse<PageResult<FoundItem>>>('/found-items/my', { params })
  },

  // 查询单个物品详情
  getById(id: number | string) {
    return request.get<any, ApiResponse<FoundItem>>(`/found-items/${id}`)
  },

  // 将物品作废（例如已损坏、虚假信息），不再展示
  invalidate(id: number | string) {
    return request.put<any, ApiResponse<void>>(`/found-items/${id}/invalidate`)
  },

  // 标记物品已放入投放点（招领点），等待认领
  markPlaced(id: number | string) {
    return request.put<any, ApiResponse<void>>(`/found-items/${id}/mark-placed`)
  },

  // 将物品关联（转发）到某条寻物启事，主动匹配失主
  forwardToNotice(id: number | string, noticeId: number) {
    return request.put<any, ApiResponse<void>>(`/found-items/${id}/forward`, { noticeId })
  },

  // 归档物品（长期无人认领，移入历史归档）
  archive(id: number | string) {
    return request.put<any, ApiResponse<void>>(`/found-items/${id}/archive`)
  },

  // 删除物品
  remove(id: number | string) {
    return request.delete<any, ApiResponse<void>>(`/found-items/${id}`)
  }
}

// ==================== 投放点（招领点）模块 ====================
export const dropPointApi = {
  // 查询所有投放点列表
  list() {
    return request.get<any, ApiResponse<DropPoint[]>>('/drop-points')
  },

  // 查询单个投放点详情
  getById(id: number | string) {
    return request.get<any, ApiResponse<DropPoint>>(`/drop-points/${id}`)
  },

  // 查询某投放点下待核验（未确认入库）的物品
  getPendingItems(id: number | string) {
    return request.get<any, ApiResponse<FoundItem[]>>(`/drop-points/${id}/items`)
  },

  // 投放点管理员核验物品入库，可附核验备注
  checkItem(id: number | string, data: { itemId: number; checkNote?: string }) {
    return request.put<any, ApiResponse<void>>(`/drop-points/${id}/check`, data)
  },

  // 查询某投放点待领取的认领申请（用户已通过审核、等待到点取件）
  getPendingPickups(id: number | string) {
    return request.get<any, ApiResponse<ClaimApply[]>>(`/drop-points/${id}/pending-pickups`)
  },

  // 查询某投放点的库存清单（当前存放在该点的所有物品）
  getInventory(id: number | string) {
    return request.get<any, ApiResponse<any[]>>(`/drop-points/${id}/inventory`)
  }
}

// ==================== 认领申请模块 ====================
export const claimApi = {
  // 提交认领申请（需要填写认领理由、回答失主设置的问题等）
  apply(data: ClaimApplyRequest) {
    return request.post<any, ApiResponse<ClaimApply>>('/claims', data)
  },

  // 分页查询某物品下的所有认领申请（供发布者审核）
  listByItem(itemId: number | string, params?: { page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<ClaimApply>>>('/claims', {
      params: { itemId, ...params }
    })
  },

  // 查询单条认领申请详情
  getById(id: number | string) {
    return request.get<any, ApiResponse<ClaimApply>>(`/claims/${id}`)
  },

  // 通过认领申请
  approve(id: number | string) {
    return request.put<any, ApiResponse<void>>(`/claims/${id}/approve`)
  },

  // 驳回认领申请，需填写驳回理由
  reject(id: number | string, data: { rejectReason?: string }) {
    return request.put<any, ApiResponse<void>>(`/claims/${id}/reject`, data)
  },

  // 确认取件：可附带取件照片与手写签名（凭证），完成整个认领流程
  pickup(id: number | string, data?: { pickupPhoto?: string; pickupSignature?: string }) {
    return request.put<any, ApiResponse<void>>(`/claims/${id}/pickup`, data || {})
  },

  // 撤销取件确认（误操作时回退状态）
  cancelPickup(id: number | string) {
    return request.put<any, ApiResponse<void>>(`/claims/${id}/cancel-pickup`)
  },

  // 分页查询“我提交的”认领申请
  listMy(params?: { page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<ClaimApply>>>('/claims/my', { params })
  },

  // 分页查询待我审核的认领申请（按状态筛选）
  listForReview(params?: { status?: number; page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<ClaimApply>>>('/claims/review', { params })
  }
}

// ==================== 信用分模块 ====================
export const creditApi = {
  // 查询我当前的信用分
  getMyCredit() {
    return request.get<any, ApiResponse<number>>('/credits/me')
  },

  // 分页查询我的信用分变动流水（加分 / 扣分记录）
  getMyLogs(params?: { page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<CreditLog>>>('/credits/logs', { params })
  },

  // 查询信用分排行榜（默认取前 100 名）
  getLeaderboard(top = 100) {
    return request.get<any, ApiResponse<LeaderboardVO>>('/credits/leaderboard', { params: { top } })
  },

  // 获取我的公益证书数据（用于生成证书页面）
  getCertificate() {
    return request.get<any, ApiResponse<CertificateVO>>('/credits/certificate')
  },

  // 设置是否同意公开上榜（隐私开关：1 允许 / 0 拒绝）
  setLeaderboardSetting(allow: number) {
    return request.put<any, ApiResponse<void>>('/credits/leaderboard-setting', {
      allowLeaderboard: allow
    })
  }
}

// ==================== 寻物启事模块（失主发布的找物信息） ====================
export const lostNoticeApi = {
  // 发布寻物启事
  publish(data: LostNoticeRequest) {
    return request.post<any, ApiResponse<LostNotice>>('/lost-notices', data)
  },

  // 分页查询寻物启事列表（关键词 / 分类 / 状态筛选）
  list(params?: { keyword?: string; category?: string; status?: number; page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<LostNotice>>>('/lost-notices', { params })
  },

  // 查询单条寻物启事详情
  getById(id: number | string) {
    return request.get<any, ApiResponse<LostNotice>>(`/lost-notices/${id}`)
  },

  // 以图搜物：上传一张图片，后端进行相似度匹配，返回物品列表及相似度分数
  // （用于失主随手拍一张类似物品 / 网图来检索可能的捡到记录）
  searchByImage(file: File, category?: string) {
    const formData = new FormData()
    formData.append('image', file)
    if (category) formData.append('category', category)   // 可选：限定物品分类缩小搜索范围
    return request.post<any, ApiResponse<Array<{ item: FoundItem; similarity: number }>>>(
      '/lost-notices/search-by-image', formData, { headers: { 'Content-Type': 'multipart/form-data' } }
    )
  },

  // 从寻物启事出发认领某条捡到物品：携带预设问题答案，直接生成认领申请
  forward(id: number | string, data: { itemId: number; answer: string }) {
    return request.post<any, ApiResponse<ClaimApply>>(`/lost-notices/${id}/forward`, data)
  },

  // 查询与该启事匹配的候选物品列表（按分类 / 关键词自动匹配的结果）
  matches(id: number | string) {
    return request.get<any, ApiResponse<Array<{ itemId: number; title: string; category: string; images: string[] | null; foundLocation: string; itemStatus: number; claimQuestion: string }>>>(`/lost-notices/${id}/matches`)
  },

  // 查询我已通过该启事转发认领的物品
  forwardedItems(id: number | string) {
    return request.get<any, ApiResponse<FoundItem[]>>(`/lost-notices/${id}/forwarded-items`)
  },

  // 关闭寻物启事（已找回或不再寻找）
  close(id: number | string) {
    return request.put<any, ApiResponse<void>>(`/lost-notices/${id}/close`)
  },

  // 删除寻物启事
  remove(id: number | string) {
    return request.delete<any, ApiResponse<void>>(`/lost-notices/${id}`)
  }
}

// ==================== 争议（申诉纠纷）模块 ====================
export const disputeApi = {
  // 用户发起争议（例如对驳回结果不服、对取件过程有异议）
  create(data: DisputeCreateRequest) {
    return request.post<any, ApiResponse<DisputeVO>>('/disputes', data)
  },

  // 分页查询我发起的争议
  listMy(params?: { page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<DisputeVO>>>('/disputes/my', { params })
  },

  // 查询单条争议详情
  getById(id: number | string) {
    return request.get<any, ApiResponse<DisputeVO>>(`/disputes/${id}`)
  },

  // 管理员分页查询所有争议（按类型 / 状态筛选）
  list(params?: { disputeType?: string; status?: number; page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<DisputeVO>>>('/disputes', { params })
  },

  // 管理员处理争议（给出处理结果与说明）
  handle(id: number | string, data: DisputeHandleRequest) {
    return request.put<any, ApiResponse<void>>(`/disputes/${id}/handle`, data)
  }
}

// ==================== 风控模块（管理员专用） ====================
export const riskControlApi = {
  // 获取所有风控预警列表
  getWarnings() {
    return request.get<any, ApiResponse<RiskWarningVO[]>>('/admin/risk-control/warnings')
  },

  // 查询“串通作弊”预警：检测发布者与认领者是否为熟人刷分
  getCollusion() {
    return request.get<any, ApiResponse<RiskWarningVO[]>>('/admin/risk-control/collusion')
  },

  // 查询“异常高认领率”预警：某些用户认领成功率异常，疑似刷单
  getClaimRate() {
    return request.get<any, ApiResponse<RiskWarningVO[]>>('/admin/risk-control/claim-rate')
  },

  // 查询“重复图片”预警：不同物品使用同一张图，疑似虚假发布
  getDuplicateImages() {
    return request.get<any, ApiResponse<RiskWarningVO[]>>('/admin/risk-control/duplicates')
  }
}

// ==================== 监控调阅日志模块（管理员专用） ====================
// CameraLog：查看投放点监控录像的申请与审批记录
export interface CameraLog {
  id: number
  dropPointId: number          // 投放点 ID
  dropPointName: string        // 投放点名称
  itemId: number | null        // 关联物品 ID（可为空）
  itemTitle: string | null     // 关联物品标题
  applyReason: string          // 调阅理由
  applicantName: string        // 申请人姓名
  timeRangeStart: string       // 申请调阅的时间段起点
  timeRangeEnd: string         // 申请调阅的时间段终点
  status: number               // 审批状态（待审 / 通过 / 驳回）
  resultNote: string | null    // 审批备注
  handlerName: string | null   // 审批人姓名
  handledAt: string | null     // 审批时间
  createdAt: string            // 申请提交时间
}

export const cameraLogAdminApi = {
  // 分页查询监控调阅申请（按状态 / 投放点 / 物品筛选）
  list(params: { status?: number; dropPointId?: number; itemId?: number; page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<CameraLog>>>('/admin/camera-logs', { params })
  },
  // 新建一条调阅申请（管理员代录或用户申请入口）
  create(data: { dropPointId: number; itemId?: number; applyReason: string; timeRangeStart: string; timeRangeEnd: string }) {
    return request.post<any, ApiResponse<CameraLog>>('/admin/camera-logs', data)
  },
  // 审批一条调阅申请（status 为审批结果，resultNote 为说明）
  handle(id: number, data: { status: number; resultNote: string }) {
    return request.put<any, ApiResponse<void>>(`/admin/camera-logs/${id}`, data)
  }
}

// ==================== 管理后台模块（系统管理员专用） ====================
export const adminApi = {
  // 获取管理端仪表盘统计数据（用户数、物品数、认领数等汇总指标）
  getDashboard() {
    return request.get<any, ApiResponse<DashboardVO>>('/admin/dashboard')
  },

  // 分页查询用户列表（关键词 / 角色 / 状态筛选）
  listUsers(params?: { keyword?: string; role?: string; status?: number; page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<User>>>('/admin/users', { params })
  },

  // 管理员直接调整某用户的信用分（需填写调整原因，留痕）
  adjustCredit(userId: number | string, data: { newScore: number; reason: string }) {
    return request.put<any, ApiResponse<void>>(`/admin/users/${userId}/credit`, data)
  },

  // 查询某用户的信用分变动历史
  userCreditLogs(userId: number | string) {
    return request.get<any, ApiResponse<any>>(`/admin/users/${userId}/credit-logs`)
  },

  // 获取超期未认领物品的预警列表（提醒及时归档处理）
  getExpireWarnings() {
    return request.get<any, ApiResponse<any[]>>('/admin/expire-warnings')
  }
}

// ==================== 举报模块 ====================
export const reportApi = {
  // 用户提交举报（举报违规物品 / 启事 / 用户）
  create(data: ReportCreateRequest) {
    return request.post<any, ApiResponse<ReportVO>>('/reports', data)
  },

  // 分页查询我提交的举报及其处理进度
  listMyReports(params?: { page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<ReportVO>>>('/reports/my', { params })
  },
  // 管理员分页查询所有举报（按状态 / 举报类型筛选）
  list(params?: { status?: number; reportType?: string; page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<ReportVO>>>('/admin/reports', { params })
  },

  // 管理员处理举报（给出处理结论）
  handle(id: number | string, data: ReportHandleRequest) {
    return request.put<any, ApiResponse<void>>(`/admin/reports/${id}/handle`, data)
  }
}

// ==================== 归档查询模块（投放点管理员 / 系统管理员） ====================
export const archiveApi = {
  // 分页查询归档物品（长期未认领已被归档的历史数据）
  listItems(params: any = {}) {
    return request.get<any, ApiResponse<any[]>>('/admin/archive/items', { params })
  }
}

// ==================== 内容审核（下架 / 申诉）模块 ====================
export const moderationApi = {
  // 管理员下架某条物品信息（需给出理由）
  takedownItem(id: number | string, reason: string) {
    return request.put<any, ApiResponse<void>>(`/admin/items/${id}/takedown`, { reason })
  },
  // 管理员下架某条寻物启事
  takedownNotice(id: number | string, reason: string) {
    return request.put<any, ApiResponse<void>>(`/admin/notices/${id}/takedown`, { reason })
  },
  // 被下架物品的发布者提出申诉
  appealItem(id: number | string, reason: string) {
    return request.post<any, ApiResponse<void>>(`/items/${id}/appeal`, { reason })
  },
  // 被下架启事的发布者提出申诉
  appealNotice(id: number | string, reason: string) {
    return request.post<any, ApiResponse<void>>(`/notices/${id}/appeal`, { reason })
  },
  // 查询“我发布的信息中被下架的记录”（供发布者查看并申诉）
  myTakedowns() {
    return request.get<any, ApiResponse<{ items: any[]; notices: any[] }>>('/my-takedowns')
  },
  // 管理员查询待处理的申诉列表
  listAppeals() {
    return request.get<any, ApiResponse<{ items: FoundItem[]; notices: LostNotice[] }>>('/admin/appeals')
  },
  // 管理员审批物品申诉（approved: true 恢复上架 / false 驳回）
  handleItemAppeal(id: number | string, approved: boolean) {
    return request.put<any, ApiResponse<void>>(`/admin/items/${id}/appeal`, { approved })
  },
  // 管理员审批启事申诉
  handleNoticeAppeal(id: number | string, approved: boolean) {
    return request.put<any, ApiResponse<void>>(`/admin/notices/${id}/appeal`, { approved })
  }
}

// ==================== 投放点管理模块（增删改，管理员专用） ====================
export const dropPointAdminApi = {
  // 新建投放点
  create(data: Partial<DropPoint>) {
    return request.post<any, ApiResponse<DropPoint>>('/drop-points', data)
  },

  // 修改投放点信息
  update(id: number | string, data: Partial<DropPoint>) {
    return request.put<any, ApiResponse<DropPoint>>(`/drop-points/${id}`, data)
  },

  // 删除投放点
  delete(id: number | string) {
    return request.delete<any, ApiResponse<void>>(`/drop-points/${id}`)
  }
}
