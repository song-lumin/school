<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-content">
        <div class="logo">
          <el-icon><Postcard /></el-icon>
          <span>拾回<span class="logo-subtitle">校园失物招领</span></span>
        </div>
        <el-menu
          mode="horizontal"
          :default-active="activeMenu"
          :ellipsis="false"
          class="header-menu"
          router
        >
          <el-menu-item index="/home">首页</el-menu-item>
          <el-menu-item index="/items">失物招领</el-menu-item>
          <el-menu-item index="/notices">寻物启事</el-menu-item>
          <el-menu-item index="/claims" v-if="userStore.isLoggedIn">认领管理</el-menu-item>
          <el-menu-item index="/disputes" v-if="userStore.isLoggedIn">申诉中心</el-menu-item>
          <el-menu-item index="/certificates" v-if="userStore.isLoggedIn && !userStore.isAdmin && !userStore.isPointAdmin">诚信证书</el-menu-item>
          <el-menu-item index="/profile" v-if="userStore.isLoggedIn">个人中心</el-menu-item>
          <el-menu-item index="/admin" v-if="userStore.isAdmin">管理后台</el-menu-item>
          <el-menu-item index="/point-admin" v-if="userStore.isPointAdmin || userStore.isAdmin">点位工作台</el-menu-item>
        </el-menu>
        <div class="header-right">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-icon><User /></el-icon>
                {{ userStore.userInfo?.realName }}
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" @click="$router.push('/login')">登录</el-button>
            <el-button @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>
    <el-main class="layout-main">
      <router-view />
    </el-main>
    <el-footer class="layout-footer">
      <p>拾回 · 校园失物招领与诚信同行</p>
    </el-footer>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await authApi.logout()
      userStore.clearAuth()
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch (error) {
      console.error('退出登录失败:', error)
    }
  } else if (command === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
}

.layout-header {
  position: sticky;
  top: 0;
  z-index: 20;
  height: auto;
  background: rgb(255 255 255 / 96%);
  border-bottom: 1px solid #e4ebe6;
  padding: 0;
}

.header-content {
  display: flex;
  align-items: center;
  min-height: 66px;
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 28px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  color: #24664f;
  margin-right: 28px;
  white-space: nowrap;
}

.logo .el-icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  background: #e9f2ec;
  font-size: 19px;
}

.logo-subtitle {
  margin-left: 8px;
  color: #829087;
  font-size: 11px;
  font-weight: 450;
}

.header-menu {
  flex: 1;
  min-width: 0;
  border-bottom: none;
}

.header-menu :deep(.el-menu-item) {
  padding: 0 13px;
  color: #5b6961;
  font-size: 13px;
}

.header-menu :deep(.el-menu-item.is-active) {
  color: #26745c;
  border-bottom-width: 2px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 0 12px;
  height: 40px;
  border-radius: 3px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.user-info .el-icon {
  margin: 0 4px;
}

.layout-main {
  background: #f4f7f5;
  padding: 24px 28px;
  min-height: calc(100vh - 120px);
}

.layout-footer {
  min-height: 54px;
  background: #f4f7f5;
  text-align: center;
  color: #89958e;
  border-top: 1px solid #e4ebe6;
  font-size: 12px;
}

.layout-footer p {
  margin: 0;
}

@media (max-width: 1100px) {
  .header-content { flex-wrap: wrap; padding: 0 20px 8px; }
  .logo { min-height: 62px; }
  .header-right { margin-left: auto; }
  .header-menu { order: 3; flex-basis: 100%; overflow-x: auto; overflow-y: hidden; }
  .header-menu :deep(.el-menu) { width: max-content; }
  .header-menu :deep(.el-menu-item) { height: 42px; line-height: 42px; }
}

@media (max-width: 640px) {
  .header-content { padding: 0 14px 6px; }
  .logo-subtitle { display: none; }
  .header-right { gap: 5px; }
  .layout-main { padding: 16px 14px; }
  .layout-footer { min-height: 46px; }
}
</style>
