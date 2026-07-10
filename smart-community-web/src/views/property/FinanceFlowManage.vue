<template>
  <div class="page-container">
    <div class="page-header">
      <h2>收支流水明细</h2>
    </div>

    <div class="search-toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchForm.paymentId" placeholder="流水单号" clearable style="width:150px" @change="handleSearch" />
        <el-input v-model="searchForm.houseNo" placeholder="房屋号" clearable style="width:130px" @change="handleSearch" />
        <el-select v-model="searchForm.transactionType" placeholder="交易类型" clearable style="width:130px" @change="handleSearch">
          <el-option label="收入" value="收入" />
          <el-option label="支出" value="支出" />
        </el-select>
        <el-select v-model="searchForm.payMethod" placeholder="支付方式" clearable style="width:140px" @change="handleSearch">
          <el-option label="微信" value="微信" />
          <el-option label="支付宝" value="支付宝" />
          <el-option label="现金" value="现金" />
          <el-option label="银行转账" value="银行转账" />
          <el-option label="对公转账" value="对公转账" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          @change="handleDateChange"
          style="width:260px"
        />
        <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
        <el-button icon="Refresh" @click="handleReset">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="success" icon="Download" @click="handleExport">导出 Excel</el-button>
      </div>
    </div>

    <!-- 统计概要 -->
    <div class="summary-bar">
      <div class="summary-item">
        <span class="summary-label">总笔数</span>
        <span class="summary-value">{{ total }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">收入合计</span>
        <span class="summary-value income">￥ {{ incomeTotal.toFixed(2) }}</span>
      </div>
      <div class="summary-item">
        <span class="summary-label">支出合计</span>
        <span class="summary-value expense">￥ {{ expenseTotal.toFixed(2) }}</span>
      </div>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe style="width:100%">
      <el-table-column prop="paymentId" label="流水号" width="80" />
      <el-table-column prop="billId" label="账单编号" width="80" />
      <el-table-column prop="houseNo" label="房屋号" width="120" />
      <el-table-column prop="personName" label="居民姓名" width="100" />
      <el-table-column label="交易类型" width="100">
        <template #default="{ row }">
          <el-tag type="success" effect="plain">收入</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="支付方式" width="120">
        <template #default="{ row }">
          <el-tag :type="getPayMethodType(row.payMethod)" effect="plain" size="small">
            {{ row.payMethod }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="交易金额" width="130" align="right">
        <template #default="{ row }">
          <span class="amount-income">￥ {{ row.paidAmount?.toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="交易时间" width="170">
        <template #default="{ row }">
          {{ row.payTime || row.createTime || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="operator" label="操作人" width="100" />
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
    </el-table>

    <div style="margin-top:16px;display:flex;justify-content:flex-end">
      <el-pagination
        v-model:current-page="searchForm.pageNum"
        v-model:page-size="searchForm.pageSize"
        :page-sizes="[10,20,50,100]"
        :total="total"
        layout="total,sizes,prev,pager,next"
        @change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getPaymentRecords } from '@/api/payment'
import request from '@/utils/request'

// ==================== 数据状态 ====================
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dateRange = ref([])

const searchForm = reactive({
  paymentId: '',
  houseNo: '',
  transactionType: null,
  payMethod: null,
  pageNum: 1,
  pageSize: 10
})

// ==================== 计算属性 ====================
const incomeTotal = computed(() => {
  return tableData.value
    .filter(r => !r.transactionType || r.transactionType !== '支出')
    .reduce((sum, r) => sum + (r.paidAmount || 0), 0)
})

const expenseTotal = computed(() => 0)

// ==================== 辅助函数 ====================
const getPayMethodType = (method) => {
  const map = {
    '微信': 'success',
    '支付宝': 'primary',
    '现金': 'warning',
    '银行转账': 'info',
    '对公转账': 'danger'
  }
  return map[method] || 'info'
}

// ==================== 数据加载 ====================
const loadData = async () => {
  loading.value = true
  try {
    const params = {}
    if (searchForm.payMethod) params.payMethod = searchForm.payMethod
    if (searchForm.paymentId) params.paymentId = searchForm.paymentId
    if (searchForm.houseNo) params.houseNo = searchForm.houseNo
    params.pageNum = searchForm.pageNum
    params.pageSize = searchForm.pageSize
    const { data } = await getPaymentRecords(params)
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
  searchForm.paymentId = ''
  searchForm.houseNo = ''
  searchForm.transactionType = null
  searchForm.payMethod = null
  dateRange.value = []
  searchForm.pageNum = 1
  loadData()
}

const handleDateChange = (val) => {
  handleSearch()
}

const handleExport = async () => {
  const params = new URLSearchParams()
  if (searchForm.payMethod) params.append('payMethod', searchForm.payMethod)
  if (searchForm.paymentId) params.append('keyword', searchForm.paymentId)
  if (dateRange.value && dateRange.value.length >= 2) {
    params.append('startTime', `${dateRange.value[0]} 00:00:00`)
    params.append('endTime', `${dateRange.value[1]} 23:59:59`)
  }
  
  try {
    const response = await request.get(`/financial/payment-record/export?${params.toString()}`, {
      responseType: 'blob'
    })
    const blob = new Blob([response.data])
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `收支流水明细_${new Date().toLocaleDateString('zh-CN').replace(/\//g, '-')}.xlsx`
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
onMounted(() => loadData())
</script>

<style scoped>
.summary-bar {
  display: flex;
  gap: 24px;
  padding: 16px 20px;
  margin-bottom: 16px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-label {
  font-size: 14px;
  color: #909399;
}

.summary-value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.summary-value.income {
  color: #67c23a;
}

.summary-value.expense {
  color: #f56c6c;
}

.amount-income {
  font-weight: 600;
  color: #67c23a;
  font-family: 'Helvetica Neue', monospace;
}
</style>
