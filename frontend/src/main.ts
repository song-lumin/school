// main.ts —— 应用入口文件
// 功能：创建并启动整个 Vue 应用，装配 Pinia（状态管理）、Vue Router（路由）、
//       Element Plus（UI 组件库）等全局插件，最后把应用挂载到 index.html 的 #app 节点上。
// 为什么需要：Vite 以本文件为打包入口，所有全局性的初始化（插件注册、全局样式引入、
//       全局组件注册）都必须在这里完成后应用才能正常运行。
import { createApp } from 'vue'
import { createPinia } from 'pinia'                       // Pinia：Vue 官方推荐的状态管理库，用于存放用户登录态等全局数据
import ElementPlus from 'element-plus'                    // Element Plus：本项目使用的 UI 组件库（按钮、弹窗、表单等）
import 'element-plus/dist/index.css'                      // Element Plus 的全局样式
import './styles/theme.css'                               // 项目自定义主题样式（覆盖/补充 Element Plus 默认样式）
import * as ElementPlusIconsVue from '@element-plus/icons-vue' // Element Plus 官方图标库（全部图标）
import App from './App.vue'                               // 根组件
import router from './router'                             // 路由实例（router/index.ts 中创建）

// 创建 Vue 应用实例和 Pinia 状态管理实例
const app = createApp(App)
const pinia = createPinia()

// 将图标库中的所有图标注册为全局组件：
// 之后模板里可直接写 <el-icon><User /></el-icon> 这类用法，无需在每个页面单独 import
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 依次安装全局插件：状态管理、路由、UI 组件库
app.use(pinia)
app.use(router)
app.use(ElementPlus)

// 把应用挂载到 index.html 中 id="app" 的 DOM 元素上，页面开始渲染
app.mount('#app')
