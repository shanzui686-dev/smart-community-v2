<template>
  <view class="home-page">
    <!-- Stats Cards -->
    <view class="stats-row">
      <view class="stat-card" v-for="item in stats" :key="item.title" :style="{ backgroundColor: item.bg }">
        <text class="stat-num">{{ item.value }}</text>
        <text class="stat-label">{{ item.title }}</text>
      </view>
    </view>

    <!-- 功能入口 -->
    <view class="feature-section">
      <text class="section-title">功能服务</text>
      <view class="feature-grid">
        <view class="feature-item" @click="goTo('/pages/face/face')">
          <view class="feature-icon" style="background: #ecf5ff">
            <text class="iconfont">&#xe6b2;</text>
          </view>
          <text class="feature-name">人脸门禁</text>
        </view>
        <view class="feature-item" @click="goTo('/pages/scan/scan')">
          <view class="feature-icon" style="background: #f0f9eb">
            <text class="iconfont">&#xe67c;</text>
          </view>
          <text class="feature-name">扫码查询</text>
        </view>
        <view class="feature-item" @click="goTo('/pages/map/map')">
          <view class="feature-icon" style="background: #fdf6ec">
            <text class="iconfont">&#xe6b4;</text>
          </view>
          <text class="feature-name">小区地图</text>
        </view>
        <view class="feature-item" @click="goTo('/pages/mine/mine')">
          <view class="feature-icon" style="background: #fef0f0">
            <text class="iconfont">&#xe6b5;</text>
          </view>
          <text class="feature-name">个人中心</text>
        </view>
      </view>
    </view>

    <!-- 最新出入记录 -->
    <view class="record-section">
      <text class="section-title">最新出入记录</text>
      <view class="record-list">
        <view class="record-item" v-for="r in records" :key="r.recordId">
          <view class="record-left">
            <text class="record-name">{{ r.personName }}</text>
            <text class="record-time">{{ r.time }}</text>
          </view>
          <view class="record-right">
            <text :class="['record-type', r.type === 1 ? 'in' : 'out']">
              {{ r.type === 1 ? '进入' : '外出' }}
            </text>
            <text :class="['record-status', r.verified ? 'pass' : 'fail']">
              {{ r.verified ? '已通过' : '未通过' }}
            </text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getStatistics, getRecords } from '../../utils/request'

const stats = ref([
  { title: '小区', value: 0, bg: '#ecf5ff' },
  { title: '居民', value: 0, bg: '#f0f9eb' },
  { title: '今日出入', value: 0, bg: '#fdf6ec' },
])

const records = ref([])

const loadData = async () => {
  try {
    const res = await getStatistics()
    stats.value[0].value = res.data.communityCount || 0
    stats.value[1].value = res.data.personCount || 0
    stats.value[2].value = res.data.todayRecordCount || 0

    const recordRes = await getRecords({ pageNum: 1, pageSize: 5 })
    records.value = recordRes.data.records || []
  } catch (e) {}
}

const goTo = (url) => {
  uni.navigateTo({ url })
}

onMounted(() => loadData())
</script>

<style lang="scss" scoped>
.home-page {
  padding: 20rpx;
  padding-bottom: 40rpx;
}

.stats-row {
  display: flex;
  gap: 16rpx;
  margin-bottom: 30rpx;

  .stat-card {
    flex: 1;
    padding: 24rpx;
    border-radius: 12rpx;
    text-align: center;

    .stat-num {
      font-size: 40rpx;
      font-weight: bold;
      color: #303133;
      display: block;
    }
    .stat-label {
      font-size: 24rpx;
      color: #909399;
      margin-top: 8rpx;
    }
  }
}

.section-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #303133;
  margin-bottom: 20rpx;
  display: block;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx;
  margin-bottom: 30rpx;

  .feature-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20rpx 0;
    background: #fff;
    border-radius: 12rpx;

    .feature-icon {
      width: 80rpx;
      height: 80rpx;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 12rpx;
      font-size: 36rpx;
    }
    .feature-name {
      font-size: 24rpx;
      color: #606266;
    }
  }
}

.record-list {
  background: #fff;
  border-radius: 12rpx;
  padding: 0 20rpx;

  .record-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24rpx 0;
    border-bottom: 1px solid #f0f0f0;

    &:last-child { border-bottom: none; }

    .record-name { font-size: 28rpx; color: #303133; }
    .record-time { font-size: 22rpx; color: #909399; display: block; margin-top: 6rpx; }
    .record-type { font-size: 24rpx; padding: 4rpx 12rpx; border-radius: 4rpx; }
    .record-type.in { background: #f0f9eb; color: #67C23A; }
    .record-type.out { background: #fdf6ec; color: #E6A23C; }
    .record-status { font-size: 22rpx; margin-left: 10rpx; }
    .record-status.pass { color: #67C23A; }
    .record-status.fail { color: #F56C6C; }
  }
}
</style>
