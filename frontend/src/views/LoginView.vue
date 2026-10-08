<template>
  <AuthShell>
    <div class="auth-card">
      <div class="auth-head">
        <h2>欢迎回到拾回</h2>
        <p>登录后继续管理你的线索与诚信记录</p>
      </div>
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
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const res = await authApi.login(form)
      userStore.setToken(res.data.token)
      userStore.setUserInfo(res.data.user)
      ElMessage.success('登录成功')
      router.push('/home')
    } catch (error) {
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
