<template>
  <view class="home">
    <!-- 自定义导航栏 -->
    <view class="navbar">
      <view class="navbar-spacer" />
      <text class="navbar-title">智慧小区</text>
      <view class="navbar-spacer" />
    </view>

    <!-- 数据看板 -->
    <view class="stats-row">
      <view class="stat-card">
        <text class="stat-num">{{ stats.communityCount }}<text class="stat-unit">个</text></text>
        <text class="stat-label">小区</text>
      </view>
      <view class="stat-card">
        <text class="stat-num">{{ stats.personCount }}<text class="stat-unit">人</text></text>
        <text class="stat-label">居民</text>
      </view>
      <view class="stat-card">
        <text class="stat-num">{{ stats.todayRecordCount }}<text class="stat-unit">次</text></text>
        <text class="stat-label">今日出入</text>
      </view>
    </view>

    <!-- 功能服务金刚区 -->
    <view class="panel">
      <text class="panel-title">功能服务</text>
      <view class="feature-grid">
        <view class="feature-item" @click="go('/pages/face/face')">
          <view class="fi-icon fi-blue"><text>📷</text></view>
          <text class="fi-name">人脸门禁</text>
        </view>
        <view class="feature-item" @click="go('/pages/scan/scan')">
          <view class="fi-icon fi-green"><text>📱</text></view>
          <text class="fi-name">扫码查询</text>
        </view>
        <view class="feature-item" @click="go('/pages/records/records')">
          <view class="fi-icon fi-cyan"><text>📋</text></view>
          <text class="fi-name">出入记录</text>
        </view>
        <view class="feature-item" @click="go('/pages/visitors/visitors')">
          <view class="fi-icon fi-orange"><text>🚶</text></view>
          <text class="fi-name">访客管理</text>
        </view>
        <view class="feature-item" @click="go('/pages/persons/persons')">
          <view class="fi-icon fi-pink"><text>👥</text></view>
          <text class="fi-name">居民管理</text>
        </view>
        <view class="feature-item" @click="go('/pages/vehicles/vehicles')">
          <view class="fi-icon fi-teal"><text>🚗</text></view>
          <text class="fi-name">车辆管理</text>
        </view>
        <view class="feature-item" @click="go('/pages/bills/bills')">
          <view class="fi-icon fi-teal"><text>💰</text></view>
          <text class="fi-name">物业缴费</text>
        </view>
        <view class="feature-item" @click="go('/pages/map/map')">
          <view class="fi-icon fi-purple"><text>🗺️</text></view>
          <text class="fi-name">小区地图</text>
        </view>
        <view class="feature-item" @click="go('/pages/announcements/announcements')">
          <view class="fi-icon fi-red"><text>📢</text></view>
          <text class="fi-name">小区公告</text>
        </view>
      </view>
    </view>

    <!-- 最新公告 -->
    <view class="panel" v-if="announcements.length">
      <view class="panel-header">
        <text class="panel-title">最新公告</text>
        <text class="panel-more" @click="go('/pages/announcements/announcements')">查看全部 ›</text>
      </view>
      <view class="anno-list">
        <view class="anno-item" v-for="(a, i) in announcements" :key="i" @click="go('/pages/announcements/announcements')">
          <view class="anno-dot" />
          <text class="anno-text">{{ a.title }}</text>
          <text class="anno-date">{{ a.createTime }}</text>
        </view>
      </view>
    </view>

    <!-- 最新出入记录 -->
    <view class="panel" v-if="records.length">
      <view class="panel-header">
        <text class="panel-title">出入动态</text>
        <text class="panel-more" @click="go('/pages/records/records')">查看全部 ›</text>
      </view>
      <view class="record-list">
        <view class="record-item" v-for="r in records" :key="r.recordId">
          <view class="record-left">
            <view class="record-avatar">{{ (r.personName || '?')[0] }}</view>
            <view class="record-info">
              <text class="record-name">{{ r.personName }}</text>
              <text class="record-time">{{ r.accessTime }}</text>
            </view>
          </view>
          <text :class="['record-badge', r.accessType === 1 ? 'badge-in' : 'badge-out']">
            {{ r.accessType === 1 ? '进入' : '外出' }}
          </text>
        </view>
      </view>
    </view>

    <view class="bottom-safe" />
    <TabBar current="/pages/index/index" />
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { getStatistics, getRecords, getHomeAnnouncements, isTokenExpired } from '../../utils/request'
import TabBar from '../../components/TabBar.vue'

const stats = reactive({ communityCount: 0, personCount: 0, todayRecordCount: 0 })
const records = ref([])
const announcements = ref([])
let isLeaving = false

onHide(() => {
  isLeaving = true
})

onShow(async () => {
  if (isTokenExpired()) {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    uni.redirectTo({ url: '/pages/login/login' })
    return
  }
  isLeaving = false
  try { 
    const r = await getStatistics()
    if (r.code === -1) return
    if (r.data) Object.assign(stats, r.data) 
  } catch {}
  try { 
    const r = await getRecords({ pageNum: 1, pageSize: 5 })
    if (r.code === -1) return
    if (r.data?.records) records.value = r.data.records 
  } catch {}
  try { 
    const a = await getHomeAnnouncements()
    if (a.code === -1) return
    announcements.value = (a.data || []).slice(0, 3) 
  } catch {}
})

function go(url) { uni.navigateTo({ url }) }
</script>

<style scoped>
/* ===== 全局背景 ===== */
.home {
  min-height: 100vh;
  background: #F4F6F9;
}

/* ===== 导航栏 ===== */
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 96rpx;
  padding: 0 40rpx;
  background: #fff;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.03);
  position: sticky;
  top: 0;
  z-index: 100;
}
.navbar-spacer {
  width: 80rpx;
}
.navbar-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #1A1A1A;
  letter-spacing: 2rpx;
}

/* ===== 数据看板 ===== */
.stats-row {
  display: flex;
  gap: 24rpx;
  padding: 28rpx 32rpx 0;
}
.stat-card {
  flex: 1;
  background: #fff;
  border-radius: 24rpx;
  padding: 36rpx 16rpx 32rpx;
  text-align: center;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.03);
}
.stat-num {
  font-size: 56rpx;
  font-weight: 700;
  color: #1A1A1A;
  line-height: 1.1;
}
.stat-unit {
  font-size: 26rpx;
  font-weight: 400;
  color: #999;
  margin-left: 4rpx;
}
.stat-label {
  display: block;
  font-size: 24rpx;
  color: #999;
  margin-top: 12rpx;
  letter-spacing: 1rpx;
}

/* ===== 通用面板 ===== */
.panel {
  padding: 36rpx 32rpx 0;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}
.panel-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1A1A1A;
  letter-spacing: 1rpx;
}
.panel-more {
  font-size: 26rpx;
  color: #7B9EFF;
}

/* ===== 功能服务 ===== */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 36rpx 24rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.03);
}
.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.fi-icon {
  width: 100rpx;
  height: 100rpx;
  border-radius: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
  margin-bottom: 14rpx;
}
.fi-blue   { background: #EEF2FF; }
.fi-green  { background: #EDF7EE; }
.fi-cyan   { background: #EAF4FC; }
.fi-orange { background: #FFF5EB; }
.fi-pink   { background: #FDEDF2; }
.fi-teal   { background: #EBF6F2; }
.fi-purple { background: #F3EDF8; }
.fi-red    { background: #FCEEEF; }
.fi-name {
  font-size: 24rpx;
  color: #333;
  text-align: center;
  line-height: 1.3;
}

/* ===== 最新公告 ===== */
.anno-list {
  background: #fff;
  border-radius: 24rpx;
  padding: 12rpx 32rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.03);
}
.anno-item {
  display: flex;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1rpx solid #F2F3F5;
}
.anno-item:last-child {
  border-bottom: none;
}
.anno-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #7B9EFF;
  margin-right: 20rpx;
  flex-shrink: 0;
}
.anno-text {
  flex: 1;
  font-size: 28rpx;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.anno-date {
  font-size: 24rpx;
  color: #bbb;
  margin-left: 20rpx;
  flex-shrink: 0;
}

/* ===== 出入记录 ===== */
.record-list {
  background: #fff;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.03);
}
.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid #F2F3F5;
}
.record-item:last-child {
  border-bottom: none;
}
.record-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.record-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #7B9EFF, #A3B8FF);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  color: #fff;
  font-weight: 600;
  flex-shrink: 0;
}
.record-info {
  display: flex;
  flex-direction: column;
}
.record-name {
  font-size: 28rpx;
  color: #1A1A1A;
  font-weight: 500;
}
.record-time {
  font-size: 24rpx;
  color: #999;
  margin-top: 6rpx;
}
.record-badge {
  font-size: 24rpx;
  padding: 10rpx 24rpx;
  border-radius: 20rpx;
  font-weight: 500;
  letter-spacing: 1rpx;
}
.badge-in  { background: #EDF7EE; color: #52B85A; }
.badge-out { background: #FFF5EB; color: #E6A23C; }

/* ===== 底部安全区 ===== */
.bottom-safe {
  height: calc(140rpx + env(safe-area-inset-bottom));
}
</style>
