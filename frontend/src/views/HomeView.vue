<template>
  <div class="home-page">
    <section class="home-intro">
      <div class="intro-copy">
        <p class="intro-kicker">校园失物招领 · 诚信同行</p>
        <h1>让每一件遗失物，<br />都能回到熟悉的人身边。</h1>
        <p class="intro-description">发布线索、查找失物，也记录每一次善意的归还。</p>
        <form class="home-search" @submit.prevent="searchItems">
          <el-icon><Search /></el-icon>
          <input v-model="keyword" aria-label="搜索物品或地点" placeholder="试试搜索物品名称、地点" />
          <el-button native-type="submit" type="primary">开始寻找</el-button>
        </form>
        <div class="intro-actions">
          <el-button type="primary" @click="$router.push('/items')">浏览失物招领</el-button>
          <el-button plain @click="$router.push('/notices')">查看寻物启事</el-button>
        </div>
      </div>

      <div class="intro-scene" @pointermove="onScenePointerMove" @pointerleave="clearTrail">
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
        <img
          v-for="trail in trailImages"
          :key="trail.id"
          class="image-trail"
          :src="trail.src"
          :style="{ left: `${trail.x}px`, top: `${trail.y}px`, transform: `translate(-50%, -50%) rotate(${trail.rotation}deg)` }"
          alt=""
          @animationend="removeTrail(trail.id)"
          @error="removeTrail(trail.id)"
        />
      </div>
    </section>

    <section class="home-metrics" aria-label="平台动态">
      <div class="metric-item">
        <span class="metric-value">{{ publicCount }}</span>
        <span class="metric-label">公开招领线索</span>
      </div>
      <div class="metric-item">
        <span class="metric-value">{{ noticeCount }}</span>
        <span class="metric-label">进行中的寻物启事</span>
      </div>
      <div class="metric-message">
        <el-icon><CircleCheck /></el-icon>
        <span>每一次诚信归还，都会成为校园里新的信任。</span>
      </div>
    </section>

    <section class="feed-section">
      <div class="section-heading">
        <div>
          <h2>刚刚出现的线索</h2>
          <p>看看最近有哪些物品正在等待主人</p>
        </div>
        <el-button link type="primary" @click="$router.push('/items')">全部招领 <el-icon><ArrowRight /></el-icon></el-button>
      </div>

      <el-empty v-if="latestItems.length === 0" description="还没有新的招领线索" :image-size="72" />
      <div v-else class="item-grid">
        <button
          v-for="(item, index) in latestItems"
          :key="item.id"
          class="found-card"
          :class="`found-card-${index % 3}`"
          @click="$router.push(`/items/${item.id}`)"
        >
          <div class="found-visual" :class="`visual-${categoryTone(item.category)}`">
            <el-image v-if="item.images?.[0] && !isPlaceholderImage(item.images[0])" :src="item.images[0]" fit="cover" lazy>
              <template #error><div class="visual-fallback"><el-icon><Box /></el-icon></div></template>
            </el-image>
            <div v-else class="visual-fallback"><el-icon><Box /></el-icon></div>
            <span class="visual-category">{{ item.category }}</span>
          </div>
          <div class="found-card-copy">
            <span class="found-title">{{ item.title }}</span>
            <span class="found-location"><el-icon><Location /></el-icon>{{ item.foundLocation || '地点未填写' }}</span>
          </div>
        </button>
      </div>
    </section>

    <section class="feed-section notice-section">
      <div class="section-heading">
        <div>
          <h2>正在寻找的物品</h2>
          <p>也许你的一条线索，就能帮上忙</p>
        </div>
        <el-button link type="primary" @click="$router.push('/notices')">全部启事 <el-icon><ArrowRight /></el-icon></el-button>
      </div>
      <el-empty v-if="latestNotices.length === 0" description="暂时没有进行中的寻物启事" :image-size="72" />
      <div v-else class="notice-list">
        <button
          v-for="notice in latestNotices"
          :key="notice.id"
          class="notice-row"
          @click="$router.push(`/notices/${notice.id}`)"
        >
          <span class="notice-symbol"><el-icon><Search /></el-icon></span>
          <span class="notice-copy">
            <strong>{{ notice.title }}</strong>
            <span>{{ notice.lostLocation || '地点未填写' }} · {{ notice.category }}</span>
          </span>
          <el-tag size="small" type="warning">寻找中</el-tag>
          <el-icon class="notice-arrow"><ArrowRight /></el-icon>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { foundItemApi, lostNoticeApi } from '@/api'
import type { FoundItem, LostNotice } from '@/types'

const router = useRouter()
const publicCount = ref(0)
const noticeCount = ref(0)
const latestItems = ref<FoundItem[]>([])
const latestNotices = ref<LostNotice[]>([])
const keyword = ref('')
const trailImages = ref<Array<{ id: number; src: string; x: number; y: number; rotation: number }>>([])
let lastTrailAt = 0
let nextTrailId = 0

const searchItems = () => {
  router.push({ path: '/items', query: keyword.value.trim() ? { keyword: keyword.value.trim() } : {} })
}

const isPlaceholderImage = (src: string) => src.includes('example.com')
const categoryTone = (category: string) => {
  if (category.includes('证件') || category.includes('卡')) return 'coral'
  if (category.includes('电子')) return 'blue'
  if (category.includes('书')) return 'yellow'
  return 'green'
}

const onScenePointerMove = (event: PointerEvent) => {
  if (event.pointerType !== 'mouse' || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  const now = Date.now()
  if (now - lastTrailAt < 150) return
  const sources = latestItems.value.flatMap((item) => item.images || []).filter((src) => !isPlaceholderImage(src))
  if (!sources.length) return
  lastTrailAt = now
  const bounds = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const id = nextTrailId++
  trailImages.value.push({
    id,
    src: sources[id % sources.length],
    x: event.clientX - bounds.left,
    y: event.clientY - bounds.top,
    rotation: (id % 2 ? 1 : -1) * (4 + (id % 4) * 2)
  })
  if (trailImages.value.length > 5) trailImages.value.shift()
}

const removeTrail = (id: number) => {
  trailImages.value = trailImages.value.filter((image) => image.id !== id)
}

const clearTrail = () => {
  trailImages.value = []
}

onMounted(async () => {
  const [itemsRes, noticesRes] = await Promise.allSettled([
    foundItemApi.list({ itemStatus: 1, page: 1, size: 6 }),
    lostNoticeApi.list({ status: 1, page: 1, size: 4 })
  ])
  if (itemsRes.status === 'fulfilled') {
    latestItems.value = itemsRes.value.data.records
    publicCount.value = Number(itemsRes.value.data.total)
  }
  if (noticesRes.status === 'fulfilled') {
    latestNotices.value = noticesRes.value.data.records
    noticeCount.value = Number(noticesRes.value.data.total)
  }
})

onBeforeUnmount(clearTrail)
</script>

<style scoped>
.home-page { max-width: 1240px; margin: 0 auto; }
.home-intro { display: grid; grid-template-columns: minmax(0, 1.05fr) minmax(320px, .95fr); min-height: 390px; overflow: hidden; background: #23664f; color: #fff; }
.intro-copy { padding: 48px clamp(24px, 5vw, 68px); align-self: center; }
.intro-kicker { margin-bottom: 18px; color: #b7d9c8; font-size: 13px; }
.intro-copy h1 { font-size: clamp(30px, 3vw, 42px); line-height: 1.35; font-weight: 650; }
.intro-description { margin-top: 14px; color: #d1e5da; font-size: 15px; line-height: 1.8; }
.home-search { display: flex; align-items: center; gap: 12px; max-width: 510px; height: 54px; margin-top: 27px; padding: 5px 6px 5px 16px; background: #fff; color: #62736b; }
.home-search input { flex: 1; min-width: 0; height: 100%; border: 0; outline: 0; color: #26332f; font: inherit; }
.home-search input::placeholder { color: #9aa8a1; }
.home-search .el-button { height: 42px; }
.intro-actions { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 15px; }
.intro-actions .el-button--primary { --el-button-bg-color: #d8ecdf; --el-button-border-color: #d8ecdf; --el-button-text-color: #225940; --el-button-hover-bg-color: #eef7f1; --el-button-hover-border-color: #eef7f1; --el-button-hover-text-color: #225940; }
.intro-actions .el-button.is-plain { color: #fff; background: transparent; border-color: #83b39a; }
.intro-scene { position: relative; min-height: 390px; overflow: hidden; background: #dcebe1; color: #234b39; isolation: isolate; }
.scene-note { position: absolute; z-index: 1; top: 25px; right: 25px; max-width: 170px; color: #607a6a; font-size: 12px; line-height: 1.7; }
.scene-object { position: absolute; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; box-shadow: 0 16px 30px rgb(37 73 53 / 13%); }
.scene-object .el-icon { font-size: 44px; }
.scene-object span { font-size: 13px; }
.object-card { top: 82px; left: 18%; width: 146px; height: 186px; transform: rotate(-8deg); background: #f7f2e7; color: #39805e; }
.object-card .el-icon { font-size: 56px; }
.object-key { top: 125px; right: 16%; width: 94px; height: 94px; transform: rotate(10deg); background: #d7b976; color: #fffaf0; }
.object-book { bottom: 62px; left: 43%; width: 156px; height: 120px; transform: rotate(5deg); background: #d58568; color: #fff7ef; }
.scene-caption { position: absolute; right: 24px; bottom: 20px; color: #64816e; font-size: 12px; }
.image-trail { position: absolute; z-index: 4; width: 96px; height: 120px; object-fit: cover; pointer-events: none; animation: trail-out .72s ease-out forwards; box-shadow: 0 12px 28px rgb(23 53 36 / 22%); }
@keyframes trail-out { 0% { opacity: 0; scale: .78; } 20% { opacity: 1; scale: 1; } 100% { opacity: 0; scale: .94; } }
.home-metrics { display: flex; align-items: center; gap: 44px; padding: 22px 28px; background: #fff; border-bottom: 1px solid #e6ece8; }
.metric-item { display: flex; flex-direction: column; gap: 3px; min-width: 140px; }
.metric-value { color: #26745c; font-size: 24px; font-weight: 650; font-variant-numeric: tabular-nums; }
.metric-label { color: #78877f; font-size: 12px; }
.metric-message { display: flex; align-items: center; gap: 9px; margin-left: auto; color: #5c7065; font-size: 13px; }
.metric-message .el-icon { color: #39805e; font-size: 18px; }
.feed-section { padding: 38px 0 8px; }
.section-heading { display: flex; align-items: end; justify-content: space-between; margin-bottom: 18px; }
.section-heading h2 { color: #26332f; font-size: 23px; font-weight: 650; }
.section-heading p { margin-top: 5px; color: #84918b; font-size: 13px; }
.section-heading .el-button { gap: 4px; }
.item-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 17px; align-items: start; }
.found-card { display: block; overflow: hidden; padding: 0; border: 0; background: #fff; color: inherit; text-align: left; cursor: pointer; transition: transform .18s ease, box-shadow .18s ease; }
.found-card:hover { transform: translateY(-3px); box-shadow: 0 10px 24px rgb(33 64 48 / 10%); }
.found-card:focus-visible { outline: 3px solid #26745c; outline-offset: 3px; }
.found-visual { position: relative; display: grid; place-items: center; height: 188px; overflow: hidden; }
.found-card-1 .found-visual { height: 228px; }
.found-card-2 .found-visual { height: 166px; }
.found-visual .el-image, .found-visual :deep(.el-image__inner) { width: 100%; height: 100%; }
.visual-fallback { display: grid; place-items: center; width: 100%; height: 100%; color: rgb(255 255 255 / 82%); }
.visual-fallback .el-icon { font-size: 50px; }
.visual-green { background: #81a78e; }
.visual-coral { background: #d88770; }
.visual-blue { background: #7899a5; }
.visual-yellow { background: #c3a668; }
.visual-category { position: absolute; right: 11px; bottom: 10px; padding: 5px 9px; background: rgb(255 255 255 / 90%); color: #43564b; font-size: 11px; }
.found-card-copy { display: flex; flex-direction: column; gap: 8px; padding: 13px 14px 15px; }
.found-title { overflow: hidden; color: #2c3933; font-size: 15px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.found-location { display: flex; align-items: center; gap: 5px; color: #85928b; font-size: 12px; }
.notice-section { padding-top: 40px; padding-bottom: 42px; }
.notice-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 28px; border-top: 1px solid #e4ebe6; }
.notice-row { display: flex; align-items: center; gap: 13px; min-width: 0; min-height: 78px; padding: 12px 2px; border: 0; border-bottom: 1px solid #e4ebe6; background: transparent; text-align: left; cursor: pointer; }
.notice-row:hover .notice-copy strong { color: #26745c; }
.notice-symbol { display: grid; flex: 0 0 38px; place-items: center; width: 38px; height: 38px; background: #edf4ef; color: #39805e; }
.notice-copy { display: flex; flex: 1; flex-direction: column; gap: 5px; min-width: 0; }
.notice-copy strong, .notice-copy span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.notice-copy strong { color: #34423b; font-size: 14px; font-weight: 550; }
.notice-copy span { color: #89958f; font-size: 12px; }
.notice-arrow { color: #9aa69f; }
@media (max-width: 760px) {
  .home-intro { grid-template-columns: 1fr; }
  .intro-copy { padding: 34px 22px 30px; }
  .intro-copy h1 { font-size: 31px; }
  .intro-scene { min-height: 250px; }
  .object-card { top: 42px; left: 16%; width: 110px; height: 145px; }
  .object-key { top: 70px; right: 15%; width: 70px; height: 70px; }
  .object-book { bottom: 35px; left: 43%; width: 120px; height: 90px; }
  .home-metrics { flex-wrap: wrap; gap: 18px 28px; padding: 20px; }
  .metric-item { min-width: 115px; }
  .metric-message { width: 100%; margin-left: 0; }
  .feed-section { padding-top: 30px; }
  .item-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
  .found-visual, .found-card-1 .found-visual, .found-card-2 .found-visual { height: 155px; }
  .notice-list { grid-template-columns: 1fr; }
}
@media (max-width: 420px) {
  .home-search { gap: 8px; padding-left: 11px; }
  .home-search .el-button { padding: 0 10px; }
  .intro-actions .el-button { flex: 1; margin-left: 0; }
  .section-heading h2 { font-size: 20px; }
  .found-visual, .found-card-1 .found-visual, .found-card-2 .found-visual { height: 130px; }
  .found-card-copy { padding: 11px 10px 13px; }
}
@media (prefers-reduced-motion: reduce) {
  .found-card { transition: none; }
  .image-trail { display: none; }
}
</style>
