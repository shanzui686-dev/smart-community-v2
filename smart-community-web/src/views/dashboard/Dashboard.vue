<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in statsCards" :key="item.title">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-icon" :style="{ backgroundColor: item.bgColor }">
              <el-icon :size="28" :color="item.color"><component :is="item.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ item.value }}</div>
              <div class="stat-title">{{ item.title }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card>
          <div id="personTypeChart" style="height:350px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <div id="weeklyChart" style="height:350px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { getStatistics } from '@/api/access'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
let loaded = false

const statsCards = ref([
  { title: '小区总数', value: 0, icon: 'HomeFilled', color: '#409EFF', bgColor: '#ecf5ff' },
  { title: '居民总数', value: 0, icon: 'User', color: '#67C23A', bgColor: '#f0f9eb' },
  { title: '今日出入', value: 0, icon: 'Switch', color: '#E6A23C', bgColor: '#fdf6ec' },
  { title: '摄像头数', value: 0, icon: 'VideoCamera', color: '#F56C6C', bgColor: '#fef0f0' }
])

const loadData = async () => {
  if (loaded) return
  if (!userStore.permissions.includes('statistics:dashboard')) return
  loaded = true
  try {
    const { data } = await getStatistics()
    statsCards.value[0].value = data.communityCount || 0
    statsCards.value[1].value = data.personCount || 0
    statsCards.value[2].value = data.todayRecordCount || 0
    statsCards.value[3].value = data.cameraCount || 0

    await nextTick()
    initPersonTypeChart(data.personTypeMap)
    initWeeklyChart(data.weeklyRecordMap)
  } catch {
    loaded = false
  }
}

const initPersonTypeChart = (data) => {
  const chart = echarts.init(document.getElementById('personTypeChart'))
  chart.setOption({
    title: { text: '人员类型分布', left: 'center' },
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: Object.entries(data || {}).map(([k, v]) => ({ name: k, value: v }))
    }]
  })
}

const initWeeklyChart = (data) => {
  const chart = echarts.init(document.getElementById('weeklyChart'))
  const keys = Object.keys(data || {})
  const values = Object.values(data || {})
  chart.setOption({
    title: { text: '近7天出入趋势', left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: keys },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: values,
      itemStyle: { color: '#409EFF' }
    }]
  })
}

onMounted(() => {
  loadData()
  watch(() => userStore.permissions, () => loadData())
})
</script>

<style lang="scss" scoped>
.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;

  .stat-icon {
    width: 56px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
  }

  .stat-info {
    .stat-value {
      font-size: 28px;
      font-weight: bold;
      color: #303133;
    }
    .stat-title {
      font-size: 14px;
      color: #909399;
      margin-top: 4px;
    }
  }
}
</style>
