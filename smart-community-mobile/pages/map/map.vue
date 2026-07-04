<template>
  <view class="map-page">
    <view class="map-header">
      <picker mode="selector" :range="communityNames" @change="onCommunityChange">
        <view class="community-picker">
          <text>{{ selectedCommunity || '选择小区' }}</text>
          <text class="arrow">▼</text>
        </view>
      </picker>
    </view>

    <view class="map-container">
      <map
        id="baiduMap"
        class="map"
        :latitude="currentLat"
        :longitude="currentLng"
        :scale="15"
        :markers="markers"
        :show-location="true"
        @markertap="onMarkerTap"
      />
    </view>

    <view class="community-detail" v-if="selectedDetail">
      <view class="detail-card">
        <text class="detail-name">{{ selectedDetail.name }}</text>
        <text class="detail-address">{{ selectedDetail.address }}</text>
        <view class="detail-stats">
          <text class="stat">楼栋: {{ selectedDetail.totalBuilding }}</text>
          <text class="stat">房屋: {{ selectedDetail.totalHouse }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCommunities } from '../../utils/request'

const communityList = ref([])
const communityNames = ref([])
const selectedCommunity = ref('')
const currentLat = ref(30.2741) // 默认：成都
const currentLng = ref(120.1551) // 默认：杭州
const markers = ref([])
const selectedDetail = ref(null)

const loadCommunities = async () => {
  try {
    const res = await getCommunities()
    communityList.value = res.data || []
    communityNames.value = communityList.value.map(c => c.name)

    // 设置标记点
    markers.value = communityList.value
      .filter(c => c.mapLat && c.mapLng)
      .map(c => ({
        id: c.communityId,
        latitude: Number(c.mapLat),
        longitude: Number(c.mapLng),
        title: c.name,
        callout: { content: c.name, fontSize: 14, padding: 8 }
      }))
  } catch (e) {}
}

const onCommunityChange = (e) => {
  const idx = e.detail.value
  const community = communityList.value[idx]
  if (community && community.mapLat && community.mapLng) {
    currentLat.value = Number(community.mapLat)
    currentLng.value = Number(community.mapLng)
    selectedCommunity.value = community.name
    selectedDetail.value = community
  }
}

const onMarkerTap = (e) => {
  const community = communityList.value.find(c => c.communityId === e.detail.markerId)
  if (community) {
    selectedDetail.value = community
  }
}

onMounted(() => {
  // 获取当前位置
  uni.getLocation({
    type: 'gcj02',
    success: (res) => {
      currentLat.value = res.latitude
      currentLng.value = res.longitude
    },
    fail: () => {}
  })
  loadCommunities()
})
</script>

<style lang="scss" scoped>
.map-page {
  display: flex;
  flex-direction: column;
  height: 100vh;

  .map-header {
    padding: 20rpx;
    background: #fff;

    .community-picker {
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 16rpx;
      background: #f5f7fa;
      border-radius: 8rpx;
      font-size: 28rpx;
      color: #409EFF;

      .arrow { font-size: 20rpx; margin-left: 10rpx; }
    }
  }

  .map-container {
    flex: 1;

    .map {
      width: 100%;
      height: 100%;
    }
  }

  .community-detail {
    padding: 20rpx;

    .detail-card {
      background: #fff;
      border-radius: 12rpx;
      padding: 30rpx;

      .detail-name {
        font-size: 34rpx;
        font-weight: bold;
        color: #303133;
      }
      .detail-address {
        font-size: 26rpx;
        color: #909399;
        margin-top: 10rpx;
        display: block;
      }
      .detail-stats {
        display: flex;
        gap: 40rpx;
        margin-top: 20rpx;

        .stat {
          font-size: 26rpx;
          color: #606266;
        }
      }
    }
  }
}
</style>
