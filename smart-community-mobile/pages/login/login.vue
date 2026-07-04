<template>
  <view class="login-page">
    <view class="login-header">
      <image src="/static/logo.png" class="logo" mode="aspectFit" />
      <text class="title">智慧小区</text>
      <text class="subtitle">Smart Community</text>
    </view>

    <view class="login-form">
      <view class="form-item">
        <text class="label">用户名</text>
        <input class="input" v-model="username" placeholder="请输入用户名" />
      </view>
      <view class="form-item">
        <text class="label">密码</text>
        <input class="input" v-model="password" type="password" placeholder="请输入密码" />
      </view>
      <button class="login-btn" @click="handleLogin" :loading="loading">登 录</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { login } from '../../utils/request'

const username = ref('admin')
const password = ref('admin123')
const loading = ref(false)

const handleLogin = async () => {
  if (!username.value || !password.value) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const res = await login({ username: username.value, password: password.value })
    uni.setStorageSync('token', res.data.token)
    uni.setStorageSync('userInfo', res.data)
    uni.showToast({ title: '登录成功' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/index/index' })
    }, 500)
  } catch (e) {
    // error handled in request
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #409EFF, #337ecc);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40rpx;

  .login-header {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-bottom: 80rpx;

    .logo {
      width: 120rpx;
      height: 120rpx;
      margin-bottom: 20rpx;
    }
    .title {
      font-size: 48rpx;
      color: #fff;
      font-weight: bold;
    }
    .subtitle {
      font-size: 24rpx;
      color: rgba(255,255,255,0.7);
    }
  }

  .login-form {
    width: 100%;
    max-width: 600rpx;
    background: #fff;
    border-radius: 16rpx;
    padding: 40rpx;

    .form-item {
      margin-bottom: 30rpx;

      .label {
        font-size: 28rpx;
        color: #333;
        margin-bottom: 12rpx;
        display: block;
      }
      .input {
        border: 1px solid #e4e7ed;
        border-radius: 8rpx;
        padding: 20rpx 24rpx;
        font-size: 28rpx;
      }
    }

    .login-btn {
      background: #409EFF;
      color: #fff;
      border-radius: 8rpx;
      margin-top: 40rpx;
      height: 88rpx;
      line-height: 88rpx;
      font-size: 32rpx;
    }
  }
}
</style>
