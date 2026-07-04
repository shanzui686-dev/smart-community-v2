<template>
  <view class="face-page">
    <view class="face-header">
      <text class="face-title">人脸识别门禁</text>
      <text class="face-desc">请将面部对准摄像头进行识别</text>
    </view>

    <view class="camera-area">
      <image v-if="photoUrl" :src="photoUrl" class="photo-preview" mode="aspectFill" />
      <view v-else class="camera-placeholder">
        <text class="camera-icon">📷</text>
        <text class="camera-text">点击下方按钮拍照</text>
      </view>
    </view>

    <view class="action-buttons">
      <button class="btn-primary" @click="takePhoto">拍照识别</button>
      <button v-if="photoUrl" class="btn-success" @click="verifyFace">确认验证</button>
      <button v-if="photoUrl" class="btn-default" @click="retake">重新拍照</button>
    </view>

    <view class="result-area" v-if="result">
      <view :class="['result-card', result.verified ? 'success' : 'fail']">
        <text class="result-title">{{ result.verified ? '验证通过' : '验证失败' }}</text>
        <text class="result-info">{{ result.personName || '' }}</text>
        <text class="result-time">{{ result.time }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { faceVerify } from '../../utils/request'

const photoUrl = ref('')
const result = ref(null)
const tempFilePath = ref('')

const takePhoto = () => {
  uni.chooseImage({
    count: 1,
    sourceType: ['camera'],
    success: (res) => {
      photoUrl.value = res.tempFilePaths[0]
      tempFilePath.value = res.tempFilePaths[0]
      result.value = null
    },
    fail: () => {
      uni.showToast({ title: '拍照失败', icon: 'none' })
    }
  })
}

const verifyFace = async () => {
  uni.showLoading({ title: '识别中...' })
  try {
    const res = await faceVerify({ imagePath: tempFilePath.value })
    result.value = {
      verified: res.data?.verified === 1,
      personName: res.data?.personName || '',
      time: new Date().toLocaleString()
    }
    if (result.value.verified) {
      uni.vibrateShort()
    }
  } catch (e) {
    result.value = { verified: false, personName: '', time: '' }
  } finally {
    uni.hideLoading()
  }
}

const retake = () => {
  photoUrl.value = ''
  result.value = null
}
</script>

<style lang="scss" scoped>
.face-page {
  padding: 30rpx;

  .face-header {
    text-align: center;
    margin-bottom: 40rpx;

    .face-title {
      font-size: 40rpx;
      font-weight: bold;
      color: #303133;
      display: block;
    }
    .face-desc {
      font-size: 26rpx;
      color: #909399;
      margin-top: 12rpx;
    }
  }

  .camera-area {
    width: 100%;
    height: 500rpx;
    border-radius: 16rpx;
    overflow: hidden;
    background: #000;
    margin-bottom: 40rpx;

    .photo-preview {
      width: 100%;
      height: 100%;
    }

    .camera-placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      background: linear-gradient(135deg, #667eea, #764ba2);

      .camera-icon { font-size: 80rpx; margin-bottom: 20rpx; }
      .camera-text { font-size: 28rpx; color: rgba(255,255,255,0.8); }
    }
  }

  .action-buttons {
    display: flex;
    flex-direction: column;
    gap: 20rpx;

    button {
      border-radius: 12rpx;
      height: 88rpx;
      line-height: 88rpx;
      font-size: 32rpx;
    }

    .btn-primary { background: #409EFF; color: #fff; }
    .btn-success { background: #67C23A; color: #fff; }
    .btn-default { background: #f5f7fa; color: #606266; border: 1px solid #dcdfe6; }
  }

  .result-card {
    margin-top: 40rpx;
    padding: 30rpx;
    border-radius: 12rpx;
    text-align: center;

    &.success { background: #f0f9eb; }
    &.fail { background: #fef0f0; }

    .result-title {
      font-size: 36rpx;
      font-weight: bold;
    }
    .success .result-title { color: #67C23A; }
    .fail .result-title { color: #F56C6C; }

    .result-info { font-size: 28rpx; color: #606266; margin-top: 12rpx; }
    .result-time { font-size: 24rpx; color: #909399; margin-top: 8rpx; }
  }
}
</style>
