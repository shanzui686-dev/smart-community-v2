<template>
  <view class="page">
    <view class="header-bar">
      <text class="title">出入记录</text>
    </view>

    <!-- 搜索栏 -->
    <view class="search-bar">
      <input class="search-input" v-model="searchForm.personName" placeholder="输入姓名搜索" @confirm="onSearch" />
      <text class="search-btn" @click="onSearch">搜索</text>
    </view>

    <!-- 筛选栏 -->
    <view class="filter-row">
      <!-- 类型筛选 -->
      <view class="filter-chips">
        <view v-for="t in typeOptions" :key="t.value"
          :class="['chip', { active: searchForm.type === t.value }]"
          @click="searchForm.type = t.value; onSearch()"><text>{{ t.label }}</text></view>
      </view>
      <!-- 小区筛选 -->
      <view class="filter-picker">
        <view class="picker-box" @click="showCommunity = !showCommunity">
          <text>{{ searchForm.communityName || '全部小区' }}</text>
          <text class="picker-arrow">▼</text>
        </view>
        <view class="picker-drop" v-if="showCommunity">
          <view :class="['picker-item', { active: !searchForm.communityId }]"
            @click="pickCommunity(null, '全部小区')">全部小区</view>
          <view v-for="c in communities" :key="c.communityId"
            :class="['picker-item', { active: searchForm.communityId === c.communityId }]"
            @click="pickCommunity(c.communityId, c.name)">{{ c.name }}</view>
        </view>
      </view>
    </view>

    <!-- 日期筛选 -->
    <view class="date-row">
      <input class="date-input" v-model="searchForm.startTime" placeholder="开始日期 YYYY-MM-DD" />
      <text class="date-sep">至</text>
      <input class="date-input" v-model="searchForm.endTime" placeholder="结束日期 YYYY-MM-DD" />
    </view>

    <!-- 列表 -->
    <view class="list">
      <view class="card" v-for="r in list" :key="r.recordId">
        <view class="card-top">
          <text class="person-name">{{ r.personName || '-' }}</text>
          <text :class="['type-badge', r.accessType === 1 ? 'in' : 'out']">
            {{ r.accessType === 1 ? '进入' : '外出' }}
          </text>
        </view>
        <view class="card-body">
          <text class="sub">🕐 {{ r.accessTime || '-' }}</text>
          <text class="sub">🏠 {{ r.communityName || '-' }}</text>
          <text class="sub">📷 {{ r.deviceName || '-' }}</text>
          <text class="sub">✅ {{ verifyLabel(r.verifyType) }}</text>
        </view>
      </view>

      <view v-if="!list.length && !loading" class="empty"><text>暂无出入记录</text></view>
      <view v-if="list.length && hasMore" class="load-more" @click="loadMore">点击加载更多 (当前{{ list.length }}条/共{{ total }}条)</view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { getRecords, getAllCommunities } from '../../utils/request'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const hasMore = ref(false)
const pageNum = ref(1)
const communities = ref([])
const showCommunity = ref(false)
let isLeaving = false

const typeOptions = [
  { label: '全部', value: null },
  { label: '进入', value: 1 },
  { label: '外出', value: 2 }
]

const searchForm = reactive({
  personName: '',
  type: null,
  communityId: null,
  communityName: '全部小区',
  startTime: '',
  endTime: ''
})

const verifyLabel = (t) => {
  if (t === 1) return '人脸识别'
  if (t === 2) return '门禁卡'
  if (t === 3) return '访客'
  if (t === 4) return '车牌识别'
  return '未知'
}

const loadData = async (reset) => {
  if (isLeaving || loading.value) return
  if (reset) pageNum.value = 1
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: 20 }
    if (searchForm.personName) params.personName = searchForm.personName
    if (searchForm.type) params.type = searchForm.type
    if (searchForm.communityId) params.communityId = searchForm.communityId
    if (searchForm.startTime) params.startTime = searchForm.startTime
    if (searchForm.endTime) params.endTime = searchForm.endTime
    const res = await getRecords(params)
    if (res.code === -1) return
    const data = res.data || {}
    const records = JSON.parse(JSON.stringify(data.records || []))
    if (reset) {
      list.value = records
    } else {
      list.value = [...list.value, ...records]
    }
    total.value = data.total || 0
    hasMore.value = list.value.length < total.value
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const loadCommunities = async () => {
  if (isLeaving) return
  try {
    const res = await getAllCommunities()
    if (res.code === -1) return
    communities.value = JSON.parse(JSON.stringify(res.data || []))
  } catch (e) { console.error(e) }
}

const onSearch = () => { showCommunity.value = false; loadData(true) }

const loadMore = () => {
  if (hasMore.value && !loading.value) { pageNum.value++; loadData(false) }
}

const pickCommunity = (id, name) => {
  searchForm.communityId = id
  searchForm.communityName = name
  showCommunity.value = false
  onSearch()
}

onHide(() => { isLeaving = true })

onShow(() => { isLeaving = false; loadCommunities(); loadData(true) })
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; padding-bottom: 40rpx; }

.header-bar { padding: 24rpx 32rpx 20rpx; background: #fff; position: sticky; top: 0; z-index: 50; }
.title { font-size: 34rpx; font-weight: 700; color: #1A1A1A; }

.search-bar { display: flex; align-items: center; padding: 12rpx 32rpx; background: #fff; gap: 12rpx; }
.search-input { flex: 1; height: 64rpx; background: #f5f7fa; border-radius: 32rpx; padding: 0 28rpx; font-size: 26rpx; }
.search-btn { font-size: 26rpx; color: #409EFF; font-weight: 500; padding: 12rpx 8rpx; }

.filter-row { display: flex; align-items: center; padding: 12rpx 32rpx; background: #fff; gap: 16rpx; border-bottom: 1rpx solid #f0f0f0; }
.filter-chips { display: flex; gap: 10rpx; flex: 1; }
.chip { padding: 10rpx 20rpx; border-radius: 24rpx; font-size: 24rpx; color: #909399; background: #f5f7fa; border: 2rpx solid transparent; }
.chip.active { color: #409EFF; background: #ecf5ff; border-color: #409EFF; }

.filter-picker { position: relative; min-width: 160rpx; }
.picker-box { height: 56rpx; background: #f5f7fa; border-radius: 10rpx; padding: 0 16rpx; display: flex; align-items: center; justify-content: space-between; font-size: 24rpx; color: #303133; }
.picker-arrow { font-size: 18rpx; color: #909399; }
.picker-drop { position: absolute; top: 60rpx; left: 0; right: 0; background: #fff; border-radius: 10rpx; border: 1rpx solid #ebeef5; max-height: 240rpx; overflow-y: auto; z-index: 60; }
.picker-item { padding: 18rpx 16rpx; font-size: 24rpx; color: #303133; border-bottom: 1rpx solid #f5f7fa; }
.picker-item:last-child { border-bottom: none; }
.picker-item.active { color: #409EFF; background: #ecf5ff; }

.date-row { display: flex; align-items: center; padding: 12rpx 32rpx; background: #fff; gap: 8rpx; }
.date-input { flex: 1; height: 56rpx; background: #f5f7fa; border-radius: 8rpx; padding: 0 16rpx; font-size: 24rpx; text-align: center; }
.date-sep { font-size: 24rpx; color: #909399; }

.list { padding: 16rpx 24rpx; }

.card { background: #fff; border-radius: 12rpx; padding: 20rpx 24rpx; margin-bottom: 14rpx; }
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10rpx; }
.person-name { font-size: 30rpx; font-weight: 600; color: #303133; }
.type-badge { font-size: 22rpx; padding: 4rpx 14rpx; border-radius: 4rpx; font-weight: 500; }
.type-badge.in { background: #f0f9eb; color: #67C23A; }
.type-badge.out { background: #fdf6ec; color: #E6A23C; }
.sub { font-size: 24rpx; color: #909399; display: block; margin-top: 4rpx; }

.empty { text-align: center; padding: 80rpx; color: #909399; font-size: 28rpx; }
.load-more { text-align: center; padding: 24rpx; color: #409EFF; font-size: 26rpx; }
</style>
