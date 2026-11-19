<!--
  App.vue —— 应用根组件
  功能：整个 Vue 应用的最顶层容器，所有页面都渲染在其中的 <router-view> 里。
  作用：由 main.ts 创建应用实例时挂载，本身不包含业务逻辑，只负责：
    1) 提供路由出口（router-view），根据当前 URL 切换显示不同页面；
    2) 定义全局基础样式（CSS Reset + 全局字体）。
-->
<template>
  <!-- 路由出口：当前路由匹配到的页面组件会渲染在这里 -->
  <router-view />
</template>

<script setup lang="ts">
// 根组件没有自身状态与逻辑，使用空 <script setup> 即可
</script>

<style>
/* 全局样式 Reset：清除浏览器默认的 margin/padding，统一盒模型为 border-box，
   避免不同浏览器默认样式差异导致布局错位 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 根元素撑满整个视口高度（配合需要全屏/滚动的页面布局），
   并设置全局字体族：优先系统字体，保证各平台观感一致 */
html, body, #app {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>
