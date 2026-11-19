/**
 * 拾取时间的模糊展示。
 * 后端 found_time 始终有值（模糊精度下只代表日期），另外存两个字段：
 * found_time_precision 记录「模糊到哪一档」，found_time_text 记录自由描述。
 * 展示优先级：自由描述 > 时段词 > 精确时分。
 */

export const FOUND_TIME_PRECISION_OPTIONS = [
  { value: 1, label: '只知道日期（推荐）' },
  { value: 2, label: '上午' },
  { value: 3, label: '下午' },
  { value: 4, label: '傍晚' },
  { value: 5, label: '晚上' },
  { value: 0, label: '精确到分钟' }
]

/** 模糊档位对应的占位时刻，仅用于满足 found_time 非空，不对外展示 */
export const PRECISION_PLACEHOLDER_HOUR: Record<number, number> = {
  1: 0,
  2: 9,
  3: 15,
  4: 18,
  5: 21
}

const PERIOD_TEXT: Record<number, string> = {
  1: '',
  2: '上午',
  3: '下午',
  4: '傍晚',
  5: '晚上'
}

/** 取日期部分 YYYY-MM-DD */
function datePart(time: string): string {
  return time.replace('T', ' ').slice(0, 10)
}

/**
 * @param time      后端 foundTime（ISO 字符串）
 * @param precision 0 精确 / 1 仅日期 / 2 上午 / 3 下午 / 4 傍晚 / 5 晚上
 * @param text      用户手填的模糊描述
 */
export function describeFoundTime(
  time?: string | null,
  precision?: number | null,
  text?: string | null
): string {
  if (!time) return '-'
  const day = datePart(time)
  const desc = (text || '').trim()
  if (desc && day) return `${day} · ${desc}`

  const p = precision == null ? 0 : precision
  if (p === 1) return day
  const period = PERIOD_TEXT[p]
  if (period) return `${day} ${period}`

  // 精确档：保留到分钟
  return time.replace('T', ' ').slice(0, 16)
}
