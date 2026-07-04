<template>
  <view class="mine-page">
    <view class="user-header">
      <image :src="userInfo.avatar || '/static/logo.png'" class="avatar" mode="aspectFill" />
      <text class="username">{{ userInfo.realName || userInfo.username || '未登录' }}</text>
      <text class="user-desc">{{ userInfo.mobile || '' }}</text>
    </view>

    <view class="menu-section">
      <view class="menu-item" @click="goFace">
        <text class="menu-text">人脸识别门禁</text>
        <text class="menu-arrow">></text>
      </view>
      <view class="menu-item" @click="goScan">
        <text class="menu-text">扫码查居民</text>
        <text class="menu-arrow">></text>
      </view>
      <view class="menu-item" @click="goMap">
        <text class="menu-text">小区地图</text>
        <text class="menu-arrow">></text>
      </view>
      <view class="menu-item" @click="aboutApp">
        <text class="menu-text">关于应用</text>
        <text class="menu-arrow">></text>
      </view>
    </view>

    <button class="logout-btn" @click="handleLogout">退出登录</button>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const userInfo = ref({})

onMounted(() => {
  try {
    userInfo.value = uni.getStorageSync('userInfo') || {}
  } catch (e) {}
})

const goFace = () => uni.navigateTo({ url: '/pages/face/face' })
const goScan = () => uni.switchTab({ url: '/pages/scan/scan' })
const goMap = () => uni.switchTab({ url: '/pages/map/map' })

const aboutApp = () => {
  uni.showModal({
    title: '智慧小区',
    content: '智慧小区管理系统 v1.0.0\n基于UniApp + Vue3开发',
    showCancel: false
  })
}

const handleLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定退出登录？',
    success: (res) => {
      if (res.confirm) {
        uni.removeStorageSync('token')
        uni.removeStorageSync('userInfo')
        uni.reLaunch({ url: '/pages/login/login' })
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.mine-page {
  min-height: 100vh;
  background: #f5f7fa;

  .user-header {
    background: linear-gradient(135deg, #409EFF, #337ecc);
    padding: 60rpx 30rpx 50rpx;
    display: flex;
    flex-direction: column;
    align-items: center;

    .avatar {
      width: 140rpx;
      height: 140rpx;
      border-radius: 50%;
      border: 4rpx solid rgba(255,255,255,0.3);
      background: #fff;
    }
    .username {
      font-size: 36rpx;
      color: #fff;
      font-weight: bold;
      margin-top: 20rpx;
    }
    .user-desc {
      font-size: 26rpx;
      color: rgba(255,255,255,0.8);
      margin-top: 8rpx;
    }
  }

  .menu-section {
    background: #fff;
    margin: 20rpx;
    border-radius: 12rpx;
    overflow: hidden;

    .menu-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 30rpx;
      border-bottom: 1px solid #f0f0f0;

      &:last-child { border-bottom: none; }

      .menu-text { font-size: 30rpx; color: #303133; }
      .menu-arrow { font-size: 28rpx; color: #c0c4cc; }
    }
  }

  .logout-btn {
    margin: 40rpx 20rpx;
    background: #fff;
    color: #F56C6C;
    border-radius: 12rpx;
    height: 88rpx;
    line-height: 88rpx;
    font-size: 32rpx;
  }
}
</style>
