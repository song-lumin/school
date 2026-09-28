<template>
  <TrailEngine
    v-if="images.length > 0"
    :key="engineKey"
    :images="images"
    :variant="variant"
    :threshold="threshold"
  />
  <div v-else class="image-trail-container" aria-hidden="true">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, ref, toRefs } from 'vue'
import type { PropType } from 'vue'
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

// useImageTrail captures variant/threshold/images as plain values and only
// watches container + images.length internally, so prop changes never reach it.
// Remounting the engine (keyed on all three) re-invokes it cleanly: unmount
// runs the composable's own cleanup, and stale listeners die with the old DOM.
const engineKey = computed(
  () => `${variant.value}|${threshold.value}|${images.value.join('|')}`
)

const TrailEngine = defineComponent({
  name: 'ImageTrailEngine',
  props: {
    images: { type: Array as PropType<string[]>, required: true },
    variant: { type: Number as PropType<1 | 3 | 5>, required: true },
    threshold: { type: Number, required: true }
  },
  setup(engineProps) {
    const containerRef = ref<HTMLElement | null>(null)

    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (!prefersReducedMotion) {
      useImageTrail(containerRef, engineProps.images, engineProps.variant, engineProps.threshold)
    }

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
.image-trail-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

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

.image-trail-container :deep(.image-trail-inner) {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  will-change: transform, filter;
}

@media (prefers-reduced-motion: reduce) {
  .image-trail-container :deep(.image-trail-item) {
    display: none;
  }
}
</style>
