// utils/request.ts —— axios 实例及全局拦截器封装
// 功能：创建一个统一配置的 axios 实例，所有 API 调用都通过它发出。
// 为什么需要：
//   1) 统一 baseURL 与超时配置，避免每个接口重复书写；
//   2) 请求拦截器自动附加登录 token，页面代码无需关心认证细节；
//   3) 响应拦截器统一处理业务错误与 HTTP 错误（弹提示、踢回登录页），
//      各调用方只需写成功分支，异常处理逻辑集中在一处。
import axios from 'axios'
import type { ApiResponse } from '@/types'
import { ElMessage } from 'element-plus'   // Element Plus 的全局消息提示组件
import router from '@/router'

// 创建 axios 实例：
// - baseURL 为 '/api'，配合 vite 代理转发到后端（见 vite.config.ts）；
// - 超时 10 秒，防止后端无响应时请求无限挂起
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// ==================== 请求拦截器：发出请求前执行 ====================
// 作用：从 localStorage 取出 token，加到请求头 Authorization 中（JWT Bearer 认证），
// 后端据此识别当前用户身份。
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// ==================== 响应拦截器：收到响应后执行 ====================
// 分两种失败情况处理：
request.interceptors.response.use(
  // 情况一：HTTP 状态码为 2xx，但后端业务码可能不是 200（业务失败）
  (response): any => {
    const res: ApiResponse = response.data
    if (res.code === 200) {
      // 业务成功：直接返回解包后的响应体 { code, message, data }，调用方取 res.data 即业务数据
      return res
    } else {
      // 业务失败（如参数错误、状态不允许）：统一弹错误提示，并让 Promise 进入 reject，
      // 调用方的 catch / 全局错误处理可以感知到失败
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  // 情况二：HTTP 层面的错误（4xx / 5xx / 网络断开）
  (error) => {
    if (error.response) {
      // 服务器有响应，按状态码给出针对性的中文提示
      const status = error.response.status
      const data = error.response.data

      if (status === 401) {
        // 未登录 / token 过期：清除本地登录态并跳转登录页，强制重新认证
        ElMessage.error('未登录或登录已过期')
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        router.push('/login')
      } else if (status === 403) {
        // 已登录但角色权限不够
        ElMessage.error('权限不足')
      } else if (status === 404) {
        // 请求的资源不存在
        ElMessage.error('请求的资源不存在')
      } else {
        // 其他错误：优先展示后端返回的错误信息
        ElMessage.error(data?.message || '请求失败')
      }
    } else {
      // 没有响应对象：通常是网络不通、服务器未启动等
      ElMessage.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default request
