<template>
  <view class="page">
    <view class="header-bar">
      <text class="title">访客管理</text>
      <view class="btn-add" @click="openAdd">＋ 新增</view>
    </view>

    <!-- 搜索过滤栏 -->
    <view class="search-bar">
      <input class="search-input" v-model="searchName" placeholder="搜索访客姓名" confirm-type="search" @confirm="doSearch" />
      <view class="filter-status">
        <view
          v-for="item in statusOptions"
          :key="item.value"
          :class="['filter-chip', { active: searchStatus === item.value }]"
          @click="pickStatus(item.value)"
        >
          <text>{{ item.label }}</text>
        </view>
      </view>
    </view>

    <!-- 访客卡片列表 -->
    <view class="list">
      <view class="card" v-for="v in list" :key="v.id">
        <view class="card-top">
          <view class="card-left">
            <text class="name">{{ v.visitorName }}</text>
            <text class="phone">{{ v.visitorPhone }}</text>
          </view>
          <text :class="['status-badge', statusClass(v.status)]">{{ statusLabel(v.status) }}</text>
        </view>
        <view class="card-info">
          <text class="info-row">被访人：{{ v.residentName }}</text>
          <text class="info-row">事由：{{ v.reason }}</text>
          <text class="info-row time">创建时间：{{ v.createTime }}</text>
        </view>
        <!-- 操作按钮 -->
        <view class="card-actions" v-if="getActions(v.status).length">
          <view
            v-for="act in getActions(v.status)"
            :key="act.key"
            :class="['action-btn', act.cls]"
            @click="act.handler(v)"
          >
            <text>{{ act.label }}</text>
          </view>
        </view>
      </view>
      <view v-if="!list.length" class="empty"><text>暂无访客记录</text></view>
      <view v-if="list.length && list.length < total" class="load-more" @click="loadMore">
        点击加载更多（当前{{ list.length }}条 / 共{{ total }}条）
      </view>
    </view>

    <!-- 新增/编辑弹窗 -->
    <view class="modal-mask" v-if="formVisible" @click="formVisible = false" />
    <view class="modal" v-if="formVisible">
      <view class="modal-header">
        <text class="modal-title">{{ isEdit ? '编辑访客' : '新增访客' }}</text>
        <text class="modal-close" @click="formVisible = false">✕</text>
      </view>
      <view class="modal-body">
        <view class="form-group">
          <text class="form-label">访客姓名</text>
          <input class="form-input" v-model="form.visitorName" placeholder="请输入访客姓名" />
        </view>
        <view class="form-group">
          <text class="form-label">手机号</text>
          <input class="form-input" v-model="form.visitorPhone" placeholder="请输入手机号" type="number" maxlength="11" />
        </view>
        <view class="form-group">
          <text class="form-label">被访人</text>
          <input class="form-input" v-model="form.residentName" placeholder="请输入被访人姓名" />
        </view>
        <view class="form-group">
          <text class="form-label">所属小区</text>
          <view class="picker-box" @click="showCommunityList = !showCommunityList">
            <text :class="{ placeholder: !form.communityName }">{{ form.communityName || '请选择小区' }}</text>
            <text class="picker-arrow">▼</text>
          </view>
          <view class="picker-drop" v-if="showCommunityList">
            <view
              v-for="c in communities"
              :key="c.communityId"
              :class="['picker-item', { active: form.communityId === c.communityId }]"
              @click="pickCommunity(c)"
            >
              <text>{{ c.name }}</text>
            </view>
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">访问事由</text>
          <textarea class="form-textarea" v-model="form.reason" placeholder="请输入访问事由" />
        </view>
      </view>
      <view class="modal-footer">
        <view class="btn-cancel" @click="formVisible = false"><text>取消</text></view>
        <view class="btn-submit" @click="doSubmit"><text>确定</text></view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  getVisitors, addVisitor, updateVisitor, cancelVisitor,
  checkInVisitor, checkOutVisitor, getAllCommunities
} from '../../utils/request'

// ---- 列表数据 ----
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 20

// ---- 搜索过滤 ----
const searchName = ref('')
const searchStatus = ref(null)
const statusOptions = [
  { value: null,  label: '全部' },
  { value: 0,     label: '待审核' },
  { value: 1,     label: '已通过' },
  { value: 2,     label: '已签入' },
  { value: 3,     label: '已签出' },
  { value: 4,     label: '已取消' }
]

// ---- 表单弹窗 ----
const formVisible = ref(false)
const isEdit = ref(false)
const communities = ref([])
const showCommunityList = ref(false)

const form = reactive({
  id: '',
  visitorName: '',
  visitorPhone: '',
  residentName: '',
  reason: '',
  communityId: null,
  communityName: ''
})

// ---- 工具函数 ----
const statusLabel = (s) => {
  const map = { 0: '待审核', 1: '已通过', 2: '已签入', 3: '已签出', 4: '已取消' }
  return map[s] !== undefined ? map[s] : '未知'
}

const statusClass = (s) => {
  const map = { 0: 'warn', 1: 'ok', 2: 'ok', 3: 'info', 4: 'err' }
  return map[s] || 'info'
}

// ---- 加载数据 ----
const loadData = async (reset) => {
  if (reset) pageNum.value = 1
  try {
    const params = { pageNum: pageNum.value, pageSize }
    if (searchName.value) params.name = searchName.value
    if (searchStatus.value !== null && searchStatus.value !== '') params.status = searchStatus.value
    const res = await getVisitors(params)
    const data = res.data || {}
    const records = JSON.parse(JSON.stringify(data.records || []))
    if (reset || pageNum.value === 1) {
      list.value = records
    } else {
      list.value = [...list.value, ...records]
    }
    total.value = data.total || 0
  } catch (e) {
    console.error(e)
  }
}

const loadMore = () => {
  if (list.value.length < total.value) {
    pageNum.value++
    loadData(false)
  }
}

const doSearch = () => {
  loadData(true)
}

const pickStatus = (val) => {
  searchStatus.value = val
  loadData(true)
}

// ---- 加载小区列表 ----
const loadCommunities = async () => {
  try {
    const res = await getAllCommunities()
    communities.value = JSON.parse(JSON.stringify(res.data || []))
  } catch (e) {
    console.error(e)
  }
}

// ---- 表单操作 ----
const resetForm = () => {
  form.id = ''
  form.visitorName = ''
  form.visitorPhone = ''
  form.residentName = ''
  form.reason = ''
  form.communityId = null
  form.communityName = ''
  showCommunityList.value = false
}

const openAdd = () => {
  resetForm()
  isEdit.value = false
  formVisible.value = true
}

const openEdit = (v) => {
  const raw = JSON.parse(JSON.stringify(v))
  form.id = raw.id
  form.visitorName = raw.visitorName || ''
  form.visitorPhone = raw.visitorPhone || ''
  form.residentName = raw.residentName || ''
  form.reason = raw.reason || ''
  form.communityId = raw.communityId || null
  form.communityName = raw.communityName || ''
  isEdit.value = true
  formVisible.value = true
}

const pickCommunity = (c) => {
  form.communityId = c.communityId
  form.communityName = c.name
  showCommunityList.value = false
}

const doSubmit = async () => {
  if (!form.visitorName.trim()) {
    uni.showToast({ title: '请输入访客姓名', icon: 'none' })
    return
  }
  const payload = {
    visitorName: form.visitorName.trim(),
    visitorPhone: form.visitorPhone,
    residentName: form.residentName.trim(),
    reason: form.reason,
    communityId: form.communityId
  }
  if (isEdit.value) payload.id = form.id
  try {
    if (isEdit.value) {
      await updateVisitor(payload)
    } else {
      await addVisitor(payload)
    }
    uni.showToast({ title: isEdit.value ? '修改成功' : '新增成功', icon: 'success' })
    formVisible.value = false
    loadData(true)
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}

// ---- 卡牌操作按钮 ----
const getActions = (status) => {
  const actions = []
  if (status === 0) {
    // pending → approve + cancel
    actions.push({ key: 'approve', label: '通过', cls: 'approve', handler: doApprove })
    actions.push({ key: 'cancel', label: '取消', cls: 'cancel', handler: doCancel })
  } else if (status === 1) {
    // approved → check-in + cancel
    actions.push({ key: 'checkin', label: '签入', cls: 'checkin', handler: doCheckIn })
    actions.push({ key: 'cancel', label: '取消', cls: 'cancel', handler: doCancel })
  } else if (status === 2) {
    // checked-in → check-out
    actions.push({ key: 'checkout', label: '签出', cls: 'checkout', handler: doCheckOut })
  }
  return actions
}

const doApprove = (v) => {
  uni.showModal({
    title: '确认通过',
    content: '确定通过「' + v.visitorName + '」的访问申请吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await updateVisitor({ id: v.id, status: 1 })
        uni.showToast({ title: '已通过', icon: 'success' })
        loadData(true)
      } catch (e) {
        uni.showToast({ title: e.message || '操作失败', icon: 'none' })
      }
    }
  })
}

const doCancel = (v) => {
  uni.showModal({
    title: '确认取消',
    content: '确定取消「' + v.visitorName + '」的访问吗？',
    confirmColor: '#F56C6C',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await cancelVisitor(v.id)
        uni.showToast({ title: '已取消', icon: 'success' })
        loadData(true)
      } catch (e) {
        uni.showToast({ title: e.message || '操作失败', icon: 'none' })
      }
    }
  })
}

const doCheckIn = (v) => {
  uni.showModal({
    title: '确认签入',
    content: '确定「' + v.visitorName + '」已到达并签入吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await checkInVisitor(v.id)
        uni.showToast({ title: '签入成功', icon: 'success' })
        loadData(true)
      } catch (e) {
        uni.showToast({ title: e.message || '签入失败', icon: 'none' })
      }
    }
  })
}

const doCheckOut = (v) => {
  uni.showModal({
    title: '确认签出',
    content: '确定「' + v.visitorName + '」已离开并签出吗？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await checkOutVisitor(v.id)
        uni.showToast({ title: '签出成功', icon: 'success' })
        loadData(true)
      } catch (e) {
        uni.showToast({ title: e.message || '签出失败', icon: 'none' })
      }
    }
  })
}

// ---- 生命周期 ----
onShow(() => {
  loadCommunities()
  loadData(true)
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; padding-bottom: 40rpx; }

/* 顶部栏 */
.header-bar { display: flex; justify-content: space-between; align-items: center; padding: 24rpx 32rpx; background: #fff; position: sticky; top: 0; z-index: 50; }
.title { font-size: 34rpx; font-weight: 700; color: #1A1A1A; }
.btn-add { background: #409EFF; color: #fff; font-size: 26rpx; padding: 12rpx 24rpx; border-radius: 32rpx; font-weight: 500; }

/* 搜索栏 */
.search-bar { padding: 20rpx 24rpx; background: #fff; border-bottom: 1rpx solid #f0f0f0; }
.search-input { width: 100%; height: 72rpx; background: #f5f7fa; border-radius: 36rpx; padding: 0 28rpx; font-size: 26rpx; box-sizing: border-box; }
.filter-status { display: flex; gap: 12rpx; margin-top: 16rpx; flex-wrap: wrap; }
.filter-chip { padding: 10rpx 24rpx; border-radius: 28rpx; background: #f5f7fa; font-size: 24rpx; color: #606266; }
.filter-chip.active { background: #ecf5ff; color: #409EFF; font-weight: 500; }

/* 卡片列表 */
.list { padding: 20rpx 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 24rpx 28rpx; margin-bottom: 16rpx; }
.card-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 14rpx; }
.card-left { flex: 1; }
.name { font-size: 30rpx; font-weight: 600; color: #1A1A1A; display: block; }
.phone { font-size: 24rpx; color: #909399; margin-top: 4rpx; display: block; }
.status-badge { font-size: 24rpx; padding: 6rpx 16rpx; border-radius: 6rpx; flex-shrink: 0; }
.status-badge.ok { background: #f0f9eb; color: #67C23A; }
.status-badge.warn { background: #fdf6ec; color: #E6A23C; }
.status-badge.err { background: #fef0f0; color: #F56C6C; }
.status-badge.info { background: #f5f7fa; color: #909399; }

.card-info { margin-bottom: 16rpx; }
.info-row { font-size: 24rpx; color: #606266; display: block; margin-top: 6rpx; }
.info-row.time { color: #909399; }

.card-actions { display: flex; gap: 16rpx; padding-top: 16rpx; border-top: 1rpx solid #f5f7fa; }
.action-btn { flex: 1; height: 60rpx; display: flex; align-items: center; justify-content: center; border-radius: 30rpx; font-size: 24rpx; font-weight: 500; }
.action-btn.approve { background: #ecf5ff; color: #409EFF; }
.action-btn.checkin { background: #f0f9eb; color: #67C23A; }
.action-btn.checkout { background: #fdf6ec; color: #E6A23C; }
.action-btn.cancel { background: #fef0f0; color: #F56C6C; }

/* 空态 & 加载更多 */
.empty { text-align: center; padding: 80rpx; color: #909399; font-size: 28rpx; }
.load-more { text-align: center; padding: 24rpx; color: #409EFF; font-size: 28rpx; }

/* 弹窗 */
.modal-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,.45); z-index: 200; }
.modal { position: fixed; top: 5%; left: 32rpx; right: 32rpx; bottom: 5%; background: #fff; border-radius: 20rpx; z-index: 201; display: flex; flex-direction: column; overflow: hidden; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 28rpx 32rpx 20rpx; border-bottom: 1rpx solid #f0f0f0; flex-shrink: 0; }
.modal-title { font-size: 32rpx; font-weight: 700; color: #1A1A1A; }
.modal-close { font-size: 36rpx; color: #c0c4cc; padding: 8rpx; }
.modal-body { flex: 1; padding: 24rpx 32rpx; overflow-y: auto; }
.modal-footer { display: flex; gap: 20rpx; padding: 20rpx 32rpx 32rpx; border-top: 1rpx solid #f0f0f0; flex-shrink: 0; }
.btn-cancel, .btn-submit { flex: 1; height: 80rpx; display: flex; align-items: center; justify-content: center; border-radius: 40rpx; font-size: 28rpx; }
.btn-cancel { background: #f5f7fa; color: #606266; }
.btn-submit { background: #409EFF; color: #fff; font-weight: 600; }

/* 表单 */
.form-group { margin-bottom: 22rpx; }
.form-label { font-size: 26rpx; color: #606266; margin-bottom: 10rpx; display: block; font-weight: 500; }
.form-input { width: 100%; height: 76rpx; background: #f5f7fa; border-radius: 10rpx; padding: 0 24rpx; font-size: 28rpx; box-sizing: border-box; }
.form-textarea { width: 100%; height: 120rpx; background: #f5f7fa; border-radius: 10rpx; padding: 16rpx 24rpx; font-size: 26rpx; box-sizing: border-box; }
.picker-box { height: 76rpx; background: #f5f7fa; border-radius: 10rpx; padding: 0 24rpx; display: flex; align-items: center; justify-content: space-between; font-size: 28rpx; color: #303133; }
.picker-box .placeholder { color: #c0c4cc; }
.picker-arrow { font-size: 22rpx; color: #909399; }
.picker-drop { margin-top: 8rpx; background: #fff; border-radius: 10rpx; border: 1rpx solid #ebeef5; max-height: 240rpx; overflow-y: auto; }
.picker-item { padding: 22rpx 24rpx; font-size: 26rpx; color: #303133; border-bottom: 1rpx solid #f5f7fa; }
.picker-item:last-child { border-bottom: none; }
.picker-item.active { color: #409EFF; background: #ecf5ff; }
</style>
