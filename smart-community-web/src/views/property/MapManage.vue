<template>
  <div class="page-container">
    <div class="page-header">
      <h2>地图管理</h2>
    </div>
    <div class="map-wrapper">
      <div id="map-container" />
      <div class="map-sidebar">
        <el-input v-model="searchKeyword" placeholder="搜索小区名称" clearable prefix-icon="Search" @input="handleSearch" />
        <div class="community-list">
          <div
            v-for="c in filteredCommunities" :key="c.communityId"
            class="community-item"
            :class="{ active: activeCommunityId === c.communityId }"
            @click="locateCommunity(c)"
          >
            <div class="community-name">{{ c.name }}</div>
            <div class="community-addr">{{ c.address || '暂无地址' }}</div>
          </div>
          <el-empty v-if="filteredCommunities.length === 0" description="无匹配小区" :image-size="60" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getAllCommunities } from '@/api/property'

const searchKeyword = ref('')
const activeCommunityId = ref(null)
let mapInstance = null
let markersMap = {}
const allCommunities = ref([])

const filteredCommunities = computed(() => {
  if (!searchKeyword.value) return allCommunities.value
  const kw = searchKeyword.value.toLowerCase()
  return allCommunities.value.filter(c => c.name && c.name.toLowerCase().includes(kw))
})

onMounted(async () => {
  try {
    const { data: communities } = await getAllCommunities()
    allCommunities.value = communities || []

    const script = document.createElement('script')
    script.src = 'https://webapi.amap.com/maps?v=2.0&key=65146e35e3f8c076f2857ae5454a2d81'
    script.onload = () => initMap(communities || [])
    document.head.appendChild(script)
  } catch (e) {
    console.error('加载小区数据失败:', e)
  }
})

const initMap = (communities) => {
  mapInstance = new window.AMap.Map('map-container', {
    zoom: 12,
    center: [104.065, 30.657],
    resizeEnable: true
  })

  if (!communities.length) return

  const markers = []
  communities.forEach(c => {
    if (!c.mapLng || !c.mapLat) return

    const marker = new window.AMap.Marker({
      position: [c.mapLng, c.mapLat],
      title: c.name,
      label: { content: c.name, offset: new window.AMap.Pixel(0, -30) }
    })

    const infoWindow = new window.AMap.InfoWindow({
      content: `<div style="padding:8px">
        <strong>${c.name}</strong><br/>
        地址：${c.address || '-'}<br/>
        楼栋：${c.totalBuilding || 0}栋 | 户数：${c.totalHouse || 0}户
      </div>`,
      offset: new window.AMap.Pixel(0, -35)
    })

    marker.on('click', () => {
      infoWindow.open(mapInstance, marker.getPosition())
      activeCommunityId.value = c.communityId
    })
    markers.push(marker)
    markersMap[c.communityId] = marker
  })

  mapInstance.add(markers)
  if (markers.length > 0) mapInstance.setFitView(markers)
}

const handleSearch = () => {
  // 搜索时过滤列表，不自动定位
}

const locateCommunity = (c) => {
  if (!mapInstance || !c.mapLng || !c.mapLat) return
  activeCommunityId.value = c.communityId
  mapInstance.setZoomAndCenter(16, [c.mapLng, c.mapLat])
  const marker = markersMap[c.communityId]
  if (marker) {
    // 触发 marker 点击事件显示 infoWindow
    marker.emit('click', { target: marker })
  }
}
</script>

<style scoped>
.map-wrapper {
  display: flex;
  height: calc(100vh - 100px);
  gap: 12px;
}
#map-container {
  flex: 1;
  border-radius: 8px;
}
.map-sidebar {
  width: 280px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.community-list {
  flex: 1;
  overflow-y: auto;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  padding: 8px;
}
.community-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}
.community-item:hover { background: #ecf5ff; }
.community-item.active { background: #d9ecff; border-left: 3px solid #409eff; }
.community-name {
  font-weight: 500;
  font-size: 14px;
  color: #303133;
}
.community-addr {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
</style>
