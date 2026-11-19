// composables/useImageTrail.ts —— 鼠标拖尾图片动画组合式函数
// 功能：监听鼠标在容器内移动，当移动距离超过阈值时，让一张张图片跟随鼠标轨迹
//       依次浮现、飞散、消失，形成“图片拖尾”视觉效果（首页 Hero 区的装饰动画）。
// 为什么需要：将复杂的 GSAP 动画逻辑封装成可复用的组合式函数（composable），
//       组件只需传入容器 ref 与图片列表即可使用，且自动在组件卸载时清理事件与动画，避免内存泄漏。
// 技术要点：GSAP（专业动画库）+ requestAnimationFrame 渲染循环 + 三种可选动画变体。
import { onBeforeUnmount, watch } from 'vue'
import type { Ref } from 'vue'
import gsap from 'gsap'

// 简单的二维坐标点
interface Point { x: number; y: number }

// 线性插值函数：返回 a、b 之间按比例 t 混合的值
// 用于让“缓存坐标”缓慢逼近“真实鼠标坐标”，产生拖尾的滞后感（值越小越滞后）
function lerp(a: number, b: number, t: number): number {
  return a * (1 - t) + b * t
}

// 把鼠标事件的页面坐标转换为容器内部坐标
// （getBoundingClientRect 得到容器左上角在视口的位置，相减即得相对坐标）
function getLocalPointerPos(container: HTMLElement, event: PointerEvent | Touch): Point {
  const rect = container.getBoundingClientRect()
  return { x: event.clientX - rect.left, y: event.clientY - rect.top }
}

// 计算两点间的欧氏距离（勾股定理），用于判断鼠标移动是否超过触发阈值
function getMouseDistance(a: Point, b: Point): number {
  return Math.hypot(a.x - b.x, a.y - b.y)
}

// 单张图片的包装类：持有 DOM 元素引用、内部图层引用与初始尺寸
class ImageItem {
  el: HTMLElement        // 外层元素（负责位置/缩放/透明度动画）
  inner: HTMLElement     // 内层元素（背景图所在，负责独立的缩放动画）
  rect: DOMRect          // 元素尺寸缓存，动画定位时需要用它计算偏移

  constructor(el: HTMLElement) {
    this.el = el
    this.inner = el.querySelector('.image-trail-inner') as HTMLElement
    this.rect = el.getBoundingClientRect()
    // 初始状态：不可见、无位移，等待被“展示”
    gsap.set(this.el, { scale: 1, x: 0, y: 0, opacity: 0 })
  }

  // 重新获取元素尺寸（窗口 resize 后 rect 会失效，需要刷新）
  getRect() {
    this.rect = this.el.getBoundingClientRect()
  }
}

// ==================== 变体 1：基础拖尾（浮现→跟随→缩小消失） ====================
class ImageTrailVariant1 {
  container: HTMLElement
  images: ImageItem[] = []
  mousePos: Point = { x: 0, y: 0 }       // 当前鼠标位置
  lastMousePos: Point = { x: 0, y: 0 }   // 上一次触发展示时的位置（用于计算移动距离）
  cacheMousePos: Point = { x: 0, y: 0 }  // 滞后的缓存位置（经 lerp 平滑，动画起点用它更有“拖”的感觉）
  threshold: number                      // 触发展示新图片所需的最小鼠标移动距离（px）
  imgPosition = 0                        // 轮询指针：下一次要展示图片在数组中的下标
  zIndexVal = 1                          // 递增的 z-index：新出现的图片总是盖在旧图片之上
  rafId: number | null = null            // requestAnimationFrame 的句柄，用于停止渲染循环
  // 预绑定事件处理函数，保证 add/removeEventListener 引用一致，能正确解绑
  bound = {
    handlePointerMove: this.handlePointerMove.bind(this),
    initRender: this.initRender.bind(this)
  }

  constructor(container: HTMLElement, threshold: number) {
    this.container = container
    this.threshold = threshold
    // 把容器内所有 .image-trail-item 元素包装成 ImageItem 实例
    this.images = Array.from(container.querySelectorAll('.image-trail-item')).map(el => new ImageItem(el as HTMLElement))
    // 监听鼠标移动（pointermove 同时兼容鼠标/触摸/手写笔）
    this.container.addEventListener('pointermove', this.bound.handlePointerMove)
    // 首次进入容器时初始化渲染循环（once: true 保证只触发一次）
    this.container.addEventListener('pointerenter', this.bound.initRender, { once: true })
  }

  // 记录最新鼠标位置（渲染循环中每帧读取）
  handlePointerMove(event: PointerEvent) {
    const pos = getLocalPointerPos(this.container, event)
    this.mousePos = pos
  }

  // 初始化：同步一次位置后启动 requestAnimationFrame 渲染循环
  initRender(event: PointerEvent) {
    const pos = getLocalPointerPos(this.container, event)
    this.mousePos = pos
    this.cacheMousePos = { ...this.mousePos }   // 缓存位置与真实位置对齐，避免首帧跳变
    this.rafId = requestAnimationFrame(() => this.render())
  }

  // 渲染循环：每帧执行
  render() {
    // 计算鼠标相对上一次展示点的移动距离
    const distance = getMouseDistance(this.mousePos, this.lastMousePos)
    // 缓存位置以 0.1 的比例缓慢逼近真实位置（拖尾滞后的核心）
    this.cacheMousePos.x = lerp(this.cacheMousePos.x, this.mousePos.x, 0.1)
    this.cacheMousePos.y = lerp(this.cacheMousePos.y, this.mousePos.y, 0.1)

    // 移动距离超过阈值：展示下一张图片，并更新“上次位置”
    if (distance > this.threshold) {
      this.showNextImage()
      this.lastMousePos = { ...this.mousePos }
    }

    // 继续下一帧（浏览器标签页切走时会自动暂停，节能）
    this.rafId = requestAnimationFrame(() => this.render())
  }

  // 展示下一张图片：从缓存位置浮现 → 移向真实鼠标位置 → 缩小淡出
  showNextImage() {
    // 取当前轮询指针指向的图片，指针 +1 并取模实现循环轮播
    const img = this.images[this.imgPosition]
    this.imgPosition = (this.imgPosition + 1) % this.images.length
    this.zIndexVal++   // 层级递增，新图盖住旧图

    // 先杀掉该元素正在进行的旧动画，避免新动画与旧动画叠加冲突
    gsap.killTweensOf(img.el)
    gsap.killTweensOf(img.inner)

    // GSAP 时间轴：动画序列
    const tl = gsap.timeline()
    // 第一段（0~0.4s）：在缓存位置出现（不透明、原尺寸），移向真实鼠标位置
    tl.fromTo(img.el, {
      opacity: 1,
      scale: 1,
      zIndex: this.zIndexVal,
      // 减去宽高的一半：把元素的左上角定位转换为“中心点对准鼠标”
      x: this.cacheMousePos.x - img.rect.width / 2,
      y: this.cacheMousePos.y - img.rect.height / 2
    }, {
      duration: 0.4,
      ease: 'power1',
      x: this.mousePos.x - img.rect.width / 2,
      y: this.mousePos.y - img.rect.height / 2
    })
    // 第二段（0.4s 起）：缩小到 0.2 倍并淡出，完成消散
    .to(img.el, {
      duration: 0.4,
      ease: 'power3',
      opacity: 0,
      scale: 0.2
    }, 0.4)
  }

  // 销毁：停止渲染循环、解绑事件、杀掉所有动画（防止内存泄漏）
  destroy() {
    if (this.rafId !== null) {
      cancelAnimationFrame(this.rafId)
    }
    this.container.removeEventListener('pointermove', this.bound.handlePointerMove)
    gsap.killTweensOf(this.images.map(img => img.el))
    gsap.killTweensOf(this.images.map(img => img.inner))
  }
}

// ==================== 变体 3：缩放弹出 + 向上飘散消失 ====================
// 继承变体 1 的全部交互逻辑，只重写“展示单张图片”的动画细节
class ImageTrailVariant3 extends ImageTrailVariant1 {
  showNextImage() {
    const img = this.images[this.imgPosition]
    this.imgPosition = (this.imgPosition + 1) % this.images.length
    this.zIndexVal++

    gsap.killTweensOf(img.el)
    gsap.killTweensOf(img.inner)

    const tl = gsap.timeline()
    // 第一段：从 0 倍缩放弹出至 1 倍，同时移向鼠标当前位置
    tl.fromTo(img.el, {
      opacity: 1,
      scale: 0,
      zIndex: this.zIndexVal,
      x: this.cacheMousePos.x - img.rect.width / 2,
      y: this.cacheMousePos.y - img.rect.height / 2
    }, {
      duration: 0.4,
      ease: 'power1',
      scale: 1,
      x: this.mousePos.x - img.rect.width / 2,
      y: this.mousePos.y - img.rect.height / 2
    })
    // 第二段：内部图层从 1.2 倍缩回 1 倍，产生“回弹聚焦”质感
    .fromTo(img.inner, {
      scale: 1.2
    }, {
      duration: 0.4,
      ease: 'power1',
      scale: 1
    }, 0)
    // 第三段（0.6s 起）：向上飘散消失，x 方向随机偏移 ±30%，模拟被“吹走”
    .to(img.el, {
      duration: 0.6,
      ease: 'power2',
      opacity: 0,
      scale: 0.2,
      // 函数值：每次执行时随机计算，实现无规律的横向漂移
      xPercent: () => gsap.utils.random(-30, 30),
      yPercent: -200
    }, 0.6)
  }
}

// ==================== 变体 5：跟随鼠标方向旋转飞出 ====================
class ImageTrailVariant5 extends ImageTrailVariant1 {
  lastAngle = 0   // 上一次计算的鼠标运动方向角（度）

  showNextImage() {
    const img = this.images[this.imgPosition]
    this.imgPosition = (this.imgPosition + 1) % this.images.length
    this.zIndexVal++

    // 鼠标本帧的位移向量
    const dx = this.mousePos.x - this.lastMousePos.x
    const dy = this.mousePos.y - this.lastMousePos.y

    // atan2 把位移向量转换为角度（弧度→度，0~360）
    let angle = Math.atan2(dy, dx) * (180 / Math.PI)
    if (angle < 0) angle += 360
    // 若角度落在“图像倒置区”(90°~270°]，加 180° 翻转，保证图片始终朝向运动方向的正向
    if (angle > 90 && angle <= 270) angle += 180

    // 根据角度变化趋势决定起始旋转角：顺时针偏 +10°，逆时针偏 -10°，制造甩动感
    const isClockwise = angle > this.lastAngle
    const startAngle = this.lastAngle + (isClockwise ? 10 : -10)
    this.lastAngle = angle

    // 位移缩放系数：控制飞出距离与位移的联动幅度
    const dxScaled = dx / 150
    const dyScaled = dy / 150

    gsap.killTweensOf(img.el)
    gsap.killTweensOf(img.inner)

    const tl = gsap.timeline()
    // 第一段：从 0.1 倍 + 偏转角 + 暗色亮度浮现，1 秒内转正、变亮、放大
    // 初始位置在鼠标位置基础上沿运动方向偏移 70px，随后被“甩”向鼠标处
    tl.fromTo(img.el, {
      opacity: 1,
      filter: 'brightness(80%)',
      scale: 0.1,
      rotation: startAngle,
      zIndex: this.zIndexVal,
      x: this.cacheMousePos.x - img.rect.width / 2 + dxScaled * 70,
      y: this.cacheMousePos.y - img.rect.height / 2 + dyScaled * 70
    }, {
      duration: 1,
      ease: 'power2',
      scale: 1,
      filter: 'brightness(100%)',
      rotation: this.lastAngle
    })
    // 第二段（0.5s 起）：淡出（expo 急缓动）
    .to(img.el, {
      duration: 0.4,
      ease: 'expo',
      opacity: 0
    }, 0.5)
    // 第三段：继续沿鼠标运动方向惯性滑行 120px（+= 表示在当前值上叠加）
    .to(img.el, {
      duration: 1.5,
      ease: 'power4',
      x: `+=${dxScaled * 120}`,
      y: `+=${dyScaled * 120}`
    }, 0.05)
  }
}

// ==================== 组合式函数入口 ====================
// 参数：
//   container —— 容器元素的 ref（动画作用范围）
//   images    —— 图片 URL 列表
//   variant   —— 动画变体（1 / 3 / 5，见上三类实现）
//   threshold —— 触发新图的最小鼠标移动距离
export function useImageTrail(
  container: Ref<HTMLElement | null>,
  images: string[],
  variant: 1 | 3 | 5 = 1,
  threshold = 80
) {
  // 持有当前动画引擎实例，init/cleanup 之间共享
  let instance: ImageTrailVariant1 | null = null

  // 初始化：容器存在且图片非空时，按 variant 选择对应类创建实例
  const init = () => {
    if (!container.value || images.length === 0) return

    const VariantClass = variant === 3 ? ImageTrailVariant3 : variant === 5 ? ImageTrailVariant5 : ImageTrailVariant1
    instance = new VariantClass(container.value, threshold) as ImageTrailVariant1
  }

  // 清理：销毁实例（停帧、解绑、杀动画），并把引用置空
  const cleanup = () => {
    instance?.destroy()
    instance = null
  }

  // 监听容器 ref 与图片数量的变化：变化时先清理旧实例再重建
  // immediate: true 保证组合式函数被调用时立即执行一次初始化
  watch([container, () => images.length], () => {
    cleanup()
    init()
  }, { immediate: true })

  // 组件卸载前自动清理，防止事件监听与动画残留
  onBeforeUnmount(cleanup)
}
