<!--
  AuthShell.vue —— 登录/注册页的通用外壳布局组件
  功能：为登录、注册页面提供统一的“左右分栏”视觉框架。
  在系统中的作用：
    - 左侧（深绿色品牌区）：展示产品 Logo“拾回”、宣传语和悬浮的失物卡片装饰元素，
      传递产品理念，增强品牌识别度；
    - 右侧（浅灰面板区）：通过 <slot> 插槽承载具体页面内容（登录表单或注册表单），
      两个页面共用同一外壳，保证观感一致、避免重复代码。
  注意：本组件纯展示，无任何业务逻辑与状态。
-->
<template>
  <div class="auth-shell">
    <!-- 左侧品牌区：Logo + 标语 + 装饰性失物卡片（校园卡/钥匙/笔记本，纯视觉元素） -->
    <aside class="auth-brand">
      <div class="brand-logo">
        <el-icon><Postcard /></el-icon>
        <span>拾回</span>
      </div>
      <div class="brand-copy">
        <h2>让每一件遗失物，<br />都能回到熟悉的人身边。</h2>
        <p>发布线索、查找失物，也记录每一次善意的归还。</p>
      </div>
      <!-- 三个倾斜摆放的装饰物件，模拟“遗失物散落”的意境，无交互 -->
      <div class="brand-object obj-card"><el-icon><Postcard /></el-icon><span>校园卡</span></div>
      <div class="brand-object obj-key"><el-icon><Key /></el-icon></div>
      <div class="brand-object obj-book"><el-icon><Reading /></el-icon><span>笔记本</span></div>
      <p class="brand-caption">拾到一份善意 · 归还一份安心</p>
    </aside>
    <!-- 右侧内容区：由父页面通过插槽注入登录/注册表单 -->
    <main class="auth-panel">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
// 纯布局组件，无需逻辑
</script>

<style scoped>
/* 左右两栏布局：左栏最窄 380px、最大 46%，右栏自动占满剩余空间 */
.auth-shell {
  display: grid;
  grid-template-columns: minmax(380px, 46%) minmax(0, 1fr);
  min-height: 100vh;
}

/* 品牌区：深绿背景、白色文字；isolation 隔离混合上下文，防止装饰元素影响外层 */
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

/* Logo 行：图标 + “拾回”文字 */
.brand-logo { display: flex; align-items: center; gap: 10px; font-size: 18px; font-weight: 700; }
.brand-logo .el-icon { display: grid; place-items: center; width: 36px; height: 36px; background: rgb(255 255 255 / 14%); font-size: 19px; }

/* 标语区：与视口高度联动的上边距，在不同屏幕高度下观感均衡 */
.brand-copy { margin-top: clamp(40px, 9vh, 96px); }
.brand-copy h2 { margin: 0; font-size: clamp(24px, 2.4vw, 32px); font-weight: 650; line-height: 1.45; }
.brand-copy p { margin-top: 14px; color: #b7d9c8; font-size: 14px; line-height: 1.8; }

/* 装饰物件通用样式：绝对定位散落在右下角，z-index: -1 压到内容之下 */
.brand-object { position: absolute; z-index: -1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; box-shadow: 0 16px 30px rgb(20 45 33 / 20%); }
.brand-object .el-icon { font-size: 40px; }
.brand-object span { font-size: 12px; }
/* 三件装饰物各自的位置、尺寸、旋转角度、配色 */
.obj-card { right: 12%; bottom: 132px; width: 118px; height: 150px; transform: rotate(-8deg); background: #f7f2e7; color: #39805e; }
.obj-card .el-icon { font-size: 46px; }
.obj-key { right: 32%; bottom: 96px; width: 76px; height: 76px; transform: rotate(10deg); background: #d7b976; color: #fffaf0; }
.obj-book { right: 17%; bottom: -26px; width: 128px; height: 96px; transform: rotate(5deg); background: #d58568; color: #fff7ef; }

/* 底部小字：margin-top:auto 把它推到最底部 */
.brand-caption { margin-top: auto; padding-top: 40px; color: #64816e; font-size: 12px; }

/* 右侧表单面板：内容垂直水平居中，子元素统一限宽 430px */
.auth-panel { display: flex; align-items: center; justify-content: center; padding: 40px 24px; background: #f4f7f5; }
.auth-panel > :deep(*) { width: min(430px, 100%); }

/* 窄屏适配：单栏布局，隐藏装饰元素，仅保留紧凑的品牌区头部 */
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
