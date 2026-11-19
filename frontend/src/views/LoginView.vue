<!--
  登录页（LoginView）
  功能：系统入口页之一，用户输入用户名和密码换取 JWT Token。
  作用：登录成功后 Token 和用户信息存入 Pinia 全局状态（userStore），
        后续所有请求由 axios 拦截器自动携带 Token 完成身份认证。
  为什么需要它：失物招领涉及个人诚信积分、认领记录等隐私数据，必须登录后才能操作。
-->
<template>
  <AuthShell>
    <div class="auth-card">
      <div class="auth-head">
        <h2>欢迎回到拾回</h2>
        <p>登录后继续管理你的线索与诚信记录</p>
      </div>
      <!-- 登录表单：@submit.prevent 拦截原生提交，改为走 handleLogin 的异步校验流程 -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%"
            native-type="submit"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <p class="auth-switch">还没有账号？<el-link type="primary" @click="$router.push('/register')">立即注册</el-link></p>
    </div>
  </AuthShell>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { authApi } from '@/api'
import { useUserStore } from '@/stores/user'
import AuthShell from '@/components/AuthShell.vue'

const router = useRouter()
const userStore = useUserStore()  // Pinia 用户状态：存放 Token 与用户信息

const formRef = ref<FormInstance>()  // 表单实例引用，用于调用 validate() 触发校验
const loading = ref(false)           // 登录按钮 loading 态，防止重复提交

// 双向绑定的表单数据
const form = reactive({
  username: '',
  password: ''
})

// 前端校验规则：两个输入框均必填，失焦(blur)时触发
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 登录逻辑：先过前端校验 → 调用后端登录接口 → 保存 Token/用户信息 → 跳转首页
const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return  // 前端校验不通过，直接返回

    loading.value = true
    try {
      const res = await authApi.login(form)
      userStore.setToken(res.data.token)      // Token 持久化到 localStorage
      userStore.setUserInfo(res.data.user)    // 用户信息（角色、积分等）存入全局状态
      ElMessage.success('登录成功')
      router.push('/home')  // 登录成功后进入主页
    } catch (error) {
      // 账号密码错误等后端提示由 axios 拦截器统一弹出，这里只记录日志
      console.error('登录失败:', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.auth-card { padding: 34px clamp(22px, 4vw, 40px) 30px; background: #fff; border: 1px solid #e2e9e4; border-top: 4px solid #26745c; }
.auth-head { margin-bottom: 22px; }
.auth-head h2 { margin: 0; color: #26332f; font-size: 22px; font-weight: 650; }
.auth-head p { margin-top: 8px; color: #7d8a83; font-size: 13px; }
.auth-switch { display: flex; align-items: center; gap: 6px; margin: 4px 0 0; color: #7d8a83; font-size: 13px; }
</style>
