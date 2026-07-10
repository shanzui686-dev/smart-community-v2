<template>
  <div class="page-container">
    <div class="page-header">
      <h2>收费标准管理</h2>
    </div>

    <div class="search-toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchForm.keyword" placeholder="费用名称" clearable style="width:200px" @change="handleSearch" />
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width:120px" @change="handleSearch">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
        <el-button icon="Refresh" @click="handleReset">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" icon="Plus" @click="openAddDialog">新增收费标准</el-button>
      </div>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe style="width:100%">
      <el-table-column prop="feeStandardId" label="标准编号" width="90" />
      <el-table-column prop="feeName" label="费用名称" min-width="180" />
      <el-table-column prop="calculationMethod" label="计算方式" width="160">
        <template #default="{ row }">
          <el-tag :type="getCalcMethodType(row.calculationMethod)" size="default">
            {{ getCalcMethodName(row.calculationMethod) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="unitPrice" label="单价" width="130">
        <template #default="{ row }">
          <span style="font-weight:600;color:#e6a23c">￥ {{ row.unitPrice?.toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch
            v-model="row.status"
            :active-value="1"
            :inactive-value="0"
            @change="(val) => handleStatusChange(row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top:16px;display:flex;justify-content:flex-end">
      <el-pagination
        v-model:current-page="searchForm.pageNum"
        v-model:page-size="searchForm.pageSize"
        :page-sizes="[10,20,50]"
        :total="total"
        layout="total,sizes,prev,pager,next"
        @change="loadData"
      />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="费用名称" prop="feeName">
          <el-input v-model="form.feeName" placeholder="请输入费用名称" />
        </el-form-item>
        <el-form-item label="计算方式" prop="calculationMethod">
          <el-select v-model="form.calculationMethod" placeholder="请选择计算方式" style="width:100%">
            <el-option label="按面积/月" :value="1" />
            <el-option label="按固定金额/月" :value="2" />
            <el-option label="按用量计费" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="单价" prop="unitPrice">
          <el-input-number
            v-model="form.unitPrice"
            :min="0.01"
            :max="999999"
            :precision="2"
            :step="0.5"
            style="width:100%"
            placeholder="请输入单价"
          >
            <template #prefix>￥</template>
          </el-input-number>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="可选填备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFeeStandardList, addFeeStandard, updateFeeStandard, deleteFeeStandard } from '@/api/payment'

// ==================== 数据状态 ====================
const loading = ref(false)
const tableData = ref([])
const total = ref(0)

const searchForm = reactive({
  keyword: '',
  status: null,
  pageNum: 1,
  pageSize: 10
})

// ==================== 对话框 ====================
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)

const form = reactive({
  feeStandardId: '',
  feeName: '',
  calculationMethod: null,
  unitPrice: null,
  status: 1,
  remark: ''
})

const rules = {
  feeName: [{ required: true, message: '请输入费用名称', trigger: 'blur' }],
  calculationMethod: [{ required: true, message: '请选择计算方式', trigger: 'change' }],
  unitPrice: [
    { required: true, message: '请输入单价', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '单价必须大于 0', trigger: 'blur' }
  ]
}

// ==================== 辅助函数 ====================
const getCalcMethodName = (method) => {
  const map = { 1: '按面积/月', 2: '按固定金额/月', 3: '按用量计费' }
  return map[method] || '未知'
}

const getCalcMethodType = (method) => {
  const map = { 1: 'primary', 2: 'success', 3: 'warning' }
  return map[method] || 'info'
}

// ==================== CRUD ====================
const loadData = async () => {
  loading.value = true
  try {
    const params = { ...searchForm }
    if (!params.keyword) delete params.keyword
    if (params.status === null || params.status === '') delete params.status
    const { data } = await getFeeStandardList(params)
    tableData.value = data.records || []
    total.value = data.total || 0
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { searchForm.pageNum = 1; loadData() }

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = null
  searchForm.pageNum = 1
  loadData()
}

const openAddDialog = () => {
  isEdit.value = false
  dialogTitle.value = '新增收费标准'
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑收费标准'
  Object.assign(form, row)
  dialogVisible.value = true
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, {
    feeStandardId: '', feeName: '', calculationMethod: null, unitPrice: null, status: 1, remark: ''
  })
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateFeeStandard(form)
      ElMessage.success('修改成功')
    } else {
      await addFeeStandard(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error(isEdit.value ? '修改失败' : '新增失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除收费标准「${row.feeName}」？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确认删除',
    cancelButtonText: '取消'
  })
  try {
    await deleteFeeStandard(row.feeStandardId)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    ElMessage.error('删除失败')
  }
}

const handleStatusChange = async (row, val) => {
  try {
    await updateFeeStandard({ feeStandardId: row.feeStandardId, status: val })
    ElMessage.success(val === 1 ? '已启用' : '已禁用')
    loadData()
  } catch {
    ElMessage.error('操作失败')
  }
}

// ==================== 初始化 ====================
onMounted(() => loadData())
</script>
