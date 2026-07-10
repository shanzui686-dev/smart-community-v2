<template>
  <view class="page">
    <view class="picker-bar">
      <select class="picker-select" @change="onSelectChange">
        <option value="">选择小区</option>
        <option v-for="(c, i) in cs" :key="i" :value="i">{{ c.name }}</option>
      </select>
    </view>

    <view class="map-wrap">
      <map class="mp" :latitude="lat" :longitude="lng" :scale="14" :markers="markers" :show-location="true" @markertap="onTap" />
      
      <view class="detail" v-if="detail">
        <text class="dn">{{ detail.name }}</text>
        <text class="da">{{ detail.address }}</text>
        <view class="ds">
          <text>🏢 {{ detail.totalBuilding }} 栋</text>
          <text>🏠 {{ detail.totalHouse }} 户</text>
        </view>
      </view>
    </view>

    <view style="height:100rpx"></view>
    <TabBar current="/pages/map/map" />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { getAllCommunities, isTokenExpired } from '../../utils/request'
import TabBar from '../../components/TabBar.vue'

const cs = ref([])
const names = ref([])
const sel = ref('')
const selectedIndex = ref(0)
const lat = ref(30.5728)
const lng = ref(104.067)
const markers = ref([])
const detail = ref(null)
let isLeaving = false
let isLoading = false

onHide(() => {
  isLeaving = true
})

onShow(async () => {
  isLeaving = false
  if (isLoading) return
  if (isTokenExpired()) {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    uni.redirectTo({ url: '/pages/login/login' })
    return
  }
  if (cs.value.length > 0) return
  isLoading = true
  try {
    const r = await getAllCommunities()
    console.log('getAllCommunities response:', r)
    if (r.code === -1) return
    const raw = JSON.parse(JSON.stringify(r.data || []))
    console.log('communities raw data:', raw)
    cs.value = raw
    names.value = raw.map(c => c.name || '')
    markers.value = raw
      .filter(c => c.mapLat && c.mapLng)
      .map(c => ({
        id: c.communityId,
        latitude: Number(c.mapLat),
        longitude: Number(c.mapLng),
        iconPath: '/static/logo.png',
        width: 30,
        height: 30,
        title: c.name,
        callout: { 
          content: `${c.name} ${c.totalBuilding || 0}栋 ${c.totalHouse || 0}户`, 
          fontSize: 11, 
          padding: 8,
          borderRadius: 6,
          color: '#333',
          bgColor: '#fff'
        }
      }))
    if (raw.length === 0) {
      uni.showToast({ title: '暂无可显示的小区', icon: 'none' })
    }
  } catch (e) {
      console.error(e)
      uni.showToast({ title: '加载小区数据失败', icon: 'none' })
    } finally {
      isLoading = false
    }
    uni.getLocation({
    type: 'gcj02',
    success: r => { lat.value = r.latitude; lng.value = r.longitude },
    fail: () => {}
  })
})

function onSelectChange(e) {
  const i = parseInt(e.target.value)
  if (isNaN(i)) {
    sel.value = ''
    detail.value = null
    return
  }
  selectedIndex.value = i
  const c = cs.value[i]
  if (c) {
    lat.value = Number(c.mapLat) || lat.value
    lng.value = Number(c.mapLng) || lng.value
    sel.value = c.name
    detail.value = c
  }
}

function onTap(e) {
  const c = cs.value.find(x => x.communityId === e.detail.markerId)
  if (c) detail.value = c
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100vh; }
.map-wrap { flex: 1; position: relative; }
.mp { width: 100%; height: 100%; }

.picker-bar {
  display: flex; justify-content: center; align-items: center;
  padding: 20rpx; background: #fff;
  border-bottom: 1rpx solid #f0f0f0;
  z-index: 100;
}
.picker-select {
  font-size: 28rpx; color: #409EFF; font-weight: 500;
  border: 1px solid #409EFF;
  border-radius: 8rpx;
  padding: 10rpx 20rpx;
  outline: none;
  min-width: 300rpx;
  text-align: center;
}

.detail {
  position: absolute; bottom: 120rpx; left: 20rpx; right: 20rpx;
  padding: 24rpx 32rpx; background: rgba(255,255,255,0.95);
  border-radius: 16rpx; box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.1);
  z-index: 100;
}
.dn { font-size: 32rpx; font-weight: bold; color: #303133; }
.da { font-size: 26rpx; color: #909399; margin-top: 8rpx; display: block; }
.ds { display: flex; gap: 40rpx; margin-top: 16rpx; font-size: 26rpx; color: #606266; }
</style>
