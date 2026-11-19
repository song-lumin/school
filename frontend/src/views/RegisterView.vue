<!--
  注册页（RegisterView）
  功能：新用户填写账号信息注册，成功后直接登录进入系统。
  作用：采集用户名、真实姓名、学号/工号、手机号等身份信息——
        真实姓名与学号用于认领核实和诚信积分归属，手机号用于联系失主/拾主。
  为什么需要它：校园失物招领基于真实身份的互助，注册时即收集身份信息可减少后续纠纷。
-->
<template>
  <AuthShell>
    <div class="auth-card">
      <div class="auth-head">
        <h2>加入拾回</h2>
        <p>成为校园互助网络的一员</p>
      </div>
      <!-- 注册表单：@submit.prevent 拦截原生提交，走 handleRegister 异步流程 -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleRegister"
      >
        <!-- 两列栅格布局收纳更多字段，窄屏时由 CSS 降为单列 -->
        <div class="form-grid">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="form.realName" placeholder="请输入真实姓名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              show-password
            />
          </el-form-item>
          <el-form-item label="学号/工号" prop="studentId">
            <el-input v-model="form.studentId" placeholder="请输入学号或工号" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>
        </div>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱（可选）" />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%"
            native-type="submit"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>
      <p class="auth-switch">已有账号？<el-link type="primary" @click="$router.push('/login')">立即登录</el-link></p>
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
const userStore = useUserStore()  // Pinia 用户状态：注册成功后直接存 Token，实现"注册即登录"

const formRef = ref<FormInstance>()  // 表单实例引用，用于触发整体校验
const loading = ref(false)           // 注册按钮 loading 态，防止重复提交

// 双向绑定的注册表单数据
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  studentId: '',
  phone: '',
  email: ''
})

// 自定义校验器：确认密码必须与密码一致，不一致则校验失败并给出提示
const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 前端校验规则：必填项 + 密码长度 + 手机号正则（1开头的11位大陆手机号）
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  studentId: [{ required: true, message: '请输入学号或工号', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

// 注册逻辑：前端校验 → 剔除多余字段 → 调后端注册接口 → 保存登录态 → 跳转首页
const handleRegister = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return  // 校验不通过，不发起请求

    loading.value = true
    try {
      // confirmPassword 只是前端校验用，不需要发给后端，解构剔除
      const { confirmPassword, ...registerData } = form
      const res = await authApi.register(registerData)
      userStore.setToken(res.data.token)    // 后端注册成功直接返回 Token
      userStore.setUserInfo(res.data.user)  // 免去用户再手动登录一次
      ElMessage.success('注册成功')
      router.push('/home')
    } catch (error) {
      // 用户名已存在等后端错误提示由 axios 拦截器统一弹出
      console.error('注册失败:', error)
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
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 16px; }
.auth-switch { display: flex; align-items: center; gap: 6px; margin: 4px 0 0; color: #7d8a83; font-size: 13px; }

@media (max-width: 560px) {
  .form-grid { grid-template-columns: 1fr; gap: 0; }
}
</style>
