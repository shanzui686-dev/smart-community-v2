<template>
  <view class="tabbar">
    <view
      v-for="tab in tabs" :key="tab.path"
      class="tab-item" :class="{ active: current === tab.path }"
      @click="switchTab(tab.path)"
    >
      <text class="tab-icon">{{ tab.icon }}</text>
      <text class="tab-text">{{ tab.text }}</text>
    </view>
  </view>
</template>

<script>
export default {
  props: { current: String },
  data() {
    return {
      tabs: [
        { path: '/pages/index/index', icon: '🏠', text: '首页' },
        { path: '/pages/announcements/announcements', icon: '📢', text: '公告' },
        { path: '/pages/map/map', icon: '🗺️', text: '地图' },
        { path: '/pages/mine/mine', icon: '👤', text: '我的' }
      ]
    }
  },
  methods: {
    switchTab(path) {
      if (path === this.current) return
      uni.navigateTo({ url: path })
    }
  }
}
</script>

<style>
.tabbar {
  position: fixed; bottom: 0; left: 0; right: 0; z-index: 999;
  display: flex; align-items: center;
  background: #fff;
  border-top: 1px solid rgba(0,0,0,.05);
  box-shadow: 0 -2rpx 12rpx rgba(0,0,0,.03);
  padding: 8rpx 0 calc(8rpx + env(safe-area-inset-bottom));
}
.tab-item {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 6rpx 0; min-height: 88rpx;
  transition: color .2s;
}
.tab-icon { font-size: 44rpx; line-height: 1.2; }
.tab-text { font-size: 20rpx; margin-top: 2rpx; color: #999; font-weight: 400; }
.tab-item.active .tab-text { color: #409EFF; font-weight: 500; }
</style>
