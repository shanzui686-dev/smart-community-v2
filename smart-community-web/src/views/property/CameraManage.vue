<template>
  <div class="page-container">
    <div class="page-header">
      <h2>摄像头管理</h2>
      <el-button type="primary" icon="Plus" @click="handleAdd">新增摄像头</el-button>
    </div>

    <div class="search-toolbar">
      <el-input v-model="searchForm.keyword" placeholder="名称/位置" clearable style="width:220px" @change="handleSearch" />
      <el-select v-model="searchForm.communityId" placeholder="选择小区" clearable style="width:160px" @change="handleSearch">
        <el-option v-for="c in communityList" :key="c.communityId" :label="c.name" :value="c.communityId" />
      </el-select>
      <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
      <el-button icon="Refresh" @click="handleReset">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe style="width:100%">
      <el-table-column prop="cameraId" label="ID" width="70" />
      <el-table-column prop="name" label="名称" min-width="120" />
      <el-table-column prop="communityName" label="所属小区" min-width="120" />
      <el-table-column prop="deviceCode" label="设备编码" min-width="120" />
      <el-table-column prop="ipAddress" label="IP地址" min-width="120" />
      <el-table-column prop="location" label="位置" min-width="120" />
      <el-table-column prop="deviceType" label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.deviceType===1?'success':'info'">
            {{ row.deviceType===1?'门禁':'监控' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="onlineStatus" label="在线状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.onlineStatus===1?'success':'danger'">{{ row.onlineStatus===1?'在线':'离线' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.status===1" type="success">正常</el-tag>
          <el-tag v-else-if="row.status===2" type="danger">损坏</el-tag>
          <el-tag v-else type="warning">维修</el-tag>
        </template>
      </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="所属小区" prop="communityId">
          <el-select v-model="form.communityId" style="width:100%">
            <el-option v-for="c in communityList" :key="c.communityId" :label="c.name" :value="c.communityId" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="设备编码" prop="deviceCode">
          <el-input v-model="form.deviceCode" />
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input v-model="form.ipAddress" />
        </el-form-item>
        <el-form-item label="安装位置">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="视频流地址">
          <el-input v-model="form.streamUrl" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.deviceType">
            <el-radio :value="1">门禁摄像头</el-radio>
            <el-radio :value="2">监控摄像头</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="在线状态">
          <el-radio-group v-model="form.onlineStatus">
            <el-radio :value="1">在线</el-radio>
            <el-radio :value="0">离线</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="2">损坏</el-radio>
            <el-radio :value="3">维修</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCameras, addCamera, updateCamera, deleteCamera, getAllCommunities } from '@/api/property'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const tableData = ref([])
const total = ref(0)
const communityList = ref([])

const searchForm = reactive({ keyword: '', communityId: null, pageNum: 1, pageSize: 10 })

const form = reactive({
  cameraId: '', communityId: null, name: '', deviceCode: '', ipAddress: '', location: '', streamUrl: '', deviceType: 1, onlineStatus: 1, status: 1
})

const rules = {
  communityId: [{ required: true, message: '请选择小区', trigger: 'change' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  deviceCode: [{ required: true, message: '请输入设备编码', trigger: 'blur' }]
}

const loadCommunities = async () => {
  const { data } = await getAllCommunities()
  communityList.value = data
}

const loadData = async () => {
  loading.value = true
  try {
    const { data } = await getCameras(searchForm)
    tableData.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

const handleSearch = () => { searchForm.pageNum = 1; loadData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.communityId = null; searchForm.pageNum = 1; loadData() }

const handleAdd = () => { isEdit.value = false; dialogTitle.value = '新增摄像头'; dialogVisible.value = true }
const handleEdit = (row) => { isEdit.value = true; dialogTitle.value = '编辑摄像头'; Object.assign(form, row); dialogVisible.value = true }

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该摄像头？', '提示', { type: 'warning' })
  await deleteCamera(row.cameraId)
  ElMessage.success('删除成功'); loadData()
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, { cameraId: '', communityId: null, name: '', deviceCode: '', ipAddress: '', location: '', streamUrl: '', deviceType: 1, onlineStatus: 1, status: 1 })
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (isEdit.value) { await updateCamera(form) } else { await addCamera(form) }
  ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
  dialogVisible.value = false; loadData()
}

onMounted(() => { loadCommunities(); loadData() })
</script>
