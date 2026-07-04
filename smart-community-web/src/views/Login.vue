<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="title">智慧小区管理系统</h2>
      <p class="subtitle">Smart Community Management System</p>
      <el-form ref="formRef" :model="loginForm" :rules="rules" size="large">
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码"
            prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item prop="captcha">
          <div class="captcha-row">
            <el-input v-model="loginForm.captcha" placeholder="验证码" maxlength="4" style="flex:1" />
            <img :src="captchaImage" class="captcha-img" @click="refreshCaptcha" title="点击刷新验证码" />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width:100%" @click="handleLogin">
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const captchaImage = ref('')

const loginForm = reactive({
  username: 'admin',
  password: 'admin123',
  captcha: '',
  captchaKey: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const refreshCaptcha = async () => {
  try {
    const { data } = await request.get('/auth/captcha')
    captchaImage.value = data.captchaImage
    loginForm.captchaKey = data.captchaKey
    loginForm.captcha = ''
  } catch {}
}

const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.login(loginForm)
        ElMessage.success('登录成功')
        router.push('/')
      } catch {
        refreshCaptcha()
      } finally {
        loading.value = false
      }
    }
  })
}

onMounted(() => { refreshCaptcha() })
</script>

<style lang="scss" scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  .login-card {
    width: 420px;
    padding: 40px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 10px 40px rgba(0,0,0,0.15);

    .title {
      text-align: center;
      margin: 0 0 8px;
      font-size: 24px;
      color: #303133;
    }

    .subtitle {
      text-align: center;
      margin: 0 0 32px;
      font-size: 13px;
      color: #909399;
    }

    .captcha-row {
      display: flex;
      gap: 10px;
      align-items: center;
    }
    .captcha-img {
      width: 130px;
      height: 48px;
      border-radius: 4px;
      cursor: pointer;
      border: 1px solid #dcdfe6;
      flex-shrink: 0;
    }
  }
}
</style>
