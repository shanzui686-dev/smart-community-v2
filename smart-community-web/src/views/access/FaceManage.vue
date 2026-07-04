<template>
  <div class="page-container">
    <div class="page-header">
      <h2>人脸识别</h2>
    </div>

    <!-- 摄像头选择 -->
    <div class="search-toolbar">
      <span style="margin-right:8px;font-weight:500">门禁摄像头：</span>
      <el-select v-model="selectedCameraId" placeholder="选择门禁摄像头" style="width:260px" @change="onCameraChange">
        <el-option v-for="cam in doorCameras" :key="cam.cameraId" :label="cam.name + ' (' + cam.location + ')'" :value="cam.cameraId" />
      </el-select>
      <el-tag v-if="cameraDirection" :type="cameraDirection===1?'success':'warning'" style="margin-left:12px">
        {{ cameraDirection === 1 ? '进入' : '外出' }}
      </el-tag>
      <span v-if="!selectedCameraId" style="color:#909399;margin-left:8px;font-size:13px">
        1号摄像头为进入，2号摄像头为外出
      </span>
    </div>

    <el-row :gutter="20">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header><span>摄像头抓拍</span></template>

          <div v-if="!cameraOpen" class="camera-placeholder" @click="openCamera">
            <el-icon :size="60" color="#c0c4cc"><Camera /></el-icon>
            <p style="color:#909399;margin-top:12px;font-size:15px">点击打开摄像头</p>
          </div>

          <div v-else class="camera-area">
            <video ref="videoRef" autoplay playsinline class="camera-video" />
            <canvas ref="canvasRef" style="display:none" />
            <div class="camera-actions">
              <el-button @click="closeCamera">关闭摄像头</el-button>
              <el-button type="primary" :icon="Camera" @click="takeSnapshot" :loading="capturing"
                :disabled="!selectedCameraId">
                抓拍并识别
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span>识别结果</span></template>

          <div v-if="!capturedImage && !identifying && !result" class="result-placeholder">
            <el-icon :size="50" color="#c0c4cc"><Search /></el-icon>
            <p style="color:#909399;margin-top:10px">请先选择摄像头，再拍照识别</p>
          </div>

          <div v-if="capturedImage" class="captured-preview">
            <el-image :src="capturedImage" style="width:100%;max-height:220px;border-radius:6px" fit="contain" />
          </div>

          <div v-if="identifying" style="text-align:center;padding:30px 0">
            <el-icon class="is-loading" :size="40" color="#409eff"><Loading /></el-icon>
            <p style="color:#409eff;margin-top:10px;font-size:15px">正在识别...</p>
          </div>

          <div v-if="result" class="result-display">
            <div v-if="result.matched" class="result-success">
              <el-result icon="success" :title="`识别成功 — 本小区居民 (${cameraDirection===1?'进入':'外出'}, 置信度${result.score}%)`">
                <template #sub-title>
                  <el-descriptions :column="1" border size="small">
                    <el-descriptions-item label="姓名">{{ result.personName || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="手机号">{{ result.personMobile || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="门牌号">{{ result.personHouseNo || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="人员类型">{{ result.personType || '-' }}</el-descriptions-item>
                    <el-descriptions-item label="摄像头">{{ selectedCameraName }}</el-descriptions-item>
                    <el-descriptions-item label="出入记录">
                      <el-tag :type="result.recorded?'success':'danger'">{{ result.recorded ? '已生成' : '未生成' }}</el-tag>
                    </el-descriptions-item>
                  </el-descriptions>
                </template>
              </el-result>
            </div>

            <div v-else class="result-fail">
              <el-result icon="warning" title="非本小区居民或置信度不足" sub-title="置信度需大于80%才视为本小区居民并生成出入记录" />
            </div>

            <div style="text-align:center;margin-top:16px">
              <el-button type="primary" @click="resetRecognition">重新识别</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 手动上传识别 -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header><span>手动上传识别</span></template>
          <div style="display:flex;align-items:center;gap:16px;flex-wrap:wrap">
            <el-upload :show-file-list="false" :http-request="handleManualUpload" accept="image/*">
              <el-button :icon="Upload">选择照片</el-button>
            </el-upload>
            <span v-if="manualImage" style="color:#409eff">已选择照片</span>
            <el-button v-if="manualImage" type="primary" @click="handleManualSearch" :loading="manualSearching"
              :disabled="!selectedCameraId">
              开始识别
            </el-button>
          </div>
          <div v-if="manualResult" style="margin-top:12px">
            <el-alert
              v-if="manualResult.matched"
              :title="`匹配成功：${manualResult.personName || '居民'} (置信度 ${manualResult.score}%) — 已记录${cameraDirection===1?'进入':'外出'}`"
              type="success" :closable="false" show-icon
            />
            <el-alert v-else title="未匹配到居民" type="warning" :closable="false" show-icon />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile } from '@/api/system'
import { getCameras } from '@/api/property'
import request from '@/utils/request'

// ==================== 摄像头选择 ====================
const doorCameras = ref([])
const selectedCameraId = ref(null)
const selectedCamera = computed(() => doorCameras.value.find(c => c.cameraId === selectedCameraId.value))
const selectedCameraName = computed(() => selectedCamera.value?.name || '')
const cameraDirection = computed(() => {
  const cam = selectedCamera.value
  if (!cam || !cam.deviceCode) return null
  // 门禁摄像头：设备编码含"1号"为进入，含"2号"为外出
  if (cam.deviceCode.includes('1号') || cam.deviceCode.includes('1') || cam.name.includes('1号') || cam.name.includes('1')) return 1
  if (cam.deviceCode.includes('2号') || cam.deviceCode.includes('2') || cam.name.includes('2号') || cam.name.includes('2')) return 2
  return 1 // 默认进入
})

const onCameraChange = () => {
  if (cameraDirection.value) {
    ElMessage.info(`当前摄像头方向：${cameraDirection.value === 1 ? '进入' : '外出'}`)
  }
}

// ==================== 摄像头 ====================
const cameraOpen = ref(false)
const videoRef = ref(null)
const canvasRef = ref(null)
const capturing = ref(false)
let mediaStream = null

const openCamera = async () => {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({
      video: { width: 640, height: 480, facingMode: 'user' }
    })
    cameraOpen.value = true
    await nextTick()
    if (videoRef.value) videoRef.value.srcObject = mediaStream
  } catch {
    ElMessage.error('无法访问摄像头，请检查权限')
  }
}

const closeCamera = () => {
  if (mediaStream) { mediaStream.getTracks().forEach(t => t.stop()); mediaStream = null }
  cameraOpen.value = false
}

// ==================== 识别流程 ====================
const capturedImage = ref('')
const identifying = ref(false)
const result = ref(null)

const takeSnapshot = async () => {
  if (!videoRef.value || !canvasRef.value) return
  capturing.value = true

  const video = videoRef.value
  const canvas = canvasRef.value
  canvas.width = video.videoWidth || 640
  canvas.height = video.videoHeight || 480
  canvas.getContext('2d').drawImage(video, 0, 0, canvas.width, canvas.height)

  try {
    const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.9))
    const file = new File([blob], 'face_snapshot.jpg', { type: 'image/jpeg' })
    const url = await uploadImageToOss(file, 'face_capture')
    if (!url) return

    capturedImage.value = url
    identifying.value = true

    const searchData = await searchFace(url)
    if (searchData && searchData.matched) {
      const personInfo = await fetchPersonInfo(searchData.personId)
      // 记录出入
      let recorded = false
      if (selectedCameraId.value && cameraDirection.value) {
        recorded = await recordAccess(personInfo, url)
      }
      result.value = {
        matched: true, personId: searchData.personId, score: searchData.score,
        personName: personInfo?.userName || '-', personMobile: personInfo?.mobile || '-',
        personHouseNo: personInfo?.houseNo || '-', personType: personInfo?.personTypeName || '-',
        recorded
      }
    } else {
      result.value = { matched: false }
    }
  } catch (err) {
    console.error('识别失败:', err)
    ElMessage.error('识别过程出错')
  } finally {
    identifying.value = false
    capturing.value = false
  }
}

const resetRecognition = () => { capturedImage.value = ''; result.value = null }

// ==================== 手动上传识别 ====================
const manualImage = ref('')
const manualSearching = ref(false)
const manualResult = ref(null)

const handleManualUpload = async (options) => {
  const url = await uploadImageToOss(options.file, 'manual_search')
  if (url) manualImage.value = url
}

const handleManualSearch = async () => {
  if (!manualImage.value) return
  manualSearching.value = true
  try {
    const data = await searchFace(manualImage.value)
    if (data && data.matched) {
      const personInfo = await fetchPersonInfo(data.personId)
      if (selectedCameraId.value && cameraDirection.value) {
        await recordAccess(personInfo, manualImage.value)
      }
      manualResult.value = { matched: true, personName: personInfo?.userName || '-', score: data.score }
    } else {
      manualResult.value = { matched: false }
    }
  } finally { manualSearching.value = false }
}

// ==================== 工具函数 ====================
const uploadImageToOss = async (file, dir) => {
  const formData = new FormData()
  formData.append('file', file); formData.append('dir', dir); formData.append('bucket', 'face')
  try { const { data } = await uploadFile(formData); return data }
  catch { ElMessage.error('图片上传失败'); return null }
}

const searchFace = async (imageUrl) => {
  try { const { data } = await request.post('/face/search', null, { params: { imageUrl } }); return data }
  catch { ElMessage.error('人脸搜索失败'); return null }
}

const fetchPersonInfo = async (personId) => {
  try {
    const { data } = await request.get(`/property/person/${personId}`)
    if (data) data.personTypeName = { 1: '业主', 2: '租户', 3: '家属' }[data.personType] || '未知'
    return data
  } catch { return null }
}

const recordAccess = async (personInfo, photoUrl) => {
  try {
    await request.post('/face/record', null, {
      params: {
        personName: personInfo?.userName || '',
        communityId: personInfo?.communityId || 0,
        houseNo: personInfo?.houseNo || '',
        cameraId: selectedCameraId.value,
        photoUrl: photoUrl,
        direction: cameraDirection.value
      }
    })
    return true
  } catch { return false }
}

// ==================== 初始化 ====================
onMounted(async () => {
  try {
    const { data } = await getCameras({ pageSize: 100 })
    doorCameras.value = (data.records || []).filter(c => c.deviceType === 1 && c.status === 1 && c.onlineStatus === 1)
  } catch {}
})
</script>

<style scoped>
.search-toolbar {
  background: #fff; padding: 12px 16px; border-radius: 6px;
  margin-bottom: 16px; display: flex; align-items: center;
  border: 1px solid #ebeef5;
}
.camera-placeholder {
  width: 100%; min-height: 320px; border: 2px dashed #d9d9d9; border-radius: 8px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  cursor: pointer; transition: border-color 0.3s;
}
.camera-placeholder:hover { border-color: #409eff; }
.camera-area { text-align: center; }
.camera-video { width: 100%; max-height: 380px; background: #000; border-radius: 6px; }
.camera-actions { margin-top: 12px; display: flex; gap: 12px; justify-content: center; }
.result-placeholder { text-align: center; padding: 40px 0; }
.captured-preview { background: #f5f5f5; border-radius: 6px; padding: 8px; text-align: center; }
.result-display { text-align: center; }
.result-success, .result-fail { margin-top: 8px; }
</style>
