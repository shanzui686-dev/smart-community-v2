<template>
  <view class="page">
    <!-- Header -->
    <view class="page-header">
      <text class="header-title">小区公告</text>
    </view>

    <!-- Announcement list -->
    <view class="list">
      <view
        class="card"
        v-for="item in list"
        :key="item.id"
        @click="toggleExpand(item.id)"
      >
        <view class="card-top">
          <text class="card-title">{{ item.title }}</text>
          <text class="card-time">{{ item.createTime }}</text>
        </view>
        <text class="card-content" v-if="!expandedMap[item.id]">
          {{ truncateContent(item.content) }}
        </text>
        <text class="card-content full-content" v-if="expandedMap[item.id]">
          {{ item.content || '' }}
        </text>
        <view class="card-toggle" v-if="needsTruncation(item.content)">
          <text class="toggle-text">{{ expandedMap[item.id] ? '收起' : '展开全文' }}</text>
        </view>
      </view>

      <!-- Empty state -->
      <view v-if="!list.length && !loading" class="empty">
        <text>暂无公告</text>
      </view>
    </view>

    <!-- Load more -->
    <view class="load-more-wrap">
      <view class="load-more" v-if="hasMore" @click="loadMore">
        <text>{{ loading ? '加载中...' : '点击加载更多' }}</text>
      </view>
      <view class="load-more" v-else-if="list.length > 0">
        <text class="no-more">— 没有更多了 —</text>
      </view>
    </view>

    <!-- Bottom spacer for TabBar -->
    <view style="height:120rpx"></view>
    <TabBar current="/pages/announcements/announcements" />
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { getAnnouncements } from '../../utils/request'
import TabBar from '../../components/TabBar.vue'

const list = ref([])
const pageNum = ref(1)
const pageSize = 20
const total = ref(0)
const hasMore = ref(false)
const loading = ref(false)
const expandedMap = reactive({})
let isLeaving = false

const fetchAnnouncements = async (reset) => {
  if (isLeaving || loading.value) return
  loading.value = true
  try {
    const p = reset ? 1 : pageNum.value
    const r = await getAnnouncements({ pageNum: p, pageSize })
    if (r.code === -1) return
    const raw = JSON.parse(JSON.stringify(r.data || {}))
    const rows = JSON.parse(JSON.stringify(raw.records || raw.list || []))
    total.value = raw.total || 0
    if (reset) {
      list.value = rows
      pageNum.value = 1
    } else {
      list.value = [...list.value, ...rows]
    }
    hasMore.value = list.value.length < total.value
  } catch (e) {
    console.error('获取公告列表失败', e)
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  if (!hasMore.value || loading.value) return
  pageNum.value = pageNum.value + 1
  fetchAnnouncements(false)
}

const truncateContent = (content) => {
  const text = content || ''
  if (text.length > 100) {
    return text.substring(0, 100) + '...'
  }
  return text
}

const needsTruncation = (content) => {
  return (content || '').length > 100
}

const toggleExpand = (id) => {
  if (expandedMap[id]) {
    expandedMap[id] = false
  } else {
    expandedMap[id] = true
  }
}

onHide(() => {
  isLeaving = true
})

onShow(() => {
  isLeaving = false
  pageNum.value = 1
  list.value = []
  fetchAnnouncements(true)
})
</script>

<style scoped>
.page {
  padding: 20rpx;
  background: #f5f7fa;
  min-height: 100vh;
}

/* Page header */
.page-header {
  padding: 24rpx 10rpx 16rpx;
}
.header-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #303133;
}

/* Cards */
.list {
  /* plain view, no scroll-view */
}
.card {
  background: #fff;
  border-radius: 12rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,.04);
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}
.card-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #303133;
  flex: 1;
}
.card-time {
  font-size: 22rpx;
  color: #909399;
  flex-shrink: 0;
  margin-left: 16rpx;
}
.card-content {
  font-size: 26rpx;
  color: #606266;
  line-height: 1.7;
  display: block;
}
.card-content.full-content {
  white-space: pre-wrap;
  word-break: break-all;
}
.card-toggle {
  margin-top: 14rpx;
  text-align: right;
}
.toggle-text {
  font-size: 24rpx;
  color: #409EFF;
}

/* Empty */
.empty {
  text-align: center;
  padding: 120rpx 0;
  color: #909399;
  font-size: 28rpx;
}

/* Load more */
.load-more-wrap {
  padding: 20rpx 0;
}
.load-more {
  text-align: center;
  padding: 24rpx;
  font-size: 26rpx;
  color: #409EFF;
}
.no-more {
  color: #c0c4cc;
}
</style>
