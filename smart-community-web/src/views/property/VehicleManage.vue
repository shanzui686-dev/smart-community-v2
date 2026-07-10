<template>
  <div class="page-container">
    <div class="page-header">
      <h2>车辆管理</h2>
    </div>

    <div class="search-toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchForm.keyword" placeholder="车牌号/车主姓名" clearable style="width:220px" @change="handleSearch" />
        <el-select v-model="searchForm.communityId" placeholder="选择小区" clearable style="width:160px" @change="handleSearch">
        <el-option v-for="c in communityList" :key="c.communityId" :label="c.name" :value="c.communityId" />
      </el-select>
      <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
      <el-button icon="Refresh" @click="handleReset">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" icon="Plus" @click="handleAdd">新增车辆</el-button>
      </div>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="plateNumber" label="车牌号" width="130" />
      <el-table-column prop="personName" label="车主姓名" width="100" />
      <el-table-column prop="mobile" label="车主电话" width="130" />
      <el-table-column prop="communityName" label="所属小区" width="140" />
      <el-table-column prop="houseNo" label="门牌号" width="120" />
      <el-table-column prop="vehicleType" label="车辆类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getVehicleTypeTagType(row.vehicleType)">
            {{ getVehicleTypeName(row.vehicleType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="hasParkingSpace" label="是否有车位" width="100">
        <template #default="{ row }">
          <el-switch v-model="row.hasParkingSpace" :active-value="1" :inactive-value="0" @change="(val) => handleHasParkingSpaceChange(row, val)" />
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="120" fixed="right">
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="车主" prop="personId">
          <el-select v-model="form.personId" placeholder="请选择车主" style="width:100%" filterable>
            <el-option v-for="p in personList" :key="p.personId" 
              :label="p.userName + ' (' + p.houseNo + ')'" :value="p.personId" />
          </el-select>
        </el-form-item>
        <el-form-item label="车牌号" prop="plateNumber">
          <el-input v-model="form.plateNumber" placeholder="请输入车牌号" />
        </el-form-item>
        <el-form-item label="车辆类型" prop="vehicleType">
          <el-select v-model="form.vehicleType" style="width:100%">
            <el-option label="摩托车" :value="1" />
            <el-option label="三轮车" :value="2" />
            <el-option label="电瓶车" :value="3" />
            <el-option label="家用车" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否有车位">
          <el-radio-group v-model="form.hasParkingSpace">
            <el-radio :value="1">有</el-radio>
            <el-radio :value="0">无</el-radio>
          </el-radio-group>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getVehicles, addVehicle, updateVehicle, deleteVehicle } from '@/api/property'
import { getAllCommunities } from '@/api/property'
import { getPersons } from '@/api/property'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const tableData = ref([])
const total = ref(0)
const communityList = ref([])
const personList = ref([])

const searchForm = reactive({ keyword: '', communityId: null, pageNum: 1, pageSize: 10 })

const form = reactive({
  vehicleId: '', personId: null, plateNumber: '', vehicleType: null,
  hasParkingSpace: 0, remark: ''
})

const rules = {
  personId: [{ required: true, message: '请选择车主', trigger: 'change' }],
  plateNumber: [{ required: true, message: '请输入车牌号', trigger: 'blur' }],
  vehicleType: [{ required: true, message: '请选择车辆类型', trigger: 'change' }]
}

const getVehicleTypeName = (type) => {
  const map = { 1: '摩托车', 2: '三轮车', 3: '电瓶车', 4: '家用车' }
  return map[type] || '未知'
}

const getVehicleTypeTagType = (type) => {
  const map = { 1: 'info', 2: 'warning', 3: 'success', 4: 'primary' }
  return map[type] || 'info'
}

const loadCommunities = async () => {
  const { data } = await getAllCommunities()
  communityList.value = data
}

const loadPersons = async () => {
  const { data } = await getPersons({ pageNum: 1, pageSize: 1000, personType: null })
  personList.value = data.records.filter(p => p.state === 1)
}

const loadData = async () => {
  loading.value = true
  try {
    const { data } = await getVehicles(searchForm)
    tableData.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

const handleSearch = () => { searchForm.pageNum = 1; loadData() }
const handleReset = () => {
  searchForm.keyword = ''; searchForm.communityId = null
  searchForm.pageNum = 1; loadData()
}

const handleAdd = () => {
  isEdit.value = false; dialogTitle.value = '新增车辆'; dialogVisible.value = true
  loadPersons()
}

const handleEdit = (row) => {
  isEdit.value = true; dialogTitle.value = '编辑车辆'
  Object.assign(form, row)
  loadPersons()
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该车辆？', '提示', { type: 'warning' })
  await deleteVehicle(row.vehicleId)
  ElMessage.success('删除成功'); loadData()
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, {
    vehicleId: '', personId: null, plateNumber: '', vehicleType: null,
    hasParkingSpace: 0, remark: ''
  })
}

const handleHasParkingSpaceChange = async (row, val) => {
  try {
    await updateVehicle({ vehicleId: row.vehicleId, hasParkingSpace: val })
    ElMessage.success(val === 1 ? '已分配车位' : '已取消车位')
    loadData()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (isEdit.value) { await updateVehicle(form) } else { await addVehicle(form) }
  ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
  dialogVisible.value = false; loadData()
}

onMounted(() => { loadCommunities(); loadData() })
</script>

<style scoped>
</style>