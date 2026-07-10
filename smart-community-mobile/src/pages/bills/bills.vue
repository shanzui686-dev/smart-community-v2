<template>
  <view class="page">
    <!-- 欠费汇总 -->
    <view class="summary-bar">
      <text class="summary-label">待缴金额</text>
      <text class="summary-amount">￥{{ pendingTotal }}</text>
    </view>

    <!-- 状态筛选 -->
    <view class="filter-bar">
      <view
        v-for="opt in statusOptions"
        :key="opt.value"
        :class="['filter-chip', { active: currentStatus === opt.value }]"
        @click="switchStatus(opt.value)"
      >
        <text>{{ opt.label }}</text>
      </view>
    </view>

    <!-- 账单列表 -->
    <view class="list">
      <view class="card" v-for="b in list" :key="b.id">
        <view class="card-top">
          <view class="card-top-left">
            <text class="fee-type">{{ b.feeTypeName }}</text>
            <text class="bill-month">{{ b.billMonth }}</text>
          </view>
          <text class="amount">￥{{ (b.amount / 100).toFixed(2) }}</text>
        </view>
        <view class="card-info">
          <text class="info-row">{{ b.communityName }} {{ b.houseNo }}</text>
          <view class="info-right">
            <text v-if="b.dueDate" class="due-date">截止：{{ b.dueDate }}</text>
            <text :class="['badge', badgeClass(b.status)]">{{ statusLabel(b.status) }}</text>
          </view>
        </view>
        <view
          v-if="b.status === 0 || b.status === 2"
          class="pay-btn"
          @click="openPay(b)"
        >
          <text>立即缴费</text>
        </view>
      </view>

      <view v-if="!list.length && !loading" class="empty">
        <text>暂无账单</text>
      </view>
    </view>

    <!-- 加载更多 -->
    <view class="load-more" v-if="hasMore" @click="loadMore">
      <text>{{ loading ? '加载中...' : '点击加载更多' }}</text>
    </view>

    <!-- 支付方式弹窗 -->
    <view class="modal-mask" v-if="payVisible" @click="closePay">
      <view class="modal-box" @click.stop>
        <text class="modal-title">选择支付方式</text>
        <view class="method-list">
          <view
            v-for="m in payMethods"
            :key="m.value"
            :class="['method-item', { selected: selectedMethod === m.value }]"
            @click="selectedMethod = m.value"
          >
            <text class="method-icon">{{ m.icon }}</text>
            <text class="method-name">{{ m.label }}</text>
          </view>
        </view>
        <view class="modal-actions">
          <view class="btn-cancel" @click="closePay"><text>取消</text></view>
          <view class="btn-confirm" @click="confirmPay"><text>确认支付</text></view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { getBillList, payBill, getFinancialReport } from '../../utils/request'

// ---- 列表数据 ----
const list = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

// ---- 筛选 ----
const currentStatus = ref('')

// ---- 汇总 ----
const pendingTotal = ref('0.00')

// ---- 支付弹窗 ----
const payVisible = ref(false)
const currentBill = ref(null)
const selectedMethod = ref('wechat')

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待缴费', value: 0 },
  { label: '已缴费', value: 1 },
  { label: '已逾期', value: 2 }
]

const payMethods = [
  { label: '微信支付', value: 'wechat', icon: '💚' },
  { label: '支付宝', value: 'alipay', icon: '💙' },
  { label: '现金', value: 'cash', icon: '💵' }
]

const hasMore = computed(() => {
  return currentPage.value * pageSize < total.value
})

const statusLabel = (s) => {
  const map = { 0: '待缴费', 1: '已缴费', 2: '已逾期' }
  return map[s] !== undefined ? map[s] : '未知'
}

const badgeClass = (s) => {
  const map = { 0: 'pending', 1: 'paid', 2: 'overdue' }
  return map[s] || ''
}

// ---- 加载汇总 ----
const fetchReport = async () => {
  try {
    const res = await getFinancialReport()
    const data = JSON.parse(JSON.stringify(res.data || res))
    const raw = data.totalPendingAmount !== undefined ? data.totalPendingAmount
      : data.pendingAmount !== undefined ? data.pendingAmount
      : data.totalPending !== undefined ? data.totalPending
      : 0
    pendingTotal.value = (Number(raw) / 100).toFixed(2)
  } catch (e) {
    console.error('fetchReport error:', e)
  }
}

// ---- 加载列表 ----
const fetchList = async (append) => {
  if (loading.value) return
  loading.value = true
  try {
    const params = { pageNum: currentPage.value, pageSize }
    if (currentStatus.value !== '') {
      params.status = currentStatus.value
    }

    const res = await getBillList(params)
    const raw = JSON.parse(JSON.stringify(res.data || {}))
    const records = raw.records || []
    total.value = raw.total || 0

    if (append) {
      list.value = [...list.value, ...records]
    } else {
      list.value = records
    }
  } catch (e) {
    console.error('fetchList error:', e)
  } finally {
    loading.value = false
  }
}

const switchStatus = (val) => {
  currentStatus.value = val
  currentPage.value = 1
  list.value = []
  fetchList(false)
}

const loadMore = () => {
  if (loading.value) return
  currentPage.value = currentPage.value + 1
  fetchList(true)
}

// ---- 支付 ----
const openPay = (bill) => {
  currentBill.value = bill
  selectedMethod.value = 'wechat'
  payVisible.value = true
}

const closePay = () => {
  payVisible.value = false
}

const confirmPay = async () => {
  if (!currentBill.value) return
  const bill = currentBill.value
  const method = selectedMethod.value
  payVisible.value = false

  uni.showLoading({ title: '支付中...' })
  try {
    await payBill(bill.id, bill.amount, method)
    uni.hideLoading()
    uni.showToast({ title: '支付成功', icon: 'success' })
    currentPage.value = 1
    await fetchList(false)
    await fetchReport()
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: e.message || '支付失败', icon: 'none' })
    console.error('confirmPay error:', e)
  }
}

// ---- 生命周期 ----
onShow(() => {
  currentPage.value = 1
  fetchList(false)
  fetchReport()
})

onPullDownRefresh(() => {
  currentPage.value = 1
  Promise.all([fetchList(false), fetchReport()]).finally(() => {
    uni.stopPullDownRefresh()
  })
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 40rpx;
}

/* ===== 汇总栏 ===== */
.summary-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 36rpx;
  background: linear-gradient(135deg, #409EFF, #66b1ff);
  margin: 20rpx 20rpx 0;
  border-radius: 16rpx;
}

.summary-label {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.85);
}

.summary-amount {
  font-size: 44rpx;
  font-weight: bold;
  color: #fff;
}

/* ===== 筛选栏 ===== */
.filter-bar {
  display: flex;
  gap: 16rpx;
  padding: 24rpx 20rpx;
}

.filter-chip {
  flex: 1;
  text-align: center;
  padding: 14rpx 0;
  border-radius: 8rpx;
  font-size: 26rpx;
  color: #606266;
  background: #fff;
}

.filter-chip.active {
  background: #409EFF;
  color: #fff;
}

/* ===== 列表 ===== */
.list {
  padding: 0 20rpx;
}

.card {
  background: #fff;
  border-radius: 12rpx;
  padding: 24rpx 28rpx;
  margin-bottom: 16rpx;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14rpx;
}

.card-top-left {
  flex: 1;
}

.fee-type {
  font-size: 30rpx;
  font-weight: 500;
  color: #303133;
  display: block;
}

.bill-month {
  font-size: 24rpx;
  color: #909399;
  margin-top: 4rpx;
  display: block;
}

.amount {
  font-size: 36rpx;
  font-weight: bold;
  color: #F56C6C;
  flex-shrink: 0;
}

.card-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 24rpx;
  color: #606266;
  margin-bottom: 16rpx;
}

.info-row {
  flex: 1;
}

.info-right {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-shrink: 0;
}

.due-date {
  font-size: 22rpx;
  color: #909399;
}

/* ===== 状态徽章 ===== */
.badge {
  font-size: 24rpx;
  padding: 4rpx 14rpx;
  border-radius: 4rpx;
}

.badge.pending {
  background: #fdf6ec;
  color: #E6A23C;
}

.badge.paid {
  background: #f0f9eb;
  color: #67C23A;
}

.badge.overdue {
  background: #fef0f0;
  color: #F56C6C;
}

/* ===== 缴费按钮 ===== */
.pay-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 64rpx;
  background: #409EFF;
  color: #fff;
  border-radius: 8rpx;
  font-size: 26rpx;
}

/* ===== 空状态 ===== */
.empty {
  text-align: center;
  padding: 80rpx;
  color: #909399;
  font-size: 28rpx;
}

/* ===== 加载更多 ===== */
.load-more {
  display: flex;
  justify-content: center;
  padding: 24rpx 20rpx 40rpx;
  color: #409EFF;
  font-size: 26rpx;
}

/* ===== 支付弹窗 ===== */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 999;
}

.modal-box {
  width: 100%;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding: 36rpx 32rpx;
  padding-bottom: calc(32rpx + env(safe-area-inset-bottom));
}

.modal-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #303133;
  display: block;
  margin-bottom: 28rpx;
}

.method-list {
  display: flex;
  gap: 20rpx;
  margin-bottom: 36rpx;
}

.method-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24rpx 0;
  border-radius: 12rpx;
  border: 2rpx solid #e4e7ed;
}

.method-item.selected {
  border-color: #409EFF;
  background: #ecf5ff;
}

.method-icon {
  font-size: 48rpx;
  margin-bottom: 8rpx;
}

.method-name {
  font-size: 24rpx;
  color: #606266;
}

.modal-actions {
  display: flex;
  gap: 20rpx;
}

.btn-cancel,
.btn-confirm {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80rpx;
  border-radius: 12rpx;
  font-size: 28rpx;
}

.btn-cancel {
  background: #f5f7fa;
  color: #606266;
}

.btn-confirm {
  background: #409EFF;
  color: #fff;
}
</style>
