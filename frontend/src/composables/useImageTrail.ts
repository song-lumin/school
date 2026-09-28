import { onBeforeUnmount, watch } from 'vue'
import type { Ref } from 'vue'
import gsap from 'gsap'

interface Point { x: number; y: number }

function lerp(a: number, b: number, t: number): number {
  return a * (1 - t) + b * t
}

function getLocalPointerPos(container: HTMLElement, event: PointerEvent | Touch): Point {
  const rect = container.getBoundingClientRect()
  return { x: event.clientX - rect.left, y: event.clientY - rect.top }
}

function getMouseDistance(a: Point, b: Point): number {
  return Math.hypot(a.x - b.x, a.y - b.y)
}

class ImageItem {
  el: HTMLElement
  inner: HTMLElement
  rect: DOMRect

  constructor(el: HTMLElement) {
    this.el = el
    this.inner = el.querySelector('.image-trail-inner') as HTMLElement
    this.rect = el.getBoundingClientRect()
    gsap.set(this.el, { scale: 1, x: 0, y: 0, opacity: 0 })
  }

  getRect() {
    this.rect = this.el.getBoundingClientRect()
  }
}

class ImageTrailVariant1 {
  container: HTMLElement
  images: ImageItem[] = []
  mousePos: Point = { x: 0, y: 0 }
  lastMousePos: Point = { x: 0, y: 0 }
  cacheMousePos: Point = { x: 0, y: 0 }
  threshold: number
  imgPosition = 0
  zIndexVal = 1
  rafId: number | null = null
  bound = {
    handlePointerMove: this.handlePointerMove.bind(this),
    initRender: this.initRender.bind(this)
  }

  constructor(container: HTMLElement, threshold: number) {
    this.container = container
    this.threshold = threshold
    this.images = Array.from(container.querySelectorAll('.image-trail-item')).map(el => new ImageItem(el as HTMLElement))
    this.container.addEventListener('pointermove', this.bound.handlePointerMove)
    this.container.addEventListener('pointerenter', this.bound.initRender, { once: true })
  }

  handlePointerMove(event: PointerEvent) {
    const pos = getLocalPointerPos(this.container, event)
    this.mousePos = pos
  }

  initRender(event: PointerEvent) {
    const pos = getLocalPointerPos(this.container, event)
    this.mousePos = pos
    this.cacheMousePos = { ...this.mousePos }
    this.rafId = requestAnimationFrame(() => this.render())
  }

  render() {
    const distance = getMouseDistance(this.mousePos, this.lastMousePos)
    this.cacheMousePos.x = lerp(this.cacheMousePos.x, this.mousePos.x, 0.1)
    this.cacheMousePos.y = lerp(this.cacheMousePos.y, this.mousePos.y, 0.1)

    if (distance > this.threshold) {
      this.showNextImage()
      this.lastMousePos = { ...this.mousePos }
    }

    this.rafId = requestAnimationFrame(() => this.render())
  }

  showNextImage() {
    const img = this.images[this.imgPosition]
    this.imgPosition = (this.imgPosition + 1) % this.images.length
    this.zIndexVal++

    gsap.killTweensOf(img.el)
    gsap.killTweensOf(img.inner)

    const tl = gsap.timeline()
    tl.fromTo(img.el, {
      opacity: 1,
      scale: 1,
      zIndex: this.zIndexVal,
      x: this.cacheMousePos.x - img.rect.width / 2,
      y: this.cacheMousePos.y - img.rect.height / 2
    }, {
      duration: 0.4,
      ease: 'power1',
      x: this.mousePos.x - img.rect.width / 2,
      y: this.mousePos.y - img.rect.height / 2
    })
    .to(img.el, {
      duration: 0.4,
      ease: 'power3',
      opacity: 0,
      scale: 0.2
    }, 0.4)
  }

  destroy() {
    if (this.rafId !== null) {
      cancelAnimationFrame(this.rafId)
    }
    this.container.removeEventListener('pointermove', this.bound.handlePointerMove)
    gsap.killTweensOf(this.images.map(img => img.el))
    gsap.killTweensOf(this.images.map(img => img.inner))
  }
}

class ImageTrailVariant3 extends ImageTrailVariant1 {
  showNextImage() {
    const img = this.images[this.imgPosition]
    this.imgPosition = (this.imgPosition + 1) % this.images.length
    this.zIndexVal++

    gsap.killTweensOf(img.el)
    gsap.killTweensOf(img.inner)

    const tl = gsap.timeline()
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
    .fromTo(img.inner, {
      scale: 1.2
    }, {
      duration: 0.4,
      ease: 'power1',
      scale: 1
    }, 0)
    .to(img.el, {
      duration: 0.6,
      ease: 'power2',
      opacity: 0,
      scale: 0.2,
      xPercent: () => gsap.utils.random(-30, 30),
      yPercent: -200
    }, 0.6)
  }
}

class ImageTrailVariant5 extends ImageTrailVariant1 {
  lastAngle = 0

  showNextImage() {
    const img = this.images[this.imgPosition]
    this.imgPosition = (this.imgPosition + 1) % this.images.length
    this.zIndexVal++

    const dx = this.mousePos.x - this.lastMousePos.x
    const dy = this.mousePos.y - this.lastMousePos.y

    let angle = Math.atan2(dy, dx) * (180 / Math.PI)
    if (angle < 0) angle += 360
    if (angle > 90 && angle <= 270) angle += 180

    const isClockwise = angle > this.lastAngle
    const startAngle = this.lastAngle + (isClockwise ? 10 : -10)
    this.lastAngle = angle

    const dxScaled = dx / 150
    const dyScaled = dy / 150

    gsap.killTweensOf(img.el)
    gsap.killTweensOf(img.inner)

    const tl = gsap.timeline()
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
    .to(img.el, {
      duration: 0.4,
      ease: 'expo',
      opacity: 0
    }, 0.5)
    .to(img.el, {
      duration: 1.5,
      ease: 'power4',
      x: `+=${dxScaled * 120}`,
      y: `+=${dyScaled * 120}`
    }, 0.05)
  }
}

export function useImageTrail(
  container: Ref<HTMLElement | null>,
  images: string[],
  variant: 1 | 3 | 5 = 1,
  threshold = 80
) {
  let instance: ImageTrailVariant1 | null = null

  const init = () => {
    if (!container.value || images.length === 0) return

    const VariantClass = variant === 3 ? ImageTrailVariant3 : variant === 5 ? ImageTrailVariant5 : ImageTrailVariant1
    instance = new VariantClass(container.value, threshold) as ImageTrailVariant1
  }

  const cleanup = () => {
    instance?.destroy()
    instance = null
  }

  watch([container, () => images.length], () => {
    cleanup()
    init()
  }, { immediate: true })

  onBeforeUnmount(cleanup)
}
