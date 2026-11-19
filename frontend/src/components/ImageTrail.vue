<!--
  ImageTrail.vue —— 图片拖尾动画组件
  功能：在 Hero 区渲染一组图片，鼠标划过时图片会沿鼠标轨迹依次出现、飞散、消失
        （视觉装饰效果，无业务功能）。
  在系统中的作用：首页 HomeView 的氛围元素，增强页面活力与吸引力。
  实现思路：
    - 真正的动画逻辑在 useImageTrail.ts 组合式函数中，本组件负责“组件化封装”；
    - 通过一个内部定义的 TrailEngine 子组件持有容器 ref 并调用组合式函数；
    - 无图片时降级为普通容器（显示默认插槽内容）。
-->
<template>
  <!-- 有图片时渲染动画引擎；:key 包含所有可变参数，
       任何参数变化都会强制重建引擎，确保动画状态与最新参数一致 -->
  <TrailEngine
    v-if="images.length > 0"
    :key="engineKey"
    :images="images"
    :variant="variant"
    :threshold="threshold"
  />
  <!-- 无图片时：仅渲染一个隐藏容器展示默认插槽内容 -->
  <div v-else class="image-trail-container" aria-hidden="true">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, ref, toRefs } from 'vue'
import type { PropType } from 'vue'
import { useImageTrail } from '@/composables/useImageTrail'

// 组件 Props：
//   images    —— 参与拖尾动画的图片 URL 列表
//   variant   —— 动画变体（1=基础跟随，3=弹出飘散，5=旋转飞出）
//   threshold —— 触发新图的最小鼠标移动距离（px）
interface Props {
  images: string[]
  variant?: 1 | 3 | 5
  threshold?: number
}

const props = withDefaults(defineProps<Props>(), {
  variant: 1,
  threshold: 80
})

// 转为 ref 以便在模板与计算属性中保持响应性
const { images, variant, threshold } = toRefs(props)

// useImageTrail captures variant/threshold/images as plain values and only
// watches container + images.length internally, so prop changes never reach it.
// Remounting the engine (keyed on all three) re-invokes it cleanly: unmount
// runs the composable's own cleanup, and stale listeners die with the old DOM.
// engineKey 由三个参数拼成：任一变化 → Vue 销毁旧引擎重新挂载新引擎，
// 旧引擎卸载时会自动执行组合式函数里注册的清理逻辑（停帧、解绑事件）
const engineKey = computed(
  () => `${variant.value}|${threshold.value}|${images.value.join('|')}`
)

// 内部引擎组件：负责创建容器 DOM 与图片节点，并挂载动画组合式函数。
// 用 h() 渲染函数而非模板，是因为图片节点需要动态生成且与动画类名严格对应。
const TrailEngine = defineComponent({
  name: 'ImageTrailEngine',
  props: {
    images: { type: Array as PropType<string[]>, required: true },
    variant: { type: Number as PropType<1 | 3 | 5>, required: true },
    threshold: { type: Number, required: true }
  },
  setup(engineProps) {
    // 容器 ref：组合式函数需要真实的 DOM 元素来绑定事件
    const containerRef = ref<HTMLElement | null>(null)

    // 无障碍/节能考虑：用户系统开启“减少动态效果”时，完全跳过动画初始化
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (!prefersReducedMotion) {
      useImageTrail(containerRef, engineProps.images, engineProps.variant, engineProps.threshold)
    }

    // 渲染函数：容器 div 内平铺所有图片节点（外层 item 负责位移动画，内层 inner 负责背景图缩放）
    return () =>
      h(
        'div',
        { ref: containerRef, class: 'image-trail-container', 'aria-hidden': 'true' },
        engineProps.images.map((src, idx) =>
          h('div', { key: idx, class: 'image-trail-item' }, [
            h('div', {
              class: 'image-trail-inner',
              style: { backgroundImage: `url(${src})` }
            })
          ])
        )
      )
  }
})
</script>

<style scoped>
/* 动画容器：铺满父元素并裁剪溢出内容，防止图片飞出可视区 */
.image-trail-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

/* 单张拖尾图片：绝对定位于左上角（位置由 GSAP 的 x/y 控制），
   初始透明（opacity: 0），pointer-events: none 保证不阻挡鼠标事件，
   will-change 提示浏览器为 transform 创建独立合成层，提升动画性能 */
.image-trail-container :deep(.image-trail-item) {
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

/* 内层图片：用 background-image 呈现（cover 裁剪填充），可独立做缩放动画 */
.image-trail-container :deep(.image-trail-inner) {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  will-change: transform, filter;
}

/* 与 JS 端的 prefersReducedMotion 检查双保险：CSS 层面也彻底隐藏动画元素 */
@media (prefers-reduced-motion: reduce) {
  .image-trail-container :deep(.image-trail-item) {
    display: none;
  }
}
</style>
