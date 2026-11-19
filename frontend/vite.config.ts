// vite.config.ts —— Vite 构建工具的配置文件
// 功能：定义前端项目在开发与构建时的行为，包括 Vue 插件、路径别名和开发服务器代理。
// 为什么需要：
//   1) 注册 @vitejs/plugin-vue 插件，让 Vite 能编译 .vue 单文件组件；
//   2) 配置 '@' 别名，代码中可以用 '@/xxx' 代替冗长的相对路径引用 src 目录；
//   3) 开发环境配置反向代理：前端运行在 5173 端口，后端 API 在 8080 端口，
//      直接跨域请求会被浏览器的同源策略拦截，通过代理把 /api 与 /uploads 请求
//      转发到后端，实现“同源”，规避跨域问题（生产环境由 Nginx 等承担同样职责）。
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'          // defineConfig 仅为提供配置对象的类型提示
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // 插件列表：启用 Vue 单文件组件支持
  plugins: [vue()],
  resolve: {
    alias: {
      // 把 '@' 映射到 src 目录的绝对路径：import xxx from '@/api' 等价于 src/api
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    // 开发服务器监听的端口，启动后访问 http://localhost:5173
    port: 5173,
    proxy: {
      // 代理规则 1：所有 /api 开头的请求转发到后端服务
      // （例如前端请求 /api/items -> http://localhost:8080/api/items）
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true   // 修改请求头中的 Host 为目标地址，后端校验 Host 时不会拒绝
      },
      // 代理规则 2：用户上传的图片等静态资源由后端提供，同样需要转发
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
