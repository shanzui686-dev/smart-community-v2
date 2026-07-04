<template>
  <view class="scan-page">
    <view class="scan-header">
      <text class="scan-title">扫码查居民</text>
      <text class="scan-desc">扫描居民二维码查询信息</text>
    </view>

    <view class="scan-area">
      <image v-if="scanResult" :src="scanResult.faceUrl" class="person-avatar" mode="aspectFill" />
      <view v-else class="scan-frame" @click="startScan">
        <text class="scan-icon">📱</text>
        <text class="scan-text">点击扫描二维码</text>
      </view>
    </view>

    <view class="person-info" v-if="scanResult">
      <view class="info-item">
        <text class="info-label">姓名</text>
        <text class="info-value">{{ scanResult.userName }}</text>
      </view>
      <view class="info-item">
        <text class="info-label">手机号</text>
        <text class="info-value">{{ scanResult.mobile }}</text>
      </view>
      <view class="info-item">
        <text class="info-label">小区</text>
        <text class="info-value">{{ scanResult.communityName }}</text>
      </view>
      <view class="info-item">
        <text class="info-label">门牌号</text>
        <text class="info-value">{{ scanResult.houseNo }}</text>
      </view>
      <view class="info-item">
        <text class="info-label">人员类型</text>
        <text class="info-value">{{ scanResult.personType === 1 ? '业主' : scanResult.personType === 2 ? '租户' : '家属' }}</text>
      </view>
    </view>

    <button v-if="scanResult" class="rescan-btn" @click="rescan">重新扫描</button>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { scanPerson } from '../../utils/request'

const scanResult = ref(null)

const startScan = () => {
  uni.scanCode({
    success: async (res) => {
      // 二维码内容示例: personId=123
      const personId = res.result.replace('personId=', '')
      uni.showLoading({ title: '查询中...' })
      try {
        const result = await scanPerson(personId)
        scanResult.value = result.data
      } catch (e) {
        uni.showToast({ title: '未找到该居民', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },
    fail: () => {
      uni.showToast({ title: '扫码取消', icon: 'none' })
    }
  })
}

const rescan = () => {
  scanResult.value = null
  startScan()
}
</script>

<style lang="scss" scoped>
.scan-page {
  padding: 30rpx;

  .scan-header {
    text-align: center;
    margin-bottom: 40rpx;

    .scan-title {
      font-size: 40rpx;
      font-weight: bold;
      color: #303133;
      display: block;
    }
    .scan-desc {
      font-size: 26rpx;
      color: #909399;
      margin-top: 12rpx;
    }
  }

  .scan-area {
    display: flex;
    justify-content: center;
    margin-bottom: 40rpx;

    .person-avatar {
      width: 240rpx;
      height: 240rpx;
      border-radius: 50%;
      background: #f0f0f0;
    }

    .scan-frame {
      width: 400rpx;
      height: 400rpx;
      border: 4rpx dashed #409EFF;
      border-radius: 24rpx;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: #f0f9ff;

      .scan-icon { font-size: 80rpx; margin-bottom: 20rpx; }
      .scan-text { font-size: 28rpx; color: #409EFF; }
    }
  }

  .person-info {
    background: #fff;
    border-radius: 12rpx;
    padding: 20rpx 30rpx;

    .info-item {
      display: flex;
      justify-content: space-between;
      padding: 20rpx 0;
      border-bottom: 1px solid #f0f0f0;

      &:last-child { border-bottom: none; }

      .info-label { font-size: 28rpx; color: #909399; }
      .info-value { font-size: 28rpx; color: #303133; font-weight: 500; }
    }
  }

  .rescan-btn {
    margin-top: 30rpx;
    background: #f5f7fa;
    color: #606266;
    border-radius: 12rpx;
    height: 80rpx;
    line-height: 80rpx;
  }
}
</style>
