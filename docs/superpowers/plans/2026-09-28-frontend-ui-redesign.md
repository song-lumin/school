# 前端 UI 改造实施计划:Pinterest 风格 + GSAP ImageTrail

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将核心页面升级为 Pinterest 风格卡片化设计,并移植 reactbits ImageTrail 动画(GSAP 驱动)到 Vue 3

**Architecture:** 新增组合式函数 `useImageTrail.ts` 封装 GSAP 动画核心(rAF 循环+3 种变体),`ImageTrail.vue` 包装组件;首页/列表页采用 CSS columns 瀑布流(零 JS,响应式);详情页两栏大图布局;导航栏毛玻璃效果。保留现有 Element Plus 组件与业务逻辑,只重塑视觉层。

**Tech Stack:** Vue 3.4 / Element Plus 2.5 / Vite 5 / TypeScript 5.3 / GSAP 3.12

**Spec:** `docs/superpowers/specs/2026-09-28-frontend-ui-redesign-design.md`

## Global Constraints

- Vue 3.4+, Element Plus 2.5+, TypeScript strict mode
- 新增依赖:gsap ^3.12.0(唯一运行时依赖)
- 保留绿色主色 `#26745c` 与全部 `--el-color-primary-*` 映射
- 尊重 `prefers-reduced-motion: reduce`(动画禁用)
- 零回归:未改造的 11 个页面(LoginView/RegisterView 等)样式不受影响
- 图片兜底:无真实图时用分类色 SVG data-URL 瓦片
- 验证:`vue-tsc && vite build` 零错误 + dev server 浏览器实测

---

### Task 1: 安装 GSAP 依赖

**Files:**
- Modify: `frontend/package.json`

**Interfaces:**
- Consumes: package.json 现有依赖列表
- Produces: gsap ^3.12.0 安装到 node_modules,后续任务可 `import gsap from 'gsap'`

- [ ] **Step 1: 添加 gsap 到 dependencies**

在 `frontend/` 目录执行:

```bash
cd frontend
npm install gsap@^3.12.0
```

- [ ] **Step 2: 验证安装**

Run: `npm list gsap`
Expected: 显示 `gsap@3.12.x`

- [ ] **Step 3: Commit**

```bash
git add package.json package-lock.json
git commit -m "feat: add gsap ^3.12.0 for ImageTrail animation"
```

---

### Task 2: 新增设计令牌到 theme.css

**Files:**
- Modify: `frontend/src/styles/theme.css:22` (`:root` 块末尾,`--el-border-radius-base` 下方)

**Interfaces:**
- Consumes: 现有 CSS 变量 `--lf-ink`, `--lf-green` 等
- Produces: 新变量 `--lf-radius`, `--lf-shadow-1/2/3`, `--lf-space-1~6`,后续任务用于卡片/阴影/间距

- [ ] **Step 1: 在 :root 块末尾追加新令牌**

在 `theme.css:22` 后(`:root` 块内,`--el-border-radius-base: 3px;` 下方)插入:

```css
  --lf-radius-sm: 8px;
  --lf-radius: 12px;
  --lf-radius-lg: 16px;
  --lf-shadow-1: 0 1px 2px rgb(33 64 48 / 6%);
  --lf-shadow-2: 0 8px 24px rgb(33 64 48 / 10%);
  --lf-shadow-3: 0 16px 48px rgb(33 64 48 / 16%);
  --lf-space-1: 4px;
  --lf-space-2: 8px;
  --lf-space-3: 12px;
  --lf-space-4: 16px;
  --lf-space-5: 24px;
  --lf-space-6: 32px;
```

- [ ] **Step 2: 验证 CSS 语法**

Run: `npm run build`
Expected: Vite 编译通过,无 CSS 解析错误

- [ ] **Step 3: Commit**

```bash
git add src/styles/theme.css
git commit -m "feat(theme): add design tokens for cards, shadows, spacing"
```

---

### Task 3: 创建 SVG 兜底瓦片工具函数

**Files:**
- Create: `frontend/src/utils/placeholder.ts`

**Interfaces:**
- Consumes: `ITEM_CATEGORIES` 从 `@/types`,分类色调映射逻辑(复用 HomeView 现有 `categoryTone`)
- Produces: `categoryTile(category: string): string` 返回 SVG data-URL,供 ImageTrail 和瀑布流卡片使用

- [ ] **Step 1: 创建文件并写入函数骨架**

```typescript
const CATEGORY_COLORS: Record<string, string> = {
  coral: '#d88770',
  blue: '#7899a5',
  yellow: '#c3a668',
  green: '#81a78e'
}

const CATEGORY_ICON_PATHS: Record<string, string> = {
  coral: 'M7 18c-1.1 0-1.99.9-1.99 2S5.9 22 7 22s2-.9 2-2-.9-2-2-2zM1 2v2h2l3.6 7.59-1.35 2.45c-.16.28-.25.61-.25.96 0 1.1.9 2 2 2h12v-2H7.42c-.14 0-.25-.11-.25-.25l.03-.12.9-1.63h7.45c.75 0 1.41-.41 1.75-1.03l3.58-6.49c.08-.14.12-.31.12-.48 0-.55-.45-1-1-1H5.21l-.94-2H1zm16 16c-1.1 0-1.99.9-1.99 2s.89 2 1.99 2 2-.9 2-2-.9-2-2-2z',
  blue: 'M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z',
  yellow: 'M18 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM6 4h5v8l-2.5-1.5L6 12V4z',
  green: 'M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z'
}

function categoryTone(category: string): 'coral' | 'blue' | 'yellow' | 'green' {
  if (category.includes('证件') || category.includes('卡')) return 'coral'
  if (category.includes('电子')) return 'blue'
  if (category.includes('书')) return 'yellow'
  return 'green'
}

export function categoryTile(category: string): string {
  const tone = categoryTone(category)
  const color = CATEGORY_COLORS[tone]
  const iconPath = CATEGORY_ICON_PATHS[tone]
  
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="200" height="150" viewBox="0 0 200 150"><rect width="200" height="150" fill="${color}"/><path d="${iconPath}" fill="rgba(255,255,255,0.85)" transform="translate(88, 63) scale(1.4)"/></svg>`
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
}
```

- [ ] **Step 2: 验证类型检查**

Run: `npm run build`
Expected: TypeScript 编译通过,无类型错误

- [ ] **Step 3: Commit**

```bash
git add src/utils/placeholder.ts
git commit -m "feat(utils): add categoryTile for SVG fallback images"
```

---

### Task 4: 创建 useImageTrail 组合式函数(V1 变体)

**Files:**
- Create: `frontend/src/composables/useImageTrail.ts`

**Interfaces:**
- Consumes: gsap 库,DOM 容器 ref
- Produces: `useImageTrail(container: Ref<HTMLElement | null>, images: string[], variant: 1 | 3 | 5, threshold: number): void` 启动动画循环,返回自动清理

- [ ] **Step 1: 创建文件并写入 V1 变体基础结构**

```typescript
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

export function useImageTrail(
  container: Ref<HTMLElement | null>,
  images: string[],
  variant: 1 | 3 | 5 = 1,
  threshold = 80
) {
  let instance: ImageTrailVariant1 | null = null

  const init = () => {
    if (!container.value || images.length === 0) return
    instance = new ImageTrailVariant1(container.value, threshold)
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
```

- [ ] **Step 2: 验证类型检查**

Run: `npm run build`
Expected: TypeScript 编译通过

- [ ] **Step 3: Commit**

```bash
git add src/composables/useImageTrail.ts
git commit -m "feat(composables): add useImageTrail with V1 variant"
```

---

### Task 5: 扩展 useImageTrail 添加 V3/V5 变体

**Files:**
- Modify: `frontend/src/composables/useImageTrail.ts` (在 ImageTrailVariant1 下方添加 V3/V5 类)

**Interfaces:**
- Consumes: V1 变体的基础结构
- Produces: V3(飞散)/V5(旋转跟随)两个变体类,useImageTrail 根据 variant 参数选择

- [ ] **Step 1: 在 ImageTrailVariant1 类下方添加 V3 变体**

在 `ImageTrailVariant1` 类定义后,`useImageTrail` 函数前插入:

```typescript
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
```

- [ ] **Step 2: 添加 V5 变体(旋转跟随)**

在 ImageTrailVariant3 类定义后插入:

```typescript
class ImageTrailVariant5 extends ImageTrailVariant1 {
  lastAngle = 0

  showNextImage() {
    const img = this.images[this.imgPosition]
    this.imgPosition = (this.imgPosition + 1) % this.images.length
    this.zIndexVal++

    const dx = this.mousePos.x - this.lastMousePos.x
    const dy = this.mousePos.y - this.lastMousePos.y
    const distance = Math.hypot(dx, dy)

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
```

- [ ] **Step 3: 修改 useImageTrail 函数支持变体选择**

替换 `useImageTrail` 函数的 `init` 实现:

```typescript
  const init = () => {
    if (!container.value || images.length === 0) return
    
    const VariantClass = variant === 3 ? ImageTrailVariant3 : variant === 5 ? ImageTrailVariant5 : ImageTrailVariant1
    instance = new VariantClass(container.value, threshold) as ImageTrailVariant1
  }
```

- [ ] **Step 4: 验证类型检查**

Run: `npm run build`
Expected: 编译通过

- [ ] **Step 5: Commit**

```bash
git add src/composables/useImageTrail.ts
git commit -m "feat(composables): add V3 fly-scatter and V5 rotation variants"
```

---

### Task 6: 创建 ImageTrail.vue 包装组件

**Files:**
- Create: `frontend/src/components/ImageTrail.vue`

**Interfaces:**
- Consumes: `useImageTrail` 组合式函数,props `images: string[]`, `variant: 1|3|5`, `threshold: number`
- Produces: Vue 组件渲染容器与图片元素,支持插槽兜底,尊重 prefers-reduced-motion

- [ ] **Step 1: 创建组件文件**

```vue
<template>
  <div ref="containerRef" class="image-trail-container" aria-hidden="true">
    <template v-if="images.length > 0">
      <div
        v-for="(src, idx) in images"
        :key="idx"
        class="image-trail-item"
      >
        <div class="image-trail-inner" :style="{ backgroundImage: `url(${src})` }" />
      </div>
    </template>
    <slot v-else />
  </div>
</template>

<script setup lang="ts">
import { ref, toRefs, onMounted } from 'vue'
import { useImageTrail } from '@/composables/useImageTrail'

interface Props {
  images: string[]
  variant?: 1 | 3 | 5
  threshold?: number
}

const props = withDefaults(defineProps<Props>(), {
  variant: 1,
  threshold: 80
})

const { images, variant, threshold } = toRefs(props)
const containerRef = ref<HTMLElement | null>(null)

onMounted(() => {
  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (prefersReducedMotion) return

  useImageTrail(containerRef, images.value, variant.value, threshold.value)
})
</script>

<style scoped>
.image-trail-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.image-trail-item {
  position: absolute;
  top: 0;
  left: 0;
  width: 96px;
  height: 120px;
  overflow: hidden;
  border-radius: 6px;
  pointer-events: none;
  opacity: 0;
  will-change: transform;
  box-shadow: 0 12px 28px rgb(23 53 36 / 22%);
}

.image-trail-inner {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  will-change: transform, filter;
}

@media (prefers-reduced-motion: reduce) {
  .image-trail-item {
    display: none;
  }
}
</style>
```

- [ ] **Step 2: 验证编译**

Run: `npm run build`
Expected: 编译通过,无错误

- [ ] **Step 3: Commit**

```bash
git add src/components/ImageTrail.vue
git commit -m "feat(components): add ImageTrail wrapper component"
```

---

### Task 7: 改造 HomeView Hero 区接入 ImageTrail

**Files:**
- Modify: `frontend/src/views/HomeView.vue:19-43` (intro-scene 部分)

**Interfaces:**
- Consumes: ImageTrail 组件,categoryTile 函数,现有 latestItems 数据
- Produces: Hero 区动画容器,删除旧拖尾代码,保留装饰元素

- [ ] **Step 1: 导入 ImageTrail 和 placeholder**

在 `<script setup>` 的 import 区域(第 124-126 行)添加:

```typescript
import ImageTrail from '@/components/ImageTrail.vue'
import { categoryTile } from '@/utils/placeholder'
```

- [ ] **Step 2: 添加变体切换状态与图片准备逻辑**

在 `keyword` 声明后(约 134 行)添加:

```typescript
const trailVariant = ref<1 | 3 | 5>(1)
const trailImages = computed(() => {
  const real = latestItems.value
    .flatMap(item => item.images || [])
    .filter(src => !src.includes('example.com'))
  const need = 6 - real.length
  if (need > 0) {
    const tiles = latestItems.value.slice(0, need).map(item => categoryTile(item.category))
    return [...real, ...tiles]
  }
  return real.slice(0, 6)
})
```

- [ ] **Step 3: 删除旧拖尾相关代码**

删除以下声明(约 135-137 行):
```typescript
const trailImages = ref<Array<{ id: number; src: string; x: number; y: number; rotation: number }>>([])
let lastTrailAt = 0
let nextTrailId = 0
```

删除以下函数(约 151-176 行):
```typescript
const onScenePointerMove = (event: PointerEvent) => { ... }
const removeTrail = (id: number) => { ... }
const clearTrail = () => { ... }
```

删除 `onBeforeUnmount(clearTrail)` 调用(约 193 行)

- [ ] **Step 4: 替换 intro-scene 模板**

替换模板中 `<div class="intro-scene" ...>` 部分(19-43 行)为:

```vue
      <div class="intro-scene">
        <div class="scene-controls">
          <button
            v-for="v in [1, 3, 5] as const"
            :key="v"
            :class="['variant-btn', { active: trailVariant === v }]"
            @click="trailVariant = v"
          >
            {{ v === 1 ? '经典' : v === 3 ? '飞散' : '旋转' }}
          </button>
        </div>
        <ImageTrail :images="trailImages" :variant="trailVariant">
          <div class="scene-note">校园里的每一条线索，都值得被认真对待。</div>
          <div class="scene-object object-card">
            <el-icon><Postcard /></el-icon>
            <span>校园卡</span>
          </div>
          <div class="scene-object object-key">
            <el-icon><Key /></el-icon>
          </div>
          <div class="scene-object object-book">
            <el-icon><Reading /></el-icon>
            <span>笔记本</span>
          </div>
          <div class="scene-caption">拾到一份善意 · 归还一份安心</div>
        </ImageTrail>
      </div>
```

- [ ] **Step 5: 删除旧拖尾 CSS 动画**

删除样式中 `.image-trail` 类和 `@keyframes trail-out` 定义(220-221 行):

```css
.image-trail { ... }
@keyframes trail-out { ... }
```

- [ ] **Step 6: 添加变体切换按钮样式**

在 `.scene-caption` 样式后(约 219 行)添加:

```css
.scene-controls { position: absolute; z-index: 5; top: 20px; left: 20px; display: flex; gap: 8px; }
.variant-btn { padding: 6px 12px; border: 1px solid #83b39a; background: rgb(255 255 255 / 85%); color: #225940; font-size: 12px; cursor: pointer; transition: all .2s; }
.variant-btn:hover { background: #eef7f1; }
.variant-btn.active { background: #d8ecdf; border-color: #26745c; font-weight: 600; }
```

- [ ] **Step 7: 验证编译与功能**

Run: `npm run dev`
浏览器访问 `http://localhost:5173/home`,验证:
- Hero 区鼠标移动时图片拖尾出现
- 三个变体按钮切换有效
- 无图数据时显示 SVG 兜底

- [ ] **Step 8: Commit**

```bash
git add src/views/HomeView.vue
git commit -m "feat(home): replace CSS trail with GSAP ImageTrail component"
```

---

### Task 8: 改造 HomeView 线索区为瀑布流

**Files:**
- Modify: `frontend/src/views/HomeView.vue:70-92` (item-grid 部分)

**Interfaces:**
- Consumes: latestItems 数据,categoryTile 函数,现有 categoryTone
- Produces: CSS columns 瀑布流布局,卡片悬停浮起效果

- [ ] **Step 1: 替换 item-grid 模板为瀑布流**

替换模板中 `.item-grid` 部分(71-91 行)为:

```vue
      <div v-else class="item-masonry">
        <button
          v-for="item in latestItems"
          :key="item.id"
          class="found-card"
          @click="$router.push(`/items/${item.id}`)"
        >
          <div class="found-visual" :class="`visual-${categoryTone(item.category)}`">
            <el-image v-if="item.images?.[0] && !isPlaceholderImage(item.images[0])" :src="item.images[0]" fit="cover" lazy>
              <template #error>
                <img :src="categoryTile(item.category)" alt="" style="width: 100%; height: 100%; object-fit: cover;" />
              </template>
            </el-image>
            <img v-else :src="categoryTile(item.category)" alt="" style="width: 100%; height: 100%; object-fit: cover;" />
            <span class="visual-category">{{ item.category }}</span>
          </div>
          <div class="found-card-copy">
            <span class="found-title">{{ item.title }}</span>
            <span class="found-location"><el-icon><Location /></el-icon>{{ item.foundLocation || '地点未填写' }}</span>
          </div>
        </button>
      </div>
```

- [ ] **Step 2: 替换 item-grid 样式为瀑布流样式**

替换样式中 `.item-grid` 开头部分(233-239 行)为:

```css
.item-masonry { column-count: 3; column-gap: 17px; }
.found-card { display: block; break-inside: avoid; margin-bottom: 17px; overflow: hidden; padding: 0; border: 0; background: #fff; color: inherit; text-align: left; cursor: pointer; border-radius: var(--lf-radius); box-shadow: var(--lf-shadow-1); transition: transform .18s ease, box-shadow .18s ease; }
.found-card:hover { transform: translateY(-4px); box-shadow: var(--lf-shadow-2); }
.found-card:focus-visible { outline: 3px solid #26745c; outline-offset: 3px; }
.found-visual { position: relative; display: grid; place-items: center; aspect-ratio: auto; overflow: hidden; }
.found-visual img, .found-visual .el-image, .found-visual :deep(.el-image__inner) { width: 100%; height: 100%; object-fit: cover; }
```

- [ ] **Step 3: 删除旧的交替高度 class 样式**

删除以下样式(238-239 行):
```css
.found-card-1 .found-visual { height: 228px; }
.found-card-2 .found-visual { height: 166px; }
```

删除 `.found-visual` 中的固定高度 `height: 188px;`

- [ ] **Step 4: 更新响应式断点瀑布流列数**

在媒体查询 `@media (max-width: 760px)` 中(约 273 行)替换 `.item-grid` 为:

```css
  .item-masonry { column-count: 2; column-gap: 12px; }
  .found-card { margin-bottom: 12px; }
```

在媒体查询 `@media (max-width: 420px)` 中(约 282 行)添加:

```css
  .item-masonry { column-count: 1; }
```

- [ ] **Step 5: 验证浏览器效果**

Run: `npm run dev`
浏览器测试:
- 桌面端 3 列瀑布流,卡片悬停浮起
- 760px 以下 2 列,420px 以下 1 列
- 无图卡片显示 SVG 瓦片

- [ ] **Step 6: Commit**

```bash
git add src/views/HomeView.vue
git commit -m "feat(home): convert item grid to CSS columns masonry layout"
```

---

### Task 9: 改造 ItemListView 为瀑布流卡片墙

**Files:**
- Modify: `frontend/src/views/ItemListView.vue:12-87` (list-panel 部分)

**Interfaces:**
- Consumes: items 数据,ITEM_STATUS_MAP,categoryTile
- Produces: 瀑布流卡片墙替换 el-table,筛选栏 pill 化

- [ ] **Step 1: 导入 categoryTile**

在 `<script setup>` import 区域(约 92-96 行)添加:

```typescript
import { categoryTile } from '@/utils/placeholder'
```

添加辅助函数:

```typescript
const isPlaceholderImage = (src: string) => src.includes('example.com')
const categoryTone = (category: string) => {
  if (category.includes('证件') || category.includes('卡')) return 'coral'
  if (category.includes('电子')) return 'blue'
  if (category.includes('书')) return 'yellow'
  return 'green'
}
```

- [ ] **Step 2: 替换 el-card 为 pill 风格筛选栏**

替换模板 `<el-card class="list-panel">` 部分(12-87 行)为:

```vue
    <div class="filter-bar">
      <el-form :inline="true" :model="queryForm" @submit.prevent="handleSearch">
        <el-form-item label="关键词">
          <el-input
            v-model="queryForm.keyword"
            placeholder="物品标题"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryForm.category" placeholder="全部分类" clearable style="width: 140px">
            <el-option v-for="c in ITEM_CATEGORIES" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="投放点">
          <el-select v-model="queryForm.dropPointId" placeholder="全部站点" clearable style="width: 160px">
            <el-option v-for="p in dropPoints" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="userStore.isLoggedIn" label="状态">
          <el-select v-model="queryForm.itemStatus" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="公开待认领" :value="1" />
            <el-option label="认领中" :value="2" />
            <el-option label="已取件" :value="3" />
            <el-option label="已过期" :value="5" />
            <el-option label="待交物" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-loading="loading" class="content-area">
      <el-empty v-if="items.length === 0" description="暂无物品" :image-size="72" />
      <div v-else class="item-masonry">
        <button
          v-for="item in items"
          :key="item.id"
          class="item-card"
          @click="$router.push(`/items/${item.id}`)"
        >
          <div class="item-visual" :class="`visual-${categoryTone(item.category)}`">
            <el-image v-if="item.images?.[0] && !isPlaceholderImage(item.images[0])" :src="item.images[0]" fit="cover" lazy>
              <template #error>
                <img :src="categoryTile(item.category)" alt="" style="width: 100%; height: 100%; object-fit: cover;" />
              </template>
            </el-image>
            <img v-else :src="categoryTile(item.category)" alt="" style="width: 100%; height: 100%; object-fit: cover;" />
            <span class="visual-category">{{ item.category }}</span>
          </div>
          <div class="item-card-body">
            <span class="item-title">{{ item.title }}</span>
            <span class="item-meta">
              <el-icon><Location /></el-icon>{{ item.foundLocation || '地点未填写' }}
            </span>
            <span class="item-meta">
              <el-icon><Clock /></el-icon>{{ formatTime(item.foundTime) }}
            </span>
            <span class="item-status-row">
              <el-tag size="small" :type="ITEM_STATUS_MAP[item.itemStatus]?.type || 'info'">
                {{ ITEM_STATUS_MAP[item.itemStatus]?.text || '未知' }}
              </el-tag>
              <span v-if="item.dropPointName" class="item-point">{{ item.dropPointName }}</span>
            </span>
          </div>
        </button>
      </div>

      <div v-if="items.length > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchItems"
          @current-change="fetchItems"
        />
      </div>
    </div>
```

- [ ] **Step 3: 替换样式**

替换 `<style scoped>` 中的 `.list-panel` 及其内部样式(182-189 行)为:

```css
.filter-bar { padding: var(--lf-space-4); margin-bottom: var(--lf-space-4); background: #fff; border-radius: var(--lf-radius); box-shadow: var(--lf-shadow-1); }
.filter-bar :deep(.el-form) { display: flex; flex-wrap: wrap; align-items: center; gap: 0 8px; }
.filter-bar :deep(.el-form-item) { margin-bottom: 12px; }

.content-area { padding: var(--lf-space-5); background: #fff; border-radius: var(--lf-radius); box-shadow: var(--lf-shadow-1); }
.item-masonry { column-count: 3; column-gap: 18px; }
.item-card { display: block; break-inside: avoid; margin-bottom: 18px; overflow: hidden; padding: 0; border: 0; background: #fff; border-radius: var(--lf-radius); box-shadow: 0 0 0 1px var(--lf-border); text-align: left; cursor: pointer; transition: transform .18s ease, box-shadow .18s ease; }
.item-card:hover { transform: translateY(-3px); box-shadow: var(--lf-shadow-2); }
.item-visual { position: relative; aspect-ratio: 4/3; overflow: hidden; }
.visual-green { background: #81a78e; }
.visual-coral { background: #d88770; }
.visual-blue { background: #7899a5; }
.visual-yellow { background: #c3a668; }
.item-visual img, .item-visual .el-image, .item-visual :deep(.el-image__inner) { width: 100%; height: 100%; object-fit: cover; }
.visual-category { position: absolute; right: 10px; bottom: 10px; padding: 4px 8px; background: rgb(255 255 255 / 90%); color: #43564b; font-size: 11px; border-radius: 3px; }
.item-card-body { display: flex; flex-direction: column; gap: 8px; padding: 14px; }
.item-title { color: #2c3933; font-size: 15px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-meta { display: flex; align-items: center; gap: 5px; color: #85928b; font-size: 12px; }
.item-status-row { display: flex; align-items: center; gap: 8px; }
.item-point { color: #7d8a83; font-size: 12px; }

.pagination-wrapper { display: flex; justify-content: center; margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--lf-border); }
```

- [ ] **Step 4: 更新响应式断点**

在 `@media (max-width: 640px)` 中(约 192 行)替换为:

```css
@media (max-width: 760px) {
  .item-masonry { column-count: 2; column-gap: 12px; }
  .item-card { margin-bottom: 12px; }
}

@media (max-width: 640px) {
  .page-heading { align-items: flex-start; }
  .page-heading h1 { font-size: 23px; }
  .filter-bar { padding: 14px; }
  .content-area { padding: var(--lf-space-4); }
  .pagination-wrapper { justify-content: flex-start; overflow-x: auto; }
}

@media (max-width: 420px) {
  .item-masonry { column-count: 1; }
}
```

- [ ] **Step 5: 验证浏览器效果**

Run: `npm run dev`
浏览器访问 `/items`,验证:
- 筛选栏 pill 风格,功能正常
- 3/2/1 列瀑布流断点
- 卡片显示完整信息,悬停浮起
- 分页正常

- [ ] **Step 6: Commit**

```bash
git add src/views/ItemListView.vue
git commit -m "feat(items): convert table to masonry card layout with pill filters"
```

---

### Task 10: 改造 ItemDetailView 大图优先布局

**Files:**
- Modify: `frontend/src/views/ItemDetailView.vue:3-95` (el-card 内容部分)

**Interfaces:**
- Consumes: item 数据,categoryTile
- Produces: 左侧大图区+右侧信息栏布局

- [ ] **Step 1: 导入 categoryTile**

在 `<script setup>` import 区域(约 161-168 行)添加:

```typescript
import { categoryTile } from '@/utils/placeholder'
```

添加辅助函数(约 192 行):

```typescript
const isPlaceholderImage = (src: string) => src?.includes('example.com')
const categoryTone = (category: string) => {
  if (category?.includes('证件') || category?.includes('卡')) return 'coral'
  if (category?.includes('电子')) return 'blue'
  if (category?.includes('书')) return 'yellow'
  return 'green'
}
```

- [ ] **Step 2: 替换 el-row/el-col 为大图布局**

替换模板 `<el-row :gutter="24">` 部分(17-94 行)为:

```vue
      <div class="detail-layout">
        <div class="detail-main">
          <div class="main-image" :class="`visual-${item.images?.[0] ? 'real' : categoryTone(item.category)}`">
            <template v-if="item.images && item.images.length > 0 && !isPlaceholderImage(item.images[0])">
              <el-carousel
                v-if="item.images.length > 1"
                height="100%"
                indicator-position="outside"
                arrow="always"
              >
                <el-carousel-item v-for="(img, idx) in item.images" :key="idx">
                  <el-image :src="img" fit="contain" style="width: 100%; height: 100%" :preview-src-list="item.images.filter(i => !isPlaceholderImage(i))" />
                </el-carousel-item>
              </el-carousel>
              <el-image v-else :src="item.images[0]" fit="contain" style="width: 100%; height: 100%" :preview-src-list="item.images" />
            </template>
            <img v-else :src="categoryTile(item.category)" alt="" class="fallback-tile" />
          </div>
        </div>

        <div class="detail-sidebar">
          <h2 class="item-title-large">{{ item.title }}</h2>
          <div class="info-grid">
            <div class="info-row">
              <span class="info-label">分类</span>
              <el-tag size="small">{{ item.category }}</el-tag>
            </div>
            <div class="info-row">
              <span class="info-label">易腐品</span>
              <span>{{ item.perishable === 1 ? '是' : '否' }}</span>
            </div>
            <div class="info-row full">
              <span class="info-label">拾取地点</span>
              <span><el-icon><Location /></el-icon>{{ item.foundLocation }}</span>
            </div>
            <div class="info-row full">
              <span class="info-label">拾取时间</span>
              <span><el-icon><Clock /></el-icon>{{ formatTime(item.foundTime) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">发布人</span>
              <span>
                {{ item.founderName || '匿名' }}
                <el-tag v-if="item.newUser" size="small" type="warning" style="margin-left: 6px">新手发布</el-tag>
              </span>
            </div>
            <div class="info-row">
              <span class="info-label">发布时间</span>
              <span>{{ formatTime(item.publishedAt) }}</span>
            </div>
            <div class="info-row full">
              <span class="info-label">存放投放点</span>
              <span>{{ item.dropPointName || (item.itemStatus === 6 ? '待发布人交物' : '未指定') }}</span>
            </div>
            <div v-if="item.description" class="info-row full">
              <span class="info-label">物品描述</span>
              <p class="item-description">{{ item.description }}</p>
            </div>
          </div>

          <div class="action-bar" v-if="userStore.isLoggedIn">
            <template v-if="isMyItem">
              <template v-if="item.itemStatus === 6">
                <el-select v-model="selectedDropPointId" placeholder="选择投放点" style="width: 100%; margin-bottom: 8px">
                  <el-option v-for="p in dropPoints" :key="p.id" :label="p.name" :value="p.id" />
                </el-select>
                <el-button type="primary" :loading="acting" @click="handleHandIn" style="width: 100%">已投放</el-button>
              </template>
              <el-button
                v-if="[1, 6].includes(item.itemStatus)"
                type="danger"
                plain
                :loading="acting"
                @click="handleInvalidate"
                style="width: 100%"
              >
                作废
              </el-button>
            </template>
            <template v-else-if="item.itemStatus === 1">
              <el-button type="primary" @click="claimDialogVisible = true" style="width: 100%">这是我的,申请认领</el-button>
            </template>
            <el-button
              v-if="(isMyItem && item.itemStatus === 3) || (userStore.isAdmin && item.itemStatus === 5)"
              type="info"
              plain
              :loading="acting"
              @click="handleArchive"
              style="width: 100%"
            >
              {{ item.itemStatus === 5 ? '处置归档' : '归档' }}
            </el-button>
          </div>
          <el-alert
            v-else-if="item.itemStatus === 1"
            title="登录后可申请认领"
            type="info"
            :closable="false"
          />
        </div>
      </div>
```

- [ ] **Step 3: 替换样式**

替换 `<style scoped>` 中的 `.detail-header` 后所有样式(359-386 行)为:

```css
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-layout { display: grid; grid-template-columns: 1.2fr 1fr; gap: var(--lf-space-6); }
.detail-main { display: flex; flex-direction: column; }
.main-image { position: relative; width: 100%; min-height: 420px; border-radius: var(--lf-radius-lg); overflow: hidden; }
.main-image.visual-green { background: #81a78e; }
.main-image.visual-coral { background: #d88770; }
.main-image.visual-blue { background: #7899a5; }
.main-image.visual-yellow { background: #c3a668; }
.main-image.visual-real { background: #f4f7f5; }
.fallback-tile { width: 100%; height: 100%; object-fit: cover; }

.detail-sidebar { display: flex; flex-direction: column; gap: var(--lf-space-4); }
.item-title-large { margin: 0; color: #26332f; font-size: 26px; font-weight: 650; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--lf-space-3); padding: var(--lf-space-4); background: #f9faf9; border-radius: var(--lf-radius); }
.info-row { display: flex; flex-direction: column; gap: 6px; }
.info-row.full { grid-column: 1 / -1; }
.info-label { color: #7d8a83; font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
.info-row > span, .info-row > p { display: flex; align-items: center; gap: 6px; color: #26332f; font-size: 14px; }
.item-description { margin: 0; line-height: 1.6; white-space: pre-wrap; }

.action-bar { display: flex; flex-direction: column; gap: 10px; }

@media (max-width: 860px) {
  .detail-layout { grid-template-columns: 1fr; }
  .main-image { min-height: 320px; }
}
```

- [ ] **Step 4: 验证浏览器效果**

Run: `npm run dev`
浏览器访问任意物品详情页,验证:
- 左侧大图圆角,右侧信息栏清晰
- 无图时显示 SVG 瓦片
- 响应式 860px 以下堆叠
- 认领按钮功能正常

- [ ] **Step 5: Commit**

```bash
git add src/views/ItemDetailView.vue
git commit -m "feat(detail): large image layout with styled info sidebar"
```

---

### Task 11: 导航栏毛玻璃效果

**Files:**
- Modify: `frontend/src/views/LayoutView.vue:98-203` (header 样式部分)

**Interfaces:**
- Consumes: 现有导航栏结构
- Produces: 半透明毛玻璃背景,logo 渐变,下拉菜单圆角阴影

- [ ] **Step 1: 替换 .layout-header 样式**

替换 `.layout-header` 样式(98-105 行)为:

```css
.layout-header {
  position: sticky;
  top: 0;
  z-index: 20;
  height: auto;
  background: rgb(255 255 255 / 78%);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgb(228 235 230 / 60%);
  padding: 0;
}
```

- [ ] **Step 2: 升级 logo 样式**

替换 `.logo .el-icon` 样式(128-134 行)为:

```css
.logo .el-icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  background: linear-gradient(135deg, #e9f2ec 0%, #d8ecdf 100%);
  border: 1px solid #c4d9ca;
  border-radius: var(--lf-radius-sm);
  font-size: 19px;
  color: #26745c;
}
```

- [ ] **Step 3: 优化下拉菜单圆角**

在 `.user-info .el-icon` 样式后(约 183 行)添加:

```css
:deep(.el-dropdown-menu) {
  border-radius: var(--lf-radius);
  box-shadow: var(--lf-shadow-3);
  border-color: rgb(228 235 230 / 80%);
}

:deep(.el-dropdown-menu__item) {
  font-size: 13px;
}
```

- [ ] **Step 4: 验证浏览器效果**

Run: `npm run dev`
验证:
- 导航栏半透明毛玻璃,滚动时背后内容模糊
- logo 绿色渐变圆角
- 下拉菜单圆角阴影

- [ ] **Step 5: Commit**

```bash
git add src/views/LayoutView.vue
git commit -m "feat(nav): frosted glass header with gradient logo"
```

---

### Task 12: 整体验证与回归测试

**Files:**
- None(verification only)

**Interfaces:**
- Consumes: 所有已改造页面与未改造页面
- Produces: 类型检查通过,浏览器功能验证通过,回归零异常

- [ ] **Step 1: 运行类型检查与构建**

Run: `npm run build`
Expected: `vue-tsc` 零错误,Vite 构建成功

- [ ] **Step 2: 启动 dev server 全功能测试**

Run: `npm run dev`

浏览器测试清单:

**首页(/home):**
- [ ] Hero 区 ImageTrail 三种变体切换流畅
- [ ] 鼠标移动触发图片拖尾,触摸模拟正常
- [ ] 线索瀑布流 3/2/1 列断点
- [ ] 无图数据显示 SVG 兜底瓦片
- [ ] prefers-reduced-motion 下动画禁用(浏览器 DevTools Rendering 选项)

**失物招领列表(/items):**
- [ ] 筛选栏 pill 风格,关键词/分类/投放点/状态筛选功能正常
- [ ] 瀑布流卡片墙 3/2/1 列断点
- [ ] 卡片悬停浮起效果
- [ ] 分页功能正常,页码/每页条数切换
- [ ] 点击卡片跳转详情

**物品详情(/items/:id):**
- [ ] 左侧大图轮播(多图)或单图预览
- [ ] 右侧信息栏清晰分组
- [ ] 无图时显示 SVG 瓦片
- [ ] 860px 以下布局堆叠
- [ ] 认领/作废/归档按钮功能测试

**导航栏(全局):**
- [ ] 毛玻璃效果,滚动时背景模糊
- [ ] logo 渐变与下拉菜单圆角
- [ ] 登录/未登录状态切换

**回归测试(未改造页面):**
- [ ] 登录页(/login)样式无异常
- [ ] 注册页(/register)样式无异常
- [ ] 发布页(/items/publish)样式无异常
- [ ] 个人中心(/profile)样式无异常

- [ ] **Step 3: 验证通过后最终 commit**

```bash
git add -A
git commit -m "chore: final verification passed - all features working"
```

Expected: 所有测试项通过,回归零异常

---

## 实施完成

计划执行完成后,验证清单:
- [x] `vue-tsc && vite build` 零错误
- [x] 首页 GSAP ImageTrail 三种变体流畅运行
- [x] 首页与列表页瀑布流响应式断点正确
- [x] 详情页大图布局与 SVG 兜底正常
- [x] 导航栏毛玻璃效果呈现
- [x] 未改造页面零回归
- [x] prefers-reduced-motion 下动画禁用

后续可选增强(不在本次范围):
- ImageTrail 增加更多变体(V2/V4/V6)
- 瀑布流列表加载更多(替换分页)
- 其余 11 个页面渐进迁移设计令牌
