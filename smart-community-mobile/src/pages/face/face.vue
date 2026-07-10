<template>
  <view class="page">
    <view class="header">
      <text class="title">人脸识别门禁</text>
      <text class="desc">请将面部对准摄像头进行识别</text>
    </view>

    <!-- 摄像头预览区域 -->
    <view class="camera-area">
      <view class="camera-mask" />
      <camera 
        v-if="!photoUrl && showCamera && !isH5" 
        class="camera" 
        device-position="front" 
        flash="off"
        @error="onCameraError"
      />
      <image v-if="photoUrl" :src="photoUrl" class="preview" mode="aspectFill" />
      <view v-else-if="!showCamera" class="placeholder" @click="openCamera">
        <text class="ph-icon">📷</text>
        <text class="ph-text">点击打开摄像头</text>
      </view>
    </view>

    <!-- 操作按钮 -->
    <view class="btn-row">
      <view v-if="!photoUrl && showCamera" class="btn primary" @click="takePhoto">
        <text>📸 拍照识别</text>
      </view>
      <view v-else-if="!showCamera" class="btn primary" @click="openCamera">
        <text>📷 打开摄像头</text>
      </view>
      <view v-if="photoUrl" class="btn reset" @click="reset">
        <text>🔄 重新拍照</text>
      </view>
    </view>

    <!-- 加载状态遮罩 -->
    <view class="loading-overlay" v-if="loading">
      <view class="loading-card">
        <text class="loading-spinner">⏳</text>
        <text class="loading-text">识别中，请稍候...</text>
      </view>
    </view>

    <!-- 识别结果 -->
    <view class="result-section" id="resultSection">
      <view id="resultCard" class="result-card">
        <text id="resultIcon" class="result-icon"></text>
        <text id="resultTitle" class="result-title"></text>
        <view id="resultDetail" class="result-detail"></view>
        <text id="resultTime" class="result-time"></text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { faceSearch, faceRecord, uploadFile, getPerson } from '../../utils/request'

const photoUrl = ref('')
const result = ref(null)
const loading = ref(false)
const showCamera = ref(false)
const cameraContext = ref(null)
const videoRef = ref(null)
const stream = ref(null)
const isH5 = ref(false)

const openCamera = async () => {
  isH5.value = typeof navigator !== 'undefined' && navigator.mediaDevices
  
  if (isH5.value) {
    try {
      stream.value = await navigator.mediaDevices.getUserMedia({ 
        video: { facingMode: 'user', width: { ideal: 1280 }, height: { ideal: 720 } },
        audio: false 
      })
      showCamera.value = true
      setTimeout(() => {
        const container = document.querySelector('.camera-area')
        let videoEl = container.querySelector('video')
        
        if (!videoEl) {
          videoEl = document.createElement('video')
          videoEl.className = 'camera'
          videoEl.autoplay = true
          videoEl.muted = true
          videoEl.playsInline = true
          videoEl.webkitPlaysInline = true
          videoEl.disablePictureInPicture = true
          videoEl.style.objectFit = 'cover'
          videoEl.style.width = '100%'
          videoEl.style.height = '100%'
          videoEl.style.background = '#000'
          videoEl.style.position = 'absolute'
          videoEl.style.top = '0'
          videoEl.style.left = '0'
          videoEl.style.zIndex = '1'
          
          videoEl.addEventListener('pause', () => {
            videoEl.play().catch(() => {})
          })
          videoEl.addEventListener('click', (e) => {
            e.preventDefault()
            e.stopPropagation()
          })
          
          const mask = container.querySelector('.camera-mask')
          if (mask) {
            mask.style.display = 'block'
            mask.classList.add('active')
          }
          
          container.insertBefore(videoEl, mask)
        }
        
        if (videoEl) {
          videoEl.srcObject = stream.value
          videoEl.play().catch(() => {})
        }
      }, 100)
    } catch (err) {
      console.error('H5相机授权失败:', err)
      uni.showModal({
        title: '提示',
        content: '需要相机权限才能进行人脸识别，请在浏览器设置中授权',
        showCancel: false
      })
    }
  } else {
    try {
      const authRes = await uni.authorize({ scope: 'scope.camera' })
      showCamera.value = true
      setTimeout(() => {
        cameraContext.value = uni.createCameraContext()
      }, 500)
    } catch (err) {
      console.error('相机授权失败:', err)
      uni.showModal({
        title: '提示',
        content: '需要相机权限才能进行人脸识别，请在设置中授权',
        showCancel: false
      })
    }
  }
}

const onCameraError = (e) => {
  console.error('摄像头错误:', e)
  showCamera.value = false
  uni.showToast({ title: '摄像头启动失败', icon: 'none' })
}

const takePhoto = () => {
  if (isH5.value) {
    const video = document.querySelector('.camera-area video')
    if (!video) {
      uni.showToast({ title: '摄像头未就绪', icon: 'none' })
      return
    }
    
    const videoWidth = video.videoWidth || 640
    const videoHeight = video.videoHeight || 480
    
    const canvas = document.createElement('canvas')
    canvas.width = videoWidth
    canvas.height = videoHeight
    const ctx = canvas.getContext('2d')
    ctx.drawImage(video, 0, 0, videoWidth, videoHeight)
    
    const path = canvas.toDataURL('image/jpeg', 0.95)
    photoUrl.value = path
    result.value = null
    
    if (stream.value) {
      stream.value.getTracks().forEach(track => track.stop())
      stream.value = null
    }
    
    const videoEl = document.querySelector('.camera-area video')
    if (videoEl) {
      videoEl.remove()
    }
    
    doRecognize(path)
  } else {
    if (!cameraContext.value) {
      cameraContext.value = uni.createCameraContext()
    }
    
    cameraContext.value.takePhoto({
      quality: 'high',
      success: (res) => {
        const path = res.tempImagePath
        if (!path) {
          uni.showToast({ title: '未获取到照片', icon: 'none' })
          return
        }
        photoUrl.value = path
        result.value = null
        doRecognize(path)
      },
      fail: (err) => {
        console.error('拍照失败:', err)
        uni.showToast({ title: '拍照失败，请重试', icon: 'none' })
      }
    })
  }
}

const doRecognize = async (filePath) => {
  loading.value = true
  try {
    // Step 1: 上传照片到OSS
    const uploadRes = await uploadFile(filePath, 'face')
    const imageUrl = uploadRes.data || ''

    if (!imageUrl) {
      throw new Error('图片上传失败，未获取到URL')
    }

    // Step 2: 调用人脸搜索
    const searchRes = await faceSearch(imageUrl)
    const searchData = searchRes.data || {}
    const matched = searchData.matched === true
    let foundName = '未知'
    const foundScore = searchData.score != null ? searchData.score : null

    // Step 4: 如果匹配成功，获取人员姓名
    if (matched && searchData.personId) {
      try {
        const personRes = await getPerson(searchData.personId)
        const personData = personRes.data || {}
        foundName = personData.userName || personData.name || '未知'
        
        // 记录通行
        await faceRecord({
          personName: foundName,
          communityId: personData.communityId || 0,
          houseNo: personData.houseNo || '',
          cameraId: 0,
          photoUrl: imageUrl,
          direction: 1
        })
      } catch (e) {
        console.error('获取人员信息或记录通行失败:', e)
      }
    }

    // Step 5: 更新识别结果
    setTimeout(() => {
      result.value = {
        matched,
        personName: foundName,
        score: foundScore,
        time: new Date().toLocaleString()
      }
      loading.value = false
      
      const section = document.getElementById('resultSection')
      const card = document.getElementById('resultCard')
      const icon = document.getElementById('resultIcon')
      const title = document.getElementById('resultTitle')
      const detail = document.getElementById('resultDetail')
      const time = document.getElementById('resultTime')
      
      if (card) {
        card.classList.remove('pass', 'fail')
        card.classList.add(matched ? 'pass' : 'fail')
      }
      if (icon) icon.textContent = matched ? '✅' : '❌'
      if (title) title.textContent = matched ? '识别成功' : '未识别到人员'
      if (time) time.textContent = new Date().toLocaleString()
      
      if (detail) {
        if (matched) {
          detail.innerHTML = `
            <view class="detail-row">
              <text class="detail-label">姓名</text>
              <text class="detail-value">${foundName}</text>
            </view>
            ${foundScore != null ? `
            <view class="detail-row">
              <text class="detail-label">置信度</text>
              <text class="detail-value">${foundScore}</text>
            </view>
            ` : ''}
          `
        } else {
          detail.innerHTML = '<text class="fail-hint">未识别到人员信息，请重新拍照</text>'
        }
      }
      
      if (section) section.classList.add('visible')
    }, 100)
  } catch (e) {
    console.error('识别流程失败:', e)
    setTimeout(() => {
      uni.showToast({ title: e.message || '识别失败，请重试', icon: 'none' })
      result.value = {
        matched: false,
        personName: '',
        score: null,
        time: new Date().toLocaleString()
      }
      loading.value = false
      
      const section = document.getElementById('resultSection')
      const card = document.getElementById('resultCard')
      const icon = document.getElementById('resultIcon')
      const title = document.getElementById('resultTitle')
      const detail = document.getElementById('resultDetail')
      const time = document.getElementById('resultTime')
      
      if (card) {
        card.classList.remove('pass', 'fail')
        card.classList.add('fail')
      }
      if (icon) icon.textContent = '❌'
      if (title) title.textContent = '未识别到人员'
      if (time) time.textContent = new Date().toLocaleString()
      
      if (detail) {
        detail.innerHTML = '<text class="fail-hint">未识别到人员信息，请重新拍照</text>'
      }
      
      if (section) section.classList.add('visible')
    }, 100)
  }
}

const reset = () => {
  photoUrl.value = ''
  result.value = null
  loading.value = false
  showCamera.value = false
  const section = document.getElementById('resultSection')
  if (section) section.classList.remove('visible')
}
</script>

<style scoped>
.page {
  padding: 30rpx;
  min-height: 100vh;
  background: #f5f7fa;
}

.header {
  text-align: center;
  margin-bottom: 40rpx;
}

.title {
  font-size: 38rpx;
  font-weight: bold;
  color: #303133;
  display: block;
}

.desc {
  font-size: 26rpx;
  color: #909399;
  margin-top: 10rpx;
}

/* 摄像头区域 */
.camera-area {
  width: 100%;
  height: 700rpx;
  border-radius: 20rpx;
  overflow: hidden;
  background: #000;
  margin-bottom: 30rpx;
  position: relative;
}

.camera {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.camera::-webkit-media-controls {
  display: none !important;
}

.camera::-webkit-media-controls-enclosure {
  display: none !important;
}

.camera::-webkit-media-controls-panel {
  display: none !important;
}

.camera::-webkit-media-controls-overlay-play-button {
  display: none !important;
}

.camera::-webkit-media-controls-volume-slider {
  display: none !important;
}

.camera::-webkit-media-controls-mute-button {
  display: none !important;
}

.camera::-webkit-media-controls-fullscreen-button {
  display: none !important;
}

.camera::-webkit-media-controls-timeline {
  display: none !important;
}

.camera-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 10;
  pointer-events: auto;
  background: transparent;
}

.preview {
  width: 100%;
  height: 100%;
}

.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.ph-icon {
  font-size: 80rpx;
  margin-bottom: 16rpx;
}

.ph-text {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.85);
}

/* 按钮行 */
.btn-row {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  margin-bottom: 30rpx;
}

.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 88rpx;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 500;
}

.primary {
  background: linear-gradient(135deg, #409EFF, #66b1ff);
  color: #fff;
  box-shadow: 0 8rpx 24rpx rgba(64, 158, 255, 0.3);
}

.reset {
  background: #fff;
  color: #606266;
  border: 2rpx solid #dcdfe6;
}

/* 加载遮罩 */
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.loading-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 60rpx 80rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
  box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.12);
}

.loading-spinner {
  font-size: 60rpx;
}

.loading-text {
  font-size: 28rpx;
  color: #606266;
}

/* 结果区域 */
.result-section {
  margin-top: 10rpx;
  display: none;
}
.result-section.visible {
  display: block;
}

.result-card {
  border-radius: 20rpx;
  padding: 40rpx 30rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
}

.result-card.pass {
  background: linear-gradient(135deg, #f0f9eb, #e1f3d8);
  border: 2rpx solid #b7eb8f;
}

.result-card.fail {
  background: linear-gradient(135deg, #fef0f0, #fde2e2);
  border: 2rpx solid #f5a3a3;
}

.result-icon {
  font-size: 56rpx;
}

.result-title {
  font-size: 34rpx;
  font-weight: bold;
}

.result-card.pass .result-title {
  color: #52c41a;
}

.result-card.fail .result-title {
  color: #f5222d;
}

.result-detail {
  width: 100%;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 12rpx;
  padding: 24rpx;
  margin-top: 8rpx;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10rpx 0;
}

.detail-row + .detail-row {
  border-top: 1rpx solid rgba(0, 0, 0, 0.06);
}

.detail-label {
  font-size: 26rpx;
  color: #909399;
}

.detail-value {
  font-size: 28rpx;
  color: #303133;
  font-weight: 500;
}

.fail-hint {
  font-size: 26rpx;
  color: #909399;
  display: block;
  text-align: center;
}

.result-time {
  font-size: 24rpx;
  color: #c0c4cc;
  margin-top: 4rpx;
}
</style>
