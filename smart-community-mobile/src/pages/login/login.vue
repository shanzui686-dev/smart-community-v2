<template>
  <view class="login-wrap">
    <view class="login-header">
      <view class="logo-circle">SC</view>
      <text class="login-title">智慧小区</text>
      <text class="login-sub">Smart Community</text>
    </view>

    <view class="login-card">
      <view class="field">
        <text class="field-label">用户名</text>
        <input class="field-input" v-model="username" placeholder="请输入用户名" />
      </view>
      <view class="field">
        <text class="field-label">密码</text>
        <input class="field-input" v-model="password" type="password" placeholder="请输入密码" />
      </view>
      <view class="field">
        <text class="field-label">验证码</text>
        <view class="captcha-row">
          <input class="captcha-input" v-model="captchaCode" placeholder="请输入验证码" />
          <image v-if="captchaImg" class="captcha-img" :src="captchaImg" @click="loadCaptcha" mode="aspectFit" />
          <view v-else class="captcha-load" @click="loadCaptcha"><text>点击获取</text></view>
        </view>
      </view>
      <view class="submit-btn" @click="doLogin">
        <text>{{ loading ? '登录中...' : '登 录' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { login, getCaptcha, isTokenExpired } from '../../utils/request'

const username = ref('admin')
const password = ref('admin123')
const captchaCode = ref('')
const captchaKey = ref('')
const captchaImg = ref('')
const loading = ref(false)

const token = localStorage.getItem('token')
if (token && !isTokenExpired()) {
  setTimeout(() => { uni.redirectTo({ url: '/pages/index/index' }) }, 100)
} else {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
}

async function loadCaptcha() {
  console.log('loadCaptcha called')
  try {
    const res = await getCaptcha()
    console.log('getCaptcha response:', res)
    captchaKey.value = res.data.captchaKey
    captchaImg.value = res.data.captchaImage
  } catch (e) {
    console.error('loadCaptcha failed:', e)
    uni.showToast({ title: '获取验证码失败', icon: 'none' })
  }
}

if (!token || isTokenExpired()) {
  loadCaptcha()
}

async function doLogin() {
  if (!username.value || !password.value) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' }); return
  }
  if (!captchaCode.value) {
    uni.showToast({ title: '请输入验证码', icon: 'none' }); return
  }
  loading.value = true
  try {
    const res = await login({
      username: username.value,
      password: password.value,
      captcha: captchaCode.value,
      captchaKey: captchaKey.value
    })
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('userInfo', JSON.stringify(res.data))
    setTimeout(() => { uni.redirectTo({ url: '/pages/index/index' }) }, 100)
  } catch (e) {
    uni.showToast({ title: e.message || '登录失败', icon: 'none' })
    loadCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<style>
.login-wrap {
  min-height: 100vh;
  background: linear-gradient(135deg, #409EFF, #2060c0);
  display: flex; flex-direction: column;
  align-items: center; justify-content: center; padding: 40rpx;
}
.login-header { display: flex; flex-direction: column; align-items: center; margin-bottom: 80rpx; }
.logo-circle {
  width: 120rpx; height: 120rpx; border-radius: 50%;
  background: rgba(255,255,255,0.25);
  display: flex; align-items: center; justify-content: center;
  font-size: 44rpx; color: #fff; font-weight: bold; margin-bottom: 20rpx;
}
.login-title { font-size: 48rpx; color: #fff; font-weight: bold; }
.login-sub { font-size: 24rpx; color: rgba(255,255,255,0.7); margin-top: 8rpx; }
.login-card {
  width: 100%; max-width: 600rpx; background: #fff;
  border-radius: 16rpx; padding: 40rpx 30rpx;
}
.field { margin-bottom: 24rpx; }
.field-label { font-size: 28rpx; color: #333; margin-bottom: 10rpx; display: block; }
.field-input {
  border: 1px solid #dcdfe6; border-radius: 8rpx;
  padding: 18rpx 20rpx; font-size: 28rpx; height: auto; line-height: 1.4;
}
.captcha-row { display: flex; align-items: center; }
.captcha-input {
  flex: 1; border: 1px solid #dcdfe6; border-radius: 8rpx;
  padding: 18rpx 20rpx; font-size: 28rpx; height: auto; line-height: 1.4; margin-right: 16rpx;
}
.captcha-img { width: 200rpx; height: 74rpx; border-radius: 8rpx; border: 1px solid #dcdfe6; flex-shrink: 0; }
.captcha-load {
  width: 200rpx; height: 74rpx; background: #ecf5ff; border-radius: 8rpx;
  display: flex; align-items: center; justify-content: center;
  font-size: 24rpx; color: #409EFF; border: 1px dashed #409EFF; flex-shrink: 0;
}
.submit-btn {
  background: #409EFF; color: #fff; border-radius: 8rpx;
  margin-top: 30rpx; padding: 22rpx 0; text-align: center; font-size: 32rpx;
}
</style>
