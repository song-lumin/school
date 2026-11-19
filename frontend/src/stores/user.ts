// stores/user.ts —— 用户状态管理仓库（Pinia Store）
// 功能：集中保存当前登录用户的登录凭证（token）与用户信息（userInfo），
//       并提供读写、清除、初始化这些状态的方法。
// 在系统中的作用：
//   1) 全局唯一登录态：路由守卫、页面组件、API 层都从这里读取登录状态与角色；
//   2) 持久化：token/user 同步写入 localStorage，刷新浏览器后登录态不丢失；
//   3) 采用“组合式 API 风格”（setup store）定义，比 Options 风格更贴近 Vue 3 习惯。
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types'
import { authApi } from '@/api'

export const useUserStore = defineStore('user', () => {
  // ------- 响应式状态 -------
  // token：登录凭证。初始值从 localStorage 恢复，实现“刷新页面仍保持登录”
  const token = ref<string>(localStorage.getItem('token') || '')
  // userInfo：当前用户详细信息（含角色、信用分等），初始为 null（未登录或尚未拉取）
  const userInfo = ref<User | null>(null)

  // ------- 派生状态（计算属性）-------
  // 是否已登录：token 非空即视为已登录
  const isLoggedIn = computed(() => !!token.value)
  // 是否系统管理员：角色为 SYS_ADMIN
  const isAdmin = computed(() => userInfo.value?.role === 'SYS_ADMIN')
  // 是否投放点管理员：角色为 POINT_ADMIN
  const isPointAdmin = computed(() => userInfo.value?.role === 'POINT_ADMIN')

  // ------- 操作方法 -------
  // 保存新 token 并同步到 localStorage（登录成功时调用）
  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  // 保存用户信息并同步到 localStorage（登录 / 拉取最新信息时调用）
  function setUserInfo(user: User) {
    userInfo.value = user
    localStorage.setItem('user', JSON.stringify(user))
  }

  // 清空登录态：内存中的 ref 和 localStorage 一起清除（登出或 token 失效时调用）
  function clearAuth() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  // 从后端拉取当前用户最新信息（例如修改资料、管理员调整信用分后刷新）
  // 拉取失败（如 token 已失效）则清空本地登录态并向上抛出异常
  async function fetchUserInfo() {
    try {
      const res = await authApi.getCurrentUser()
      setUserInfo(res.data)
    } catch (error) {
      clearAuth()
      throw error
    }
  }

  // 应用启动时从 localStorage 恢复用户信息（token 已在 ref 初始化时恢复）
  // JSON.parse 用 try/catch 包裹：若存储内容被人为篡改成非法 JSON，则直接移除，避免崩溃
  function initFromStorage() {
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      try {
        userInfo.value = JSON.parse(storedUser)
      } catch (e) {
        localStorage.removeItem('user')
      }
    }
  }

  // store 创建时立即执行一次恢复，保证 Store 初始化完成即拥有登录态
  initFromStorage()

  // 导出状态与方法供外部使用
  return {
    token,
    userInfo,
    isLoggedIn,
    isAdmin,
    isPointAdmin,
    setToken,
    setUserInfo,
    clearAuth,
    fetchUserInfo
  }
})
