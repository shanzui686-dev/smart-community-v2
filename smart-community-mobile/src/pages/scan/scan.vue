<template>
  <view class="page">
    <view class="header">
      <text class="title">扫码查居民</text>
      <text class="desc">扫描居民二维码查询信息</text>
    </view>

    <view class="scan-area" @click="startScan">
      <view v-if="!result" class="scan-frame">
        <text class="scan-icon">📱</text>
        <text class="scan-text">点击扫描二维码</text>
      </view>
      <image v-else-if="result.faceUrl" :src="result.faceUrl" class="avatar" mode="aspectFill" />
    </view>

    <view v-if="result" class="info-card">
      <view class="info-row"><text class="k">姓名</text><text class="v">{{ result.userName }}</text></view>
      <view class="info-row"><text class="k">手机</text><text class="v">{{ result.mobile }}</text></view>
      <view class="info-row"><text class="k">小区</text><text class="v">{{ result.communityName }}</text></view>
      <view class="info-row"><text class="k">门牌</text><text class="v">{{ result.houseNo }}</text></view>
      <view class="info-row"><text class="k">类型</text><text class="v">{{ typeLabel(result.personType) }}</text></view>
    </view>

    <button v-if="result" class="btn" @click="rescan">重新扫描</button>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { getPerson } from '../../utils/request'

const result = ref(null)

function typeLabel(t) {
  if (t === 1) return '业主'
  if (t === 2) return '租户'
  return '家属'
}

function startScan() {
  uni.scanCode({
    success: async (res) => {
      const personId = res.result.replace(/^personId=/, '')
      uni.showLoading({ title: '查询中...' })
      try {
        const r = await getPerson(personId)
        result.value = r.data
      } catch (e) {
        uni.showToast({ title: '未找到该居民', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },
    fail: () => uni.showToast({ title: '扫码取消', icon: 'none' })
  })
}

function rescan() { result.value = null; startScan() }
</script>

<style scoped>
.page { padding: 30rpx; min-height: 100vh; background: #f5f7fa; }
.header { text-align: center; margin-bottom: 40rpx; }
.title { font-size: 38rpx; font-weight: bold; color: #303133; display: block; }
.desc { font-size: 26rpx; color: #909399; margin-top: 10rpx; }
.scan-area { display: flex; justify-content: center; margin-bottom: 30rpx; }
.scan-frame {
  width: 400rpx; height: 400rpx; border: 4rpx dashed #409EFF;
  border-radius: 24rpx; display: flex; flex-direction: column;
  align-items: center; justify-content: center; background: #f0f9ff;
}
.scan-icon { font-size: 80rpx; margin-bottom: 16rpx; }
.scan-text { font-size: 28rpx; color: #409EFF; }
.avatar { width: 240rpx; height: 240rpx; border-radius: 50%; background: #eee; }
.info-card { background: #fff; border-radius: 12rpx; padding: 20rpx 30rpx; margin-bottom: 24rpx; }
.info-row { display: flex; justify-content: space-between; padding: 20rpx 0; border-bottom: 1px solid #f5f5f5; }
.info-row:last-child { border-bottom: none; }
.k { font-size: 28rpx; color: #909399; }
.v { font-size: 28rpx; color: #303133; font-weight: 500; }
.btn { background: #f5f7fa; color: #606266; border-radius: 12rpx; height: 80rpx; line-height: 80rpx; font-size: 28rpx; border: none; }
</style>
