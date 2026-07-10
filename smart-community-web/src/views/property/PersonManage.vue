<template>
  <div class="page-container">
    <div class="page-header">
      <h2>居民管理</h2>
    </div>

    <div class="search-toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchForm.keyword" placeholder="姓名/手机号/门牌号" clearable style="width:220px" @change="handleSearch" />
        <el-select v-model="searchForm.communityId" placeholder="选择小区" clearable style="width:160px" @change="handleSearch">
          <el-option v-for="c in communityList" :key="c.communityId" :label="c.name" :value="c.communityId" />
        </el-select>
        <el-select v-model="searchForm.personType" placeholder="人员类型" clearable style="width:130px" @change="handleSearch">
          <el-option label="业主" :value="1" />
          <el-option label="租户" :value="2" />
          <el-option label="家属" :value="3" />
        </el-select>
        <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
        <el-button icon="Refresh" @click="handleReset">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" icon="Plus" @click="handleAdd">新增居民</el-button>
        <el-button type="primary" icon="Upload" @click="triggerImport">批量导入</el-button>
        <el-button type="success" icon="Download" @click="handleExportSelected" :disabled="selectedRows.length === 0">
          导出选中 ({{ selectedRows.length }})
        </el-button>
        <el-button type="warning" icon="Download" @click="handleExportAll">批量导出</el-button>
      </div>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="personId" label="ID" width="70" />
      <el-table-column label="人脸照片" width="80">
        <template #default="{ row }">
          <el-avatar v-if="row.faceUrl" :src="row._signedFaceUrl || row.faceUrl" :size="45" shape="square" />
          <span v-else style="color:#ccc;font-size:12px">暂无</span>
        </template>
      </el-table-column>
      <el-table-column prop="userName" label="姓名" width="100" />
      <el-table-column prop="mobile" label="手机号" width="130" />
      <el-table-column prop="sex" label="性别" width="70">
        <template #default="{ row }">{{ row.sex === 1 ? '男' : '女' }}</template>
      </el-table-column>
      <el-table-column prop="communityName" label="所属小区" width="140" />
      <el-table-column prop="houseNo" label="门牌号" width="120" />
      <el-table-column prop="personType" label="人员类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.personType===1?'success':row.personType===2?'warning':'info'">
            {{ row.personType===1?'业主':row.personType===2?'租户':'家属' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="state" label="在住状态" width="90">
        <template #default="{ row }">
          <el-switch v-model="row.state" :active-value="1" :inactive-value="0" @change="(val) => handleStateChange(row, val)" />
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top:16px;display:flex;justify-content:flex-end">
      <el-pagination v-model:current-page="searchForm.pageNum" v-model:page-size="searchForm.pageSize"
        :page-sizes="[10,20,50]" :total="total" layout="total,sizes,prev,pager,next" @change="loadData" />
    </div>

    <!-- 新增/编辑居民 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="580px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="所属小区" prop="communityId">
          <el-select v-model="form.communityId" style="width:100%">
            <el-option v-for="c in communityList" :key="c.communityId" :label="c.name" :value="c.communityId" />
          </el-select>
        </el-form-item>
        <el-form-item label="姓名" prop="userName">
          <el-input v-model="form.userName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.mobile" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.sex">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="门牌号">
          <el-input v-model="form.houseNo" />
        </el-form-item>
        <el-form-item label="人员类型">
          <el-select v-model="form.personType">
            <el-option label="业主" :value="1" />
            <el-option label="租户" :value="2" />
            <el-option label="家属" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="在住状态">
          <el-radio-group v-model="form.state">
            <el-radio :value="1">在住</el-radio>
            <el-radio :value="0">迁出</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="人脸照片">
          <div class="face-upload-area">
            <div v-if="form.faceUrl" class="face-preview">
              <el-image :src="form._signedFaceUrl || form.faceUrl" style="width:120px;height:120px;border-radius:4px" fit="cover" />
              <div class="face-preview-actions">
                <el-button size="small" @click="triggerUpload">重新上传</el-button>
                <el-button size="small" type="primary" @click="openCamera">拍照</el-button>
                <el-button size="small" type="danger" @click="form.faceUrl = ''">删除</el-button>
              </div>
            </div>
            <div v-else class="face-upload-placeholder">
              <el-icon :size="40" style="color:#c0c4cc"><Camera /></el-icon>
              <p style="color:#909399;margin:8px 0">上传人脸照片或拍照</p>
              <div>
                <el-button size="small" type="primary" @click="triggerUpload">上传照片</el-button>
                <el-button size="small" @click="openCamera">摄像头拍照</el-button>
              </div>
            </div>
            <!-- 隐藏的文件上传 -->
            <input ref="fileInputRef" type="file" accept="image/*" style="display:none" @change="handleFileChange" />
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入文件选择 -->
    <input ref="importInputRef" type="file" accept=".xlsx,.xls" style="display:none" @change="handleImportFile" />

    <!-- 摄像头拍照对话框 -->
    <el-dialog v-model="cameraVisible" title="拍照" width="500px" @close="stopCamera">
      <div class="camera-container">
        <video ref="videoRef" autoplay playsinline style="width:100%;border-radius:4px;background:#000" />
        <canvas ref="canvasRef" style="display:none" />
      </div>
      <template #footer>
        <el-button @click="cameraVisible = false">取消</el-button>
        <el-button type="primary" @click="takeSnapshot" :loading="capturing">拍照并上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPersons, addPerson, updatePerson, deletePerson, importPersons, faceSearch, faceCompare } from '@/api/property'
import { getAllCommunities } from '@/api/property'
import { uploadFile } from '@/api/system'
import { getSignedUrl } from '@/utils/oss'
import request from '@/utils/request'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const tableData = ref([])
const total = ref(0)
const communityList = ref([])

// 多选
const selectedRows = ref([])

// 导入
const importInputRef = ref(null)

// 拍照相关
const cameraVisible = ref(false)
const videoRef = ref(null)
const canvasRef = ref(null)
const fileInputRef = ref(null)
const capturing = ref(false)
let mediaStream = null

const searchForm = reactive({ keyword: '', communityId: null, personType: null, pageNum: 1, pageSize: 10 })

const form = reactive({
  personId: '', communityId: null, userName: '', mobile: '', sex: 1,
  houseNo: '', personType: 1, state: 1, faceUrl: '', remark: ''
})

const rules = {
  communityId: [{ required: true, message: '请选择小区', trigger: 'change' }],
  userName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  mobile: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
}

const loadCommunities = async () => {
  const { data } = await getAllCommunities()
  communityList.value = data
}

const loadData = async () => {
  loading.value = true
  try {
    const { data } = await getPersons(searchForm)
    // 为每条记录获取人脸照片的签名 URL
    for (const record of data.records) {
      if (record.faceUrl) {
        record._signedFaceUrl = await getSignedUrl(record.faceUrl)
      }
    }
    tableData.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

const handleSearch = () => { searchForm.pageNum = 1; loadData() }
const handleReset = () => {
  searchForm.keyword = ''; searchForm.communityId = null; searchForm.personType = null
  searchForm.pageNum = 1; loadData()
}

const handleAdd = () => {
  isEdit.value = false; dialogTitle.value = '新增居民'; dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true; dialogTitle.value = '编辑居民'
  Object.assign(form, row)
  // 获取签名 URL 用于预览
  if (form.faceUrl) {
    getSignedUrl(form.faceUrl).then(url => { form._signedFaceUrl = url })
  }
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该居民？', '提示', { type: 'warning' })
  await deletePerson(row.personId)
  ElMessage.success('删除成功'); loadData()
}

// ==================== 导出 ====================
const handleSelectionChange = (rows) => { selectedRows.value = rows }

const handleExportSelected = () => {
  if (selectedRows.value.length === 0) return
  const ids = selectedRows.value.map(r => r.personId).join(',')
  downloadExcel({ ids })
}

const handleExportAll = () => {
  downloadExcel({
    keyword: searchForm.keyword || undefined,
    communityId: searchForm.communityId || undefined,
    personType: searchForm.personType || undefined
  })
}

const downloadExcel = async (params) => {
  try {
    const response = await request.get('/property/person/export', {
      params,
      responseType: 'blob'
    })
    const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '居民信息.xlsx'
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

// ==================== 导入 ====================
const triggerImport = () => { importInputRef.value?.click() }

const handleImportFile = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  e.target.value = ''
  const formData = new FormData()
  formData.append('file', file)
  try {
    await importPersons(formData)
    ElMessage.success('导入成功')
    loadData()
  } catch {
    ElMessage.error('导入失败，请检查文件格式')
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, {
    personId: '', communityId: null, userName: '', mobile: '', sex: 1,
    houseNo: '', personType: 1, state: 1, faceUrl: '', _signedFaceUrl: '', remark: ''
  })
}

const handleStateChange = async (row, val) => {
  try {
    await updatePerson({ personId: row.personId, state: val })
    ElMessage.success(val === 1 ? '已在住' : '已迁出')
    loadData()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  try {
    if (isEdit.value) { await updatePerson(form) } else { await addPerson(form) }
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } catch (e) {
    // 后端人脸查重失败等错误，axios 拦截器已显示 ElMessage.error
    // 此处不关闭弹窗，让用户修改后重新提交
  }
}

// ==================== 人脸照片上传 ====================

const triggerUpload = () => {
  fileInputRef.value?.click()
}

const handleFileChange = async (e) => {
  const file = e.target.files[0]
  if (!file) return
  await uploadFaceFile(file)
  // 清空 input 以支持重复选择同一文件
  e.target.value = ''
}

const uploadFaceFile = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('dir', 'face')
  formData.append('bucket', 'face')

  // 上传文件到 OSS
  let uploadedUrl
  try {
    ElMessage.info('正在上传照片...')
    const uploadRes = await uploadFile(formData)
    uploadedUrl = uploadRes.data
  } catch {
    ElMessage.error('文件上传失败，请检查网络后重试')
    return
  }

  // ========== 第一步：1:N 人脸搜索查重（编辑和新增都需要） ==========
  ElMessage.info('正在与已有居民进行人脸查重...')
  try {
    const searchRes = await faceSearch(uploadedUrl)
    const result = searchRes.data
    if (result && result.matched && (Number(result.score) || 0) > 90) {
      // matchedId 来自百度 AI 是 String，form.personId 可能是 Number，统一转 String 比较
      const matchedId = String(result.personId)
      const currentId = String(form.personId)
      // 编辑模式：匹配到的是本人 → 允许（同一人换照片）
      if (isEdit.value && matchedId === currentId) {
        // 放行，继续执行后续逻辑
      } else {
        const score = Number(result.score) || 0
        ElMessage.warning(
          `该人脸已存在（匹配居民 ID: ${matchedId}，置信度 ${score.toFixed(2)}%），请勿重复录入`
        )
        return
      }
    }
  } catch (e) {
    console.error('人脸查重失败:', e)
    // 搜索异常（图片质量等问题），阻断上传，注册时也会失败
    ElMessage.error(e.message || '人脸查重服务异常，请重试')
    return
  }

  // ========== 第二步：编辑模式下额外做 1:1 本人确认 ==========
  if (isEdit.value) {
    const oldFaceUrl = form._signedFaceUrl || form.faceUrl
    if (oldFaceUrl) {
      ElMessage.info('正在与原有照片进行本人确认...')
      try {
        const compareRes = await faceCompare(uploadedUrl, oldFaceUrl)
        const cmp = compareRes.data
        const score = cmp ? (Number(cmp.score) || 0) : 0
        if (!cmp || !cmp.passed || score < 95) {
          ElMessage.warning(`本人确认未通过（置信度 ${score.toFixed(2)}%，需要 > 95%），请重新上传本人照片`)
          return
        }
      } catch (e) {
        console.error('人脸比对失败:', e)
        ElMessage.warning('人脸比对服务异常，请重试')
        return
      }
    }
  }

  // ========== 全部校验通过，接受照片 ==========
  try {
    if (isEdit.value) {
      form._oldFaceUrl = form._signedFaceUrl || form.faceUrl
    }
    form.faceUrl = uploadedUrl
    form._signedFaceUrl = await getSignedUrl(uploadedUrl)
    ElMessage.success('人脸照片上传成功')
  } catch {
    ElMessage.error('照片处理失败，请重试')
  }
}

// ==================== 摄像头拍照 ====================

const openCamera = async () => {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({
      video: { width: 1280, height: 720, facingMode: 'user' }
    })
    cameraVisible.value = true
    // 等 DOM 更新后绑定视频流
    setTimeout(() => {
      if (videoRef.value) {
        videoRef.value.srcObject = mediaStream
      }
    }, 100)
  } catch (err) {
    console.error('摄像头打开失败:', err)
    ElMessage.error('无法访问摄像头，请检查权限设置')
  }
}

const stopCamera = () => {
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
    mediaStream = null
  }
}

const takeSnapshot = async () => {
  if (!videoRef.value || !canvasRef.value) return

  capturing.value = true
  try {
    const video = videoRef.value
    const canvas = canvasRef.value
    canvas.width = video.videoWidth || 640
    canvas.height = video.videoHeight || 480

    const ctx = canvas.getContext('2d')
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height)

    // canvas → blob → File
    const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.9))
    const file = new File([blob], 'face_capture.jpg', { type: 'image/jpeg' })

    await uploadFaceFile(file)
    cameraVisible.value = false
  } catch (err) {
    console.error('拍照失败:', err)
    ElMessage.error('拍照失败')
  } finally {
    capturing.value = false
  }
}

onMounted(() => { loadCommunities(); loadData() })
</script>

<style scoped>
.face-upload-area {
  display: flex;
  align-items: center;
}
.face-preview {
  text-align: center;
}
.face-preview-actions {
  margin-top: 8px;
  display: flex;
  gap: 6px;
  justify-content: center;
}
.face-upload-placeholder {
  width: 240px;
  height: 140px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.3s;
}
.face-upload-placeholder:hover {
  border-color: #409eff;
}
.camera-container {
  background: #000;
  border-radius: 4px;
  overflow: hidden;
}
</style>
