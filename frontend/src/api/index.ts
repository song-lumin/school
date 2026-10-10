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

export const authApi = {
  register(data: RegisterRequest) {
    return request.post<any, ApiResponse<LoginResponse>>('/auth/register', data)
  },

  login(data: LoginRequest) {
    return request.post<any, ApiResponse<LoginResponse>>('/auth/login', data)
  },

  logout() {
    return request.post<any, ApiResponse<void>>('/auth/logout')
  },

  getCurrentUser() {
    return request.get<any, ApiResponse<User>>('/auth/me')
  },

  updateProfile(data: { realName?: string; phone?: string; email?: string }) {
    return request.put<any, ApiResponse<User>>('/auth/profile', data)
  },

  updatePassword(data: { oldPassword: string; newPassword: string }) {
    return request.put<any, ApiResponse<void>>('/auth/password', data)
  }
}

export const uploadApi = {
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

export const foundItemApi = {
  publish(data: FoundItemRequest) {
    return request.post<any, ApiResponse<FoundItem>>('/found-items', data)
  },

  list(params?: ItemQueryRequest) {
    return request.get<any, ApiResponse<PageResult<FoundItem>>>('/found-items', { params })
  },

  listMy(params?: ItemQueryRequest) {
    return request.get<any, ApiResponse<PageResult<FoundItem>>>('/found-items/my', { params })
  },

  getById(id: number | string) {
    return request.get<any, ApiResponse<FoundItem>>(`/found-items/${id}`)
  },

  invalidate(id: number | string) {
    return request.put<any, ApiResponse<void>>(`/found-items/${id}/invalidate`)
  },

  markPlaced(id: number | string) {
    return request.put<any, ApiResponse<void>>(`/found-items/${id}/mark-placed`)
  },

  forwardToNotice(id: number | string, noticeId: number) {
    return request.put<any, ApiResponse<void>>(`/found-items/${id}/forward`, { noticeId })
  },

  archive(id: number | string) {
    return request.put<any, ApiResponse<void>>(`/found-items/${id}/archive`)
  },

  remove(id: number | string) {
    return request.delete<any, ApiResponse<void>>(`/found-items/${id}`)
  }
}

export const dropPointApi = {
  list() {
    return request.get<any, ApiResponse<DropPoint[]>>('/drop-points')
  },

  getById(id: number | string) {
    return request.get<any, ApiResponse<DropPoint>>(`/drop-points/${id}`)
  },

  getPendingItems(id: number | string) {
    return request.get<any, ApiResponse<FoundItem[]>>(`/drop-points/${id}/items`)
  },

  checkItem(id: number | string, data: { itemId: number; checkNote?: string }) {
    return request.put<any, ApiResponse<void>>(`/drop-points/${id}/check`, data)
  },

  getPendingPickups(id: number | string) {
    return request.get<any, ApiResponse<ClaimApply[]>>(`/drop-points/${id}/pending-pickups`)
  },

  getInventory(id: number | string) {
    return request.get<any, ApiResponse<any[]>>(`/drop-points/${id}/inventory`)
  }
}

export const claimApi = {
  apply(data: ClaimApplyRequest) {
    return request.post<any, ApiResponse<ClaimApply>>('/claims', data)
  },

  listByItem(itemId: number | string, params?: { page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<ClaimApply>>>('/claims', {
      params: { itemId, ...params }
    })
  },

  getById(id: number | string) {
    return request.get<any, ApiResponse<ClaimApply>>(`/claims/${id}`)
  },

  approve(id: number | string) {
    return request.put<any, ApiResponse<void>>(`/claims/${id}/approve`)
  },

  reject(id: number | string, data: { rejectReason?: string }) {
    return request.put<any, ApiResponse<void>>(`/claims/${id}/reject`, data)
  },

  pickup(id: number | string, data?: { pickupPhoto?: string; pickupSignature?: string }) {
    return request.put<any, ApiResponse<void>>(`/claims/${id}/pickup`, data || {})
  },

  cancelPickup(id: number | string) {
    return request.put<any, ApiResponse<void>>(`/claims/${id}/cancel-pickup`)
  },

  listMy(params?: { page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<ClaimApply>>>('/claims/my', { params })
  },

  listForReview(params?: { status?: number; page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<ClaimApply>>>('/claims/review', { params })
  }
}

export const creditApi = {
  getMyCredit() {
    return request.get<any, ApiResponse<number>>('/credits/me')
  },

  getMyLogs(params?: { page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<CreditLog>>>('/credits/logs', { params })
  },

  getLeaderboard(top = 100) {
    return request.get<any, ApiResponse<LeaderboardVO>>('/credits/leaderboard', { params: { top } })
  },

  getCertificate() {
    return request.get<any, ApiResponse<CertificateVO>>('/credits/certificate')
  },

  setLeaderboardSetting(allow: number) {
    return request.put<any, ApiResponse<void>>('/credits/leaderboard-setting', {
      allowLeaderboard: allow
    })
  }
}

export const lostNoticeApi = {
  publish(data: LostNoticeRequest) {
    return request.post<any, ApiResponse<LostNotice>>('/lost-notices', data)
  },

  list(params?: { keyword?: string; category?: string; status?: number; page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<LostNotice>>>('/lost-notices', { params })
  },

  getById(id: number | string) {
    return request.get<any, ApiResponse<LostNotice>>(`/lost-notices/${id}`)
  },

  searchByImage(file: File, category?: string) {
    const formData = new FormData()
    formData.append('image', file)
    if (category) formData.append('category', category)
    return request.post<any, ApiResponse<Array<{ item: FoundItem; similarity: number }>>>(
      '/lost-notices/search-by-image', formData, { headers: { 'Content-Type': 'multipart/form-data' } }
    )
  },

  forward(id: number | string, data: { itemId: number; answer: string }) {
    return request.post<any, ApiResponse<ClaimApply>>(`/lost-notices/${id}/forward`, data)
  },

  matches(id: number | string) {
    return request.get<any, ApiResponse<Array<{ itemId: number; title: string; category: string; images: string[] | null; foundLocation: string; itemStatus: number; claimQuestion: string }>>>(`/lost-notices/${id}/matches`)
  },

  forwardedItems(id: number | string) {
    return request.get<any, ApiResponse<FoundItem[]>>(`/lost-notices/${id}/forwarded-items`)
  },

  close(id: number | string) {
    return request.put<any, ApiResponse<void>>(`/lost-notices/${id}/close`)
  },

  remove(id: number | string) {
    return request.delete<any, ApiResponse<void>>(`/lost-notices/${id}`)
  }
}

export const disputeApi = {
  create(data: DisputeCreateRequest) {
    return request.post<any, ApiResponse<DisputeVO>>('/disputes', data)
  },

  listMy(params?: { page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<DisputeVO>>>('/disputes/my', { params })
  },

  getById(id: number | string) {
    return request.get<any, ApiResponse<DisputeVO>>(`/disputes/${id}`)
  },

  list(params?: { disputeType?: string; status?: number; page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<DisputeVO>>>('/disputes', { params })
  },

  handle(id: number | string, data: DisputeHandleRequest) {
    return request.put<any, ApiResponse<void>>(`/disputes/${id}/handle`, data)
  }
}

export const riskControlApi = {
  getWarnings() {
    return request.get<any, ApiResponse<RiskWarningVO[]>>('/admin/risk-control/warnings')
  },

  getCollusion() {
    return request.get<any, ApiResponse<RiskWarningVO[]>>('/admin/risk-control/collusion')
  },

  getClaimRate() {
    return request.get<any, ApiResponse<RiskWarningVO[]>>('/admin/risk-control/claim-rate')
  },

  getDuplicateImages() {
    return request.get<any, ApiResponse<RiskWarningVO[]>>('/admin/risk-control/duplicates')
  }
}

export interface CameraLog {
  id: number
  dropPointId: number
  dropPointName: string
  itemId: number | null
  itemTitle: string | null
  applyReason: string
  applicantName: string
  timeRangeStart: string
  timeRangeEnd: string
  status: number
  resultNote: string | null
  handlerName: string | null
  handledAt: string | null
  createdAt: string
}

export const cameraLogAdminApi = {
  list(params: { status?: number; dropPointId?: number; itemId?: number; page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<CameraLog>>>('/admin/camera-logs', { params })
  },
  create(data: { dropPointId: number; itemId?: number; applyReason: string; timeRangeStart: string; timeRangeEnd: string }) {
    return request.post<any, ApiResponse<CameraLog>>('/admin/camera-logs', data)
  },
  handle(id: number, data: { status: number; resultNote: string }) {
    return request.put<any, ApiResponse<void>>(`/admin/camera-logs/${id}`, data)
  }
}

export const adminApi = {
  getDashboard() {
    return request.get<any, ApiResponse<DashboardVO>>('/admin/dashboard')
  },

  listUsers(params?: { keyword?: string; role?: string; status?: number; page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<User>>>('/admin/users', { params })
  },

  adjustCredit(userId: number | string, data: { newScore: number; reason: string }) {
    return request.put<any, ApiResponse<void>>(`/admin/users/${userId}/credit`, data)
  },

  userCreditLogs(userId: number | string) {
    return request.get<any, ApiResponse<any>>(`/admin/users/${userId}/credit-logs`)
  },

  getExpireWarnings() {
    return request.get<any, ApiResponse<any[]>>('/admin/expire-warnings')
  }
}

export const reportApi = {
  create(data: ReportCreateRequest) {
    return request.post<any, ApiResponse<ReportVO>>('/reports', data)
  },

  listMyReports(params?: { page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<ReportVO>>>('/reports/my', { params })
  },
  list(params?: { status?: number; reportType?: string; page?: number; size?: number }) {
    return request.get<any, ApiResponse<PageResult<ReportVO>>>('/admin/reports', { params })
  },

  handle(id: number | string, data: ReportHandleRequest) {
    return request.put<any, ApiResponse<void>>(`/admin/reports/${id}/handle`, data)
  }
}


export const archiveApi = {
  listItems(params: any = {}) {
    return request.get<any, ApiResponse<any[]>>('/admin/archive/items', { params })
  }
}

export const moderationApi = {
  takedownItem(id: number | string, reason: string) {
    return request.put<any, ApiResponse<void>>(`/admin/items/${id}/takedown`, { reason })
  },
  takedownNotice(id: number | string, reason: string) {
    return request.put<any, ApiResponse<void>>(`/admin/notices/${id}/takedown`, { reason })
  },
  appealItem(id: number | string, reason: string) {
    return request.post<any, ApiResponse<void>>(`/items/${id}/appeal`, { reason })
  },
  appealNotice(id: number | string, reason: string) {
    return request.post<any, ApiResponse<void>>(`/notices/${id}/appeal`, { reason })
  },
  myTakedowns() {
    return request.get<any, ApiResponse<{ items: any[]; notices: any[] }>>('/my-takedowns')
  },
  listAppeals() {
    return request.get<any, ApiResponse<{ items: FoundItem[]; notices: LostNotice[] }>>('/admin/appeals')
  },
  handleItemAppeal(id: number | string, approved: boolean) {
    return request.put<any, ApiResponse<void>>(`/admin/items/${id}/appeal`, { approved })
  },
  handleNoticeAppeal(id: number | string, approved: boolean) {
    return request.put<any, ApiResponse<void>>(`/admin/notices/${id}/appeal`, { approved })
  }
}

export const dropPointAdminApi = {
  create(data: Partial<DropPoint>) {
    return request.post<any, ApiResponse<DropPoint>>('/drop-points', data)
  },

  update(id: number | string, data: Partial<DropPoint>) {
    return request.put<any, ApiResponse<DropPoint>>(`/drop-points/${id}`, data)
  },

  delete(id: number | string) {
    return request.delete<any, ApiResponse<void>>(`/drop-points/${id}`)
  }
}
