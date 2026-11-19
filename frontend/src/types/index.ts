  // 按日期统计认领量（用于趋势图）


// ==================== 状态码映射常量 ====================
// 通用约定：每个 MAP 的 value 中，text 为展示文案，type 对应 Element Plus
// 标签（el-tag）的语义色：info 灰 / success 绿 / warning 橙 / danger 红 / primary 蓝。

// 争议类型映射
export const DISPUTE_TYPE_MAP: Record<string, { text: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  ITEM_MISMATCH: { text: '物品不符', type: 'danger' },
  OTHER: { text: '其他纠纷', type: 'warning' },
  FALSE_CLAIM: { text: '物品被冒领', type: 'primary' }
}

// 争议处理状态映射
export const DISPUTE_STATUS_MAP: Record<number, { text: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  0: { text: '待处理', type: 'warning' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已驳回', type: 'danger' }
}

// ==================== 举报相关 ====================

// 举报实体
export interface ReportVO {
  id: number
  reporterId: number
  reporterName?: string
  itemId: number
  itemTitle?: string
  reportType: string           // 举报类型（见 REPORT_TYPE_MAP）
  description: string
  evidenceImages: string[] | null
  status: number               // 处理状态（见 REPORT_STATUS_MAP）
  handlerNote?: string
  handlerId?: number
  handledAt?: string
  createdAt: string
}

// 提交举报的请求参数
export interface ReportCreateRequest {
  reportType: string
  targetType?: string          // 举报目标类型（物品/启事）
  itemId?: number
  noticeId?: number
  description: string
  evidenceImages?: string[]
}

// 处理举报的请求参数：valid 表示举报是否属实，banPublisher 可同时封禁发布者
export interface ReportHandleRequest {
  valid: boolean
  handlerNote: string
  banPublisher?: boolean
}

// 举报类型映射
export const REPORT_TYPE_MAP: Record<string, { text: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  FAKE_PUBLISH: { text: '虚假投放', type: 'danger' },
  DESC_MISMATCH: { text: '描述不符', type: 'warning' },
  OTHER: { text: '其他', type: 'info' }
}

// 举报处理状态映射
export const REPORT_STATUS_MAP: Record<number, { text: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  0: { text: '待处理', type: 'warning' },
  1: { text: '举报成立', type: 'danger' },
  2: { text: '不成立', type: 'info' }
}

// 物品状态映射（物品从发布到完结的生命周期）
export const ITEM_STATUS_MAP: Record<number, { text: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  0: { text: '已作废', type: 'info' },
  1: { text: '公开待认领', type: 'success' },
  2: { text: '认领中', type: 'warning' },
  3: { text: '已取件', type: 'primary' },
  4: { text: '已归档', type: 'info' },
  5: { text: '已过期', type: 'info' },
  6: { text: '待投放未公开', type: 'warning' }
}

// 认领申请状态映射（申请从提交到完结的生命周期）
export const CLAIM_STATUS_MAP: Record<number, { text: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '已完成', type: 'success' },
  2: { text: '已拒绝', type: 'danger' },
  3: { text: '已锁定', type: 'info' },
  4: { text: '待取件', type: 'primary' }
}

// 交物核对状态映射（捡到人交到投放点后，管理员核对入库的流程）
export const HAND_IN_STATUS_MAP: Record<number, { text: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  1: { text: '已交物待核对', type: 'warning' },
  2: { text: '已核对', type: 'success' }
}

// 寻物启事状态映射
export const NOTICE_STATUS_MAP: Record<number, { text: string; type: 'info' | 'success' | 'warning' | 'danger' | 'primary' }> = {
  0: { text: '进行中', type: 'success' },
  1: { text: '已找到', type: 'primary' },
  2: { text: '已关闭', type: 'info' }
}

// 物品分类固定选项：发布/筛选下拉框共用，保证前后端分类值一致
export const ITEM_CATEGORIES = ['电子产品', '证件卡类', '钥匙', '书籍资料','生活用品','易腐品','其他']