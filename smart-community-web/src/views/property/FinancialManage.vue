<template>
  <div class="page-container">
    <div class="page-header">
      <h2>财务与缴费管理</h2>
    </div>

    <div class="search-toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchForm.houseNo" placeholder="房屋号" clearable style="width:160px" @change="handleSearch" />
        <el-select v-model="searchForm.feeStandardId" placeholder="费用类型" clearable style="width:140px" @change="handleSearch">
          <el-option v-for="s in feeStandardList" :key="s.feeStandardId" :label="s.feeName" :value="s.feeStandardId" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="账单状态" clearable style="width:130px" @change="handleSearch">
          <el-option label="待缴" :value="0" />
          <el-option label="已缴清" :value="1" />
          <el-option label="逾期" :value="2" />
        </el-select>
        <el-date-picker
          v-model="searchForm.billMonth"
          type="month"
          placeholder="选择月份"
          value-format="YYYY-MM"
          clearable
          style="width:160px"
          @change="handleSearch"
        />
        <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
        <el-button icon="Refresh" @click="handleReset">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" icon="Plus" @click="handleGenerateBills">生成本月账单</el-button>
        <el-button type="success" icon="Download" @click="handleExport">导出 Excel</el-button>
      </div>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe style="width:100%">
      <el-table-column prop="billId" label="账单编号" width="80" />
      <el-table-column prop="houseNo" label="房屋号" width="120" />
      <el-table-column prop="personName" label="住户姓名" width="100" />
      <el-table-column prop="feeName" label="费用类型" width="120" />
      <el-table-column prop="billMonth" label="账单月份" width="100" />
      <el-table-column prop="amount" label="应缴金额(元)" width="120">
        <template #default="{ row }">
          <span style="font-weight:600;color:#303133">{{ row.amount?.toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="dueDate" label="到期日期" width="110" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.status===0" type="primary">待缴</el-tag>
          <el-tag v-else-if="row.status===1" type="success">已缴清</el-tag>
          <el-tag v-else type="danger">逾期</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 0 || row.status === 2">
            <el-button type="primary" link @click="handlePay(row)">确认收款</el-button>
            <el-button type="warning" link @click="handleDunning(row)">下发催缴</el-button>
          </template>
          <template v-else>
            <el-button type="primary" link @click="handleViewRecords(row)">查看流水</el-button>
          </template>
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

    <!-- 确认收款对话框 -->
    <el-dialog v-model="payDialogVisible" title="确认收款" width="450px" @close="resetPayForm">
      <el-form ref="payFormRef" :model="payForm" :rules="payRules" label-width="80px">
        <el-form-item label="账单编号">
          <el-input :model-value="currentBill?.billId" disabled />
        </el-form-item>
        <el-form-item label="应缴金额">
          <el-input :model-value="currentBill?.amount?.toFixed(2)" disabled>
            <template #append>元</template>
          </el-input>
        </el-form-item>
        <el-form-item label="实缴金额" prop="paidAmount">
          <el-input-number v-model="payForm.paidAmount" :min="0.01" :max="999999" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="支付方式" prop="payMethod">
          <el-select v-model="payForm.payMethod" style="width:100%">
            <el-option label="微信" value="微信" />
            <el-option label="支付宝" value="支付宝" />
            <el-option label="现金" value="现金" />
            <el-option label="银行转账" value="银行转账" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePaySubmit" :loading="payLoading">确认收款</el-button>
      </template>
    </el-dialog>

    <!-- 查看流水抽屉 -->
    <el-drawer v-model="recordDrawerVisible" title="缴费流水" size="500px" :close-on-click-modal="false">
      <template v-if="paymentRecords.length > 0">
        <el-timeline>
          <el-timeline-item
            v-for="(record, index) in paymentRecords"
            :key="record.paymentId"
            :timestamp="record.payTime"
            placement="top"
          >
            <div class="payment-record-card">
              <div class="record-row">
                <span class="record-label">实缴金额：</span>
                <span class="record-value" style="color:#67c23a;font-weight:600">{{ record.paidAmount?.toFixed(2) }} 元</span>
              </div>
              <div class="record-row">
                <span class="record-label">支付方式：</span>
                <el-tag size="small" type="primary">{{ record.payMethod }}</el-tag>
              </div>
              <div class="record-row" v-if="record.operator">
                <span class="record-label">操作人：</span>
                <span>{{ record.operator }}</span>
              </div>
              <div class="record-row" v-if="record.remark">
                <span class="record-label">备注：</span>
                <span>{{ record.remark }}</span>
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </template>
      <el-empty v-else description="暂无缴费记录" />
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBillList, payBill, sendDunning, generateMonthlyBills, getPaymentRecordsByBill } from '@/api/payment'
import { getEnabledFeeStandards } from '@/api/payment'
import request from '@/utils/request'

// ==================== 数据状态 ====================
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const feeStandardList = ref([])

const searchForm = reactive({
  houseNo: '',
  feeStandardId: null,
  status: null,
  billMonth: null,
  pageNum: 1,
  pageSize: 10
})

// ==================== 支付对话框 ====================
const payDialogVisible = ref(false)
const payLoading = ref(false)
const payFormRef = ref(null)
const currentBill = ref(null)
const payForm = reactive({
  paidAmount: null,
  payMethod: ''
})
const payRules = {
  paidAmount: [{ required: true, message: '请输入实缴金额', trigger: 'blur' }],
  payMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }]
}

// ==================== 流水抽屉 ====================
const recordDrawerVisible = ref(false)
const paymentRecords = ref([])

// ==================== 数据加载 ====================
const loadFeeStandards = async () => {
  try {
    const { data } = await getEnabledFeeStandards()
    feeStandardList.value = data || []
  } catch {
    feeStandardList.value = []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { ...searchForm }
    if (!params.billMonth) delete params.billMonth
    if (!params.feeStandardId) delete params.feeStandardId
    if (params.status === null || params.status === '') delete params.status
    if (!params.houseNo) delete params.houseNo
    const { data } = await getBillList(params)
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
  searchForm.houseNo = ''
  searchForm.feeStandardId = null
  searchForm.status = null
  searchForm.billMonth = null
  searchForm.pageNum = 1
  loadData()
}

// ==================== 确认收款 ====================
const handlePay = (row) => {
  currentBill.value = row
  payForm.paidAmount = row.amount
  payForm.payMethod = ''
  payDialogVisible.value = true
}

const resetPayForm = () => {
  payFormRef.value?.resetFields()
  currentBill.value = null
}

const handlePaySubmit = async () => {
  await payFormRef.value.validate()
  payLoading.value = true
  try {
    await payBill(currentBill.value.billId, payForm.paidAmount, payForm.payMethod)
    ElMessage.success('收款成功')
    payDialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('收款失败，请重试')
  } finally {
    payLoading.value = false
  }
}

// ==================== 下发催缴 ====================
const handleDunning = async (row) => {
  await ElMessageBox.confirm(
    `确认向住户「${row.personName}」（${row.houseNo}）下发催缴通知？`,
    '催缴确认',
    { type: 'warning', confirmButtonText: '确认下发', cancelButtonText: '取消' }
  )
  try {
    await sendDunning(row.billId)
    ElMessage.success('催缴通知已下发')
    loadData()
  } catch {
    ElMessage.error('催缴下发失败')
  }
}

// ==================== 生成本月账单 ====================
const handleGenerateBills = async () => {
  await ElMessageBox.confirm(
    '确认手动生成本月所有住户的账单？系统将自动跳过已生成的账单。',
    '生成本月账单',
    { type: 'info', confirmButtonText: '确认生成', cancelButtonText: '取消' }
  )
  try {
    await generateMonthlyBills()
    ElMessage.success('本月账单已生成')
    loadData()
  } catch {
    ElMessage.error('账单生成失败')
  }
}

// ==================== 查看流水 ====================
const handleViewRecords = async (row) => {
  paymentRecords.value = []
  recordDrawerVisible.value = true
  try {
    const { data } = await getPaymentRecordsByBill(row.billId)
    paymentRecords.value = data || []
  } catch {
    paymentRecords.value = []
  }
}

// ==================== 导出 Excel ====================
const handleExport = async () => {
  const params = new URLSearchParams()
  if (searchForm.houseNo) params.append('houseNo', searchForm.houseNo)
  if (searchForm.feeStandardId) params.append('feeStandardId', searchForm.feeStandardId)
  if (searchForm.status !== null && searchForm.status !== '') params.append('status', searchForm.status)
  if (searchForm.billMonth) params.append('billMonth', searchForm.billMonth)
  
  try {
    const response = await request.get(`/financial/bill/export?${params.toString()}`, {
      responseType: 'blob'
    })
    const blob = new Blob([response.data])
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `财务账单_${new Date().toLocaleDateString('zh-CN').replace(/\//g, '-')}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

// ==================== 初始化 ====================
onMounted(() => {
  loadFeeStandards()
  loadData()
})
</script>

<style scoped>
.payment-record-card {
  padding: 8px 0;
}
.record-row {
  margin-bottom: 6px;
  font-size: 14px;
  line-height: 1.6;
}
.record-label {
  color: #909399;
  display: inline-block;
  min-width: 70px;
}
.record-value {
  color: #303133;
}
</style>
