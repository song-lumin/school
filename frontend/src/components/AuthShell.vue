<template>
  <div class="auth-shell">
    <aside class="auth-brand">
      <div class="brand-logo">
        <el-icon><Postcard /></el-icon>
        <span>拾回</span>
      </div>
      <div class="brand-copy">
        <h2>让每一件遗失物，<br />都能回到熟悉的人身边。</h2>
        <p>发布线索、查找失物，也记录每一次善意的归还。</p>
      </div>
      <div class="brand-object obj-card"><el-icon><Postcard /></el-icon><span>校园卡</span></div>
      <div class="brand-object obj-key"><el-icon><Key /></el-icon></div>
      <div class="brand-object obj-book"><el-icon><Reading /></el-icon><span>笔记本</span></div>
      <p class="brand-caption">拾到一份善意 · 归还一份安心</p>
    </aside>
    <main class="auth-panel">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.auth-shell {
  display: grid;
  grid-template-columns: minmax(380px, 46%) minmax(0, 1fr);
  min-height: 100vh;
}

.auth-brand {
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 44px clamp(28px, 4vw, 56px);
  background: #23664f;
  color: #fff;
  isolation: isolate;
}

.brand-logo { display: flex; align-items: center; gap: 10px; font-size: 18px; font-weight: 700; }
.brand-logo .el-icon { display: grid; place-items: center; width: 36px; height: 36px; background: rgb(255 255 255 / 14%); font-size: 19px; }

.brand-copy { margin-top: clamp(40px, 9vh, 96px); }
.brand-copy h2 { margin: 0; font-size: clamp(24px, 2.4vw, 32px); font-weight: 650; line-height: 1.45; }
.brand-copy p { margin-top: 14px; color: #b7d9c8; font-size: 14px; line-height: 1.8; }

.brand-object { position: absolute; z-index: -1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; box-shadow: 0 16px 30px rgb(20 45 33 / 20%); }
.brand-object .el-icon { font-size: 40px; }
.brand-object span { font-size: 12px; }
.obj-card { right: 12%; bottom: 132px; width: 118px; height: 150px; transform: rotate(-8deg); background: #f7f2e7; color: #39805e; }
.obj-card .el-icon { font-size: 46px; }
.obj-key { right: 32%; bottom: 96px; width: 76px; height: 76px; transform: rotate(10deg); background: #d7b976; color: #fffaf0; }
.obj-book { right: 17%; bottom: -26px; width: 128px; height: 96px; transform: rotate(5deg); background: #d58568; color: #fff7ef; }

.brand-caption { margin-top: auto; padding-top: 40px; color: #64816e; font-size: 12px; }

.auth-panel { display: flex; align-items: center; justify-content: center; padding: 40px 24px; background: #f4f7f5; }
.auth-panel > :deep(*) { width: min(430px, 100%); }

@media (max-width: 860px) {
  .auth-shell { grid-template-columns: 1fr; }
  .auth-brand { min-height: 200px; padding: 28px 24px; }
  .brand-copy { margin-top: 22px; }
  .brand-copy h2 { font-size: 20px; }
  .brand-copy p { display: none; }
  .brand-object { display: none; }
  .brand-caption { display: none; }
  .auth-panel { padding: 30px 20px 44px; align-items: flex-start; }
}
</style>
