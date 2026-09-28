import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types'
import { authApi } from '@/api'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<User | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'SYS_ADMIN')
  const isPointAdmin = computed(() => userInfo.value?.role === 'POINT_ADMIN')

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(user: User) {
    userInfo.value = user
    localStorage.setItem('user', JSON.stringify(user))
  }

  function clearAuth() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  async function fetchUserInfo() {
    try {
      const res = await authApi.getCurrentUser()
      setUserInfo(res.data)
    } catch (error) {
      clearAuth()
      throw error
    }
  }

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

  initFromStorage()

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
