<template>
  <div class="page-container">
    <div class="page-header">
      <h2>访客登记</h2>
      <el-button type="primary" icon="Plus" @click="handleAdd">访客登记</el-button>
    </div>

    <div class="search-toolbar">
      <el-input v-model="searchForm.name" placeholder="访客姓名" clearable style="width:160px" @change="handleSearch" />
      <el-select v-model="searchForm.communityId" placeholder="访问小区" clearable style="width:160px" @change="handleSearch">
        <el-option v-for="c in communityList" :key="c.communityId" :label="c.name" :value="c.communityId" />
      </el-select>
      <el-select v-model="searchForm.status" placeholder="状态" clearable style="width:130px" @change="handleSearch">
        <el-option label="已预约" :value="1" />
        <el-option label="已到访" :value="2" />
        <el-option label="已离开" :value="3" />
        <el-option label="已取消" :value="4" />
      </el-select>
      <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
      <el-button icon="Refresh" @click="handleReset">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="visitorId" label="ID" width="70" />
      <el-table-column prop="name" label="访客姓名" width="100" />
      <el-table-column prop="mobile" label="手机号" width="130" />
      <el-table-column prop="idCard" label="身份证号" width="180" />
      <el-table-column prop="communityName" label="访问小区" width="140" />
      <el-table-column prop="houseNo" label="访问门牌" width="120" />
      <el-table-column prop="visitTime" label="预约时间" width="170" />
      <el-table-column label="签到时间" width="170">
        <template #default="{ row }">
          {{ row.checkInTime || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="leaveTime" label="签退时间" width="170">
        <template #default="{ row }">
          {{ row.leaveTime || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="事由" min-width="150" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.status===1" type="info">已预约</el-tag>
          <el-tag v-else-if="row.status===2" type="primary">已到访</el-tag>
          <el-tag v-else-if="row.status===3" type="success">已离开</el-tag>
          <el-tag v-else type="danger">已取消</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status===1" type="success" link @click="handleCheckIn(row)">签到</el-button>
          <el-button v-if="row.status===2" type="warning" link @click="handleCheckOut(row)">签退</el-button>
          <el-button v-if="row.status===1" type="danger" link @click="handleCancel(row)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top:16px;display:flex;justify-content:flex-end">
      <el-pagination v-model:current-page="searchForm.pageNum" v-model:page-size="searchForm.pageSize"
        :page-sizes="[10,20,50]" :total="total" layout="total,sizes,prev,pager,next" @change="loadData" />
    </div>

    <el-dialog v-model="dialogVisible" title="访客登记" width="550px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="访客姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="手机号" prop="mobile">
          <el-input v-model="form.mobile" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="form.idCard" />
        </el-form-item>
        <el-form-item label="访问小区" prop="communityId">
          <el-select v-model="form.communityId" style="width:100%">
            <el-option v-for="c in communityList" :key="c.communityId" :label="c.name" :value="c.communityId" />
          </el-select>
        </el-form-item>
        <el-form-item label="访问门牌">
          <el-input v-model="form.houseNo" />
        </el-form-item>
        <el-form-item label="预约时间" prop="visitTime">
          <el-date-picker v-model="form.visitTime" type="datetime" placeholder="选择时间" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="来访事由">
          <el-input v-model="form.reason" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="车牌号">
          <el-input v-model="form.plateNumber" />
        </el-form-item>
        <el-form-item label="来访人数">
          <el-input-number v-model="form.visitorCount" :min="1" :max="20" />
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
import { getVisitors, addVisitor, updateVisitor, cancelVisitor, checkInVisitor, checkOutVisitor } from '@/api/access'
import { getAllCommunities } from '@/api/property'

const loading = ref(false)
const dialogVisible = ref(false)
const formRef = ref(null)
const tableData = ref([])
const total = ref(0)
const communityList = ref([])

const searchForm = reactive({ name: '', communityId: null, status: null, pageNum: 1, pageSize: 10 })

const form = reactive({
  visitorId: '', name: '', mobile: '', idCard: '', communityId: null, houseNo: '', visitTime: '', reason: '', plateNumber: '', visitorCount: 1
})

const rules = {
  name: [{ required: true, message: '请输入访客姓名', trigger: 'blur' }],
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  communityId: [{ required: true, message: '请选择小区', trigger: 'change' }],
  visitTime: [{ required: true, message: '请选择预约时间', trigger: 'change' }]
}

const loadCommunities = async () => {
  const { data } = await getAllCommunities()
  communityList.value = data
}

const loadData = async () => {
  loading.value = true
  try {
    const { data } = await getVisitors(searchForm)
    tableData.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

const handleSearch = () => { searchForm.pageNum = 1; loadData() }
const handleReset = () => {
  searchForm.name = ''; searchForm.communityId = null; searchForm.status = null
  searchForm.pageNum = 1; loadData()
}

const handleAdd = () => { dialogVisible.value = true }

const handleCheckIn = async (row) => {
  await checkInVisitor(row.visitorId)
  ElMessage.success('签到成功'); loadData()
}

const handleCheckOut = async (row) => {
  await checkOutVisitor(row.visitorId)
  ElMessage.success('签退成功'); loadData()
}

const handleCancel = async (row) => {
  await ElMessageBox.confirm('确认取消该预约？', '提示', { type: 'warning' })
  await cancelVisitor(row.visitorId)
  ElMessage.success('已取消'); loadData()
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, { visitorId: '', name: '', mobile: '', idCard: '', communityId: null, houseNo: '', visitTime: '', reason: '', plateNumber: '', visitorCount: 1 })
}

const handleSubmit = async () => {
  await formRef.value.validate()
  await addVisitor(form)
  ElMessage.success('登记成功')
  dialogVisible.value = false; loadData()
}

onMounted(() => { loadCommunities(); loadData() })
</script>
