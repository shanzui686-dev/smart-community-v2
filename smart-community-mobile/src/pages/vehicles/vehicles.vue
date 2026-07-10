<template>
  <view class="page">
    <view class="header-bar">
      <text class="title">车辆管理</text>
      <view class="btn-add" @click="openAdd">＋ 新增</view>
    </view>

    <!-- 搜索 + 小区筛选 -->
    <view class="search-row">
      <view class="search-input-box">
        <text class="search-icon">🔍</text>
        <input class="search-input" v-model="keyword" placeholder="搜索车牌号/车主姓名" @confirm="onSearch" />
      </view>
      <view class="filter-box" @click="showCommunityFilter = !showCommunityFilter">
        <text>{{ filterCommunityName || '全部小区' }}</text>
        <text class="filter-arrow">▼</text>
      </view>
      <view class="filter-drop" v-if="showCommunityFilter">
        <view class="filter-item" :class="{ active: !filterCommunityId }" @click="pickFilterCommunity(null, '全部小区')">
          <text>全部小区</text>
        </view>
        <view v-for="c in communities" :key="c.communityId"
          :class="['filter-item', { active: filterCommunityId === c.communityId }]"
          @click="pickFilterCommunity(c.communityId, c.name)">
          <text>{{ c.name }}</text>
        </view>
      </view>
    </view>

    <view class="list">
      <view class="card" v-for="v in list" :key="v.vehicleId" @click="openEdit(v)">
        <view class="card-top">
          <view class="plate-box">
            <text class="plate-num">{{ v.plateNumber }}</text>
          </view>
          <text :class="['type-tag', typeClass(v.vehicleType)]">{{ typeLabel(v.vehicleType) }}</text>
        </view>
        <view class="card-info">
          <text class="info-line">👤 {{ v.personName || '-' }}  📞 {{ v.mobile || '-' }}</text>
          <text class="info-line">🏘 {{ v.communityName || '-' }}  🚪 {{ v.houseNo || '-' }}</text>
          <text class="info-line" v-if="v.hasParkingSpace === 1">✅ 有停车位</text>
        </view>
        <view class="card-actions">
          <view class="act-btn edit" @click.stop="openEdit(v)"><text>✎</text></view>
          <view class="act-btn del" @click.stop="confirmDelete(v)"><text>✕</text></view>
        </view>
      </view>
      <view v-if="!list.length" class="empty"><text>暂无车辆数据</text></view>
      <view v-if="list.length" class="load-more" @click="loadMore">
        <text>点击加载更多 (当前{{ list.length }}条/共{{ total }}条)</text>
      </view>
    </view>

    <!-- 新增/编辑弹窗 -->
    <view class="modal-mask" v-if="formVisible" @click="formVisible = false" />
    <view class="modal" v-if="formVisible">
      <view class="modal-header">
        <text class="modal-title">{{ isEdit ? '编辑' : '新增' }}车辆</text>
        <text class="modal-close" @click="formVisible = false">✕</text>
      </view>
      <view class="modal-body">
        <view class="form-group">
          <text class="form-label">车牌号 <text class="required">*</text></text>
          <input class="form-input" v-model="form.plateNumber" placeholder="如：粤B12345" />
        </view>
        <view class="form-group">
          <text class="form-label">车辆类型</text>
          <view class="radio-group">
            <view v-for="opt in vehicleTypeOptions" :key="opt.value"
              :class="['radio-item', { active: form.vehicleType === opt.value }]"
              @click="form.vehicleType = opt.value">
              <text>{{ opt.label }}</text>
            </view>
          </view>
        </view>
        <view class="form-row">
          <view class="form-group half">
            <text class="form-label">车主姓名</text>
            <input class="form-input" v-model="form.personName" placeholder="请输入" />
          </view>
          <view class="form-group half">
            <text class="form-label">车主电话</text>
            <input class="form-input" v-model="form.mobile" placeholder="请输入" />
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">所属小区</text>
          <view class="picker-box" @click="showCommunityList = !showCommunityList">
            <text>{{ form.communityName || '请选择小区' }}</text>
            <text class="picker-arrow">▼</text>
          </view>
          <view class="picker-drop" v-if="showCommunityList">
            <view v-for="c in communities" :key="c.communityId"
              :class="['picker-item', { active: form.communityId === c.communityId }]"
              @click="pickCommunity(c)">
              <text>{{ c.name }}</text>
            </view>
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">门牌号</text>
          <input class="form-input" v-model="form.houseNo" placeholder="请输入门牌号" />
        </view>
        <view class="form-group">
          <text class="form-label">停车位</text>
          <view class="radio-group">
            <view :class="['radio-item', { active: form.hasParkingSpace === 0 }]"
              @click="form.hasParkingSpace = 0">
              <text>无</text>
            </view>
            <view :class="['radio-item', { active: form.hasParkingSpace === 1 }]"
              @click="form.hasParkingSpace = 1">
              <text>有</text>
            </view>
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">备注</text>
          <textarea class="form-textarea" v-model="form.remark" placeholder="可选备注信息" />
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
import { onShow, onHide } from '@dcloudio/uni-app'
import { getVehicles, addVehicle, updateVehicle, deleteVehicle, getAllCommunities } from '../../utils/request'

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const keyword = ref('')
const communities = ref([])
let isLeaving = false

// 小区筛选
const filterCommunityId = ref(null)
const filterCommunityName = ref('')
const showCommunityFilter = ref(false)

// 表单
const formVisible = ref(false)
const isEdit = ref(false)
const showCommunityList = ref(false)

const vehicleTypeOptions = [
  { label: '摩托车', value: 1 },
  { label: '三轮车', value: 2 },
  { label: '电瓶车', value: 3 },
  { label: '家用车', value: 4 }
]

const form = reactive({
  vehicleId: '', communityId: null, communityName: '',
  plateNumber: '', vehicleType: 1, personName: '', mobile: '',
  hasParkingSpace: 0, houseNo: '', remark: ''
})

const typeLabel = (v) => {
  const map = { 1: '摩托车', 2: '三轮车', 3: '电瓶车', 4: '家用车' }
  return map[v] || '未知'
}

const typeClass = (v) => {
  const map = { 1: 'moto', 2: 'tricycle', 3: 'ebike', 4: 'car' }
  return map[v] || 'other'
}

const loadData = async (reset) => {
  if (isLeaving) return
  if (reset) pageNum.value = 1
  try {
    const params = { pageNum: pageNum.value, pageSize: 20 }
    if (keyword.value) params.keyword = keyword.value
    if (filterCommunityId.value) params.communityId = filterCommunityId.value
    const res = await getVehicles(params)
    if (res.code === -1) return
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

const onSearch = () => {
  loadData(true)
}

const loadCommunities = async () => {
  if (isLeaving) return
  try {
    const res = await getAllCommunities()
    if (res.code === -1) return
    communities.value = JSON.parse(JSON.stringify(res.data || []))
  } catch (e) {
    console.error(e)
  }
}

const pickFilterCommunity = (id, name) => {
  filterCommunityId.value = id
  filterCommunityName.value = name
  showCommunityFilter.value = false
  loadData(true)
}

const resetForm = () => {
  form.vehicleId = ''
  form.communityId = null
  form.communityName = ''
  form.plateNumber = ''
  form.vehicleType = 1
  form.personName = ''
  form.mobile = ''
  form.hasParkingSpace = 0
  form.houseNo = ''
  form.remark = ''
  showCommunityList.value = false
}

const openAdd = () => {
  resetForm()
  isEdit.value = false
  formVisible.value = true
}

const openEdit = (v) => {
  const raw = JSON.parse(JSON.stringify(v))
  form.vehicleId = raw.vehicleId
  form.communityId = raw.communityId
  form.communityName = raw.communityName || ''
  form.plateNumber = raw.plateNumber || ''
  form.vehicleType = raw.vehicleType || 1
  form.personName = raw.personName || ''
  form.mobile = raw.mobile || ''
  form.hasParkingSpace = raw.hasParkingSpace || 0
  form.houseNo = raw.houseNo || ''
  form.remark = raw.remark || ''
  isEdit.value = true
  formVisible.value = true
}

const pickCommunity = (c) => {
  form.communityId = c.communityId
  form.communityName = c.name
  showCommunityList.value = false
}

const doSubmit = async () => {
  if (!form.plateNumber.trim()) {
    uni.showToast({ title: '请输入车牌号', icon: 'none' })
    return
  }
  const payload = {
    communityId: form.communityId,
    communityName: form.communityName,
    plateNumber: form.plateNumber.trim(),
    vehicleType: form.vehicleType,
    personName: form.personName,
    mobile: form.mobile,
    hasParkingSpace: form.hasParkingSpace,
    houseNo: form.houseNo,
    remark: form.remark
  }
  if (isEdit.value) payload.vehicleId = form.vehicleId
  try {
    if (isEdit.value) await updateVehicle(payload)
    else await addVehicle(payload)
    uni.showToast({ title: isEdit.value ? '修改成功' : '新增成功', icon: 'success' })
    formVisible.value = false
    loadData(true)
  } catch (e) {
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}

const confirmDelete = (v) => {
  uni.showModal({
    title: '确认删除',
    content: '确定删除车辆「' + v.plateNumber + '」吗？',
    confirmText: '删除',
    confirmColor: '#F56C6C',
    success: (res) => {
      if (res.confirm) {
        deleteVehicle(v.vehicleId).then(() => {
          uni.showToast({ title: '删除成功', icon: 'success' })
          loadData(true)
        }).catch((e) => {
          uni.showToast({ title: e.message || '删除失败', icon: 'none' })
        })
      }
    }
  })
}

onHide(() => {
  isLeaving = true
})

onShow(() => {
  isLeaving = false
  loadCommunities()
  loadData(true)
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; padding-bottom: 40rpx; }

.header-bar { display: flex; justify-content: space-between; align-items: center; padding: 24rpx 32rpx; background: #fff; position: sticky; top: 0; z-index: 50; }
.title { font-size: 34rpx; font-weight: 700; color: #1A1A1A; }
.btn-add { background: #409EFF; color: #fff; font-size: 26rpx; padding: 12rpx 24rpx; border-radius: 32rpx; font-weight: 500; }

/* 搜索 + 筛选 */
.search-row { display: flex; gap: 16rpx; padding: 20rpx 24rpx; background: #fff; position: relative; z-index: 40; }
.search-input-box { flex: 1; display: flex; align-items: center; background: #f5f7fa; border-radius: 32rpx; padding: 0 24rpx; height: 68rpx; }
.search-icon { font-size: 28rpx; margin-right: 8rpx; }
.search-input { flex: 1; height: 100%; font-size: 26rpx; }
.filter-box { display: flex; align-items: center; gap: 8rpx; background: #f5f7fa; border-radius: 32rpx; padding: 0 28rpx; height: 68rpx; font-size: 26rpx; color: #606266; white-space: nowrap; flex-shrink: 0; }
.filter-arrow { font-size: 20rpx; color: #909399; }
.filter-drop { position: absolute; top: 100%; right: 24rpx; background: #fff; border-radius: 12rpx; border: 1rpx solid #ebeef5; max-height: 320rpx; overflow-y: auto; z-index: 60; min-width: 240rpx; box-shadow: 0 4rpx 12rpx rgba(0,0,0,.08); }
.filter-item { padding: 22rpx 28rpx; font-size: 26rpx; color: #303133; border-bottom: 1rpx solid #f5f7fa; }
.filter-item:last-child { border-bottom: none; }
.filter-item.active { color: #409EFF; background: #ecf5ff; }

/* 列表 */
.list { padding: 20rpx 24rpx; }

.card { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 16rpx; position: relative; }
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.plate-box { background: #409EFF; border-radius: 8rpx; padding: 8rpx 20rpx; }
.plate-num { font-size: 32rpx; font-weight: 700; color: #fff; letter-spacing: 2rpx; }

.type-tag { font-size: 22rpx; padding: 6rpx 16rpx; border-radius: 6rpx; font-weight: 500; }
.type-tag.car { background: #ecf5ff; color: #409EFF; }
.type-tag.suv { background: #f0f9eb; color: #67C23A; }
.type-tag.truck { background: #fdf6ec; color: #E6A23C; }
.type-tag.moto { background: #fef0f0; color: #F56C6C; }
.type-tag.other { background: #f5f7fa; color: #909399; }

.card-info { margin-bottom: 8rpx; }
.info-line { font-size: 24rpx; color: #606266; display: block; margin-top: 6rpx; }

.card-actions { position: absolute; top: 24rpx; right: 24rpx; display: flex; flex-direction: column; gap: 10rpx; }
.act-btn { width: 56rpx; height: 56rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 26rpx; }
.act-btn.edit { background: #ecf5ff; color: #409EFF; }
.act-btn.del { background: #fef0f0; color: #F56C6C; }

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
.required { color: #F56C6C; }
.form-input { width: 100%; height: 76rpx; background: #f5f7fa; border-radius: 10rpx; padding: 0 24rpx; font-size: 28rpx; box-sizing: border-box; }
.form-textarea { width: 100%; height: 120rpx; background: #f5f7fa; border-radius: 10rpx; padding: 16rpx 24rpx; font-size: 26rpx; box-sizing: border-box; }
.form-row { display: flex; gap: 12rpx; }
.half { flex: 1; }
.picker-box { height: 76rpx; background: #f5f7fa; border-radius: 10rpx; padding: 0 24rpx; display: flex; align-items: center; justify-content: space-between; font-size: 28rpx; color: #303133; }
.picker-arrow { font-size: 20rpx; color: #909399; }
.picker-drop { margin-top: 8rpx; background: #fff; border-radius: 10rpx; border: 1rpx solid #ebeef5; max-height: 240rpx; overflow-y: auto; }
.picker-item { padding: 22rpx 24rpx; font-size: 26rpx; color: #303133; border-bottom: 1rpx solid #f5f7fa; }
.picker-item:last-child { border-bottom: none; }
.picker-item.active { color: #409EFF; background: #ecf5ff; }
.radio-group { display: flex; gap: 8rpx; }
.radio-item { flex: 1; height: 68rpx; display: flex; align-items: center; justify-content: center; background: #f5f7fa; border-radius: 10rpx; font-size: 24rpx; color: #909399; border: 2rpx solid transparent; }
.radio-item.active { background: #ecf5ff; color: #409EFF; border-color: #409EFF; }
</style>
