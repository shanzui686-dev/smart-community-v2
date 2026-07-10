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

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="announcement-header">
              <span class="announcement-title-text">最新公告</span>
              <span class="announcement-view-all">查看全部 &gt;</span>
            </div>
          </template>
          <div v-if="announcements.length > 0" class="announcement-list">
            <div v-for="item in announcements" :key="item.announcementId" class="announcement-group">
              <div class="announcement-item" @click="toggleExpand(item.announcementId)">
                <div class="announcement-main">
                  <el-icon class="announcement-icon"><Bell /></el-icon>
                  <el-tag v-if="item.top === 1" type="danger" size="small">紧急</el-tag>
                  <el-tag v-else type="info" size="small">系统通知</el-tag>
                  <span class="announcement-title">{{ item.title }}</span>
                  <el-tag :type="item.status===1?'success':'warning'" size="small">{{ item.status===1?'已处理':'未处理' }}</el-tag>
                  <el-tag :type="item.expired===1?'danger':'success'" size="small">{{ item.expired===1?'已过期':'未过期' }}</el-tag>
                </div>
                <div class="announcement-meta">
                  <span class="announcement-community">{{ item.communityName }}</span>
                  <span class="announcement-time">{{ item.createTime }}</span>
                  <el-icon class="expand-icon" :class="{ expanded: expandedId === item.announcementId }">
                    <ArrowDown />
                  </el-icon>
                </div>
              </div>
              <transition name="expand">
                <div v-if="expandedId === item.announcementId" class="announcement-content">
                  <p>{{ item.content }}</p>
                </div>
              </transition>
            </div>
          </div>
          <div v-else class="empty-state">
            <el-icon class="empty-icon"><Bell /></el-icon>
            <p class="empty-text">暂无最新公告</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { getStatistics } from '@/api/access'
import { getHomeAnnouncements } from '@/api/property'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
let loaded = false

const statsCards = ref([
  { title: '小区总数', value: 0, icon: 'HomeFilled', color: '#409EFF', bgColor: '#ecf5ff' },
  { title: '居民总数', value: 0, icon: 'User', color: '#67C23A', bgColor: '#f0f9eb' },
  { title: '今日出入', value: 0, icon: 'Switch', color: '#E6A23C', bgColor: '#fdf6ec' },
  { title: '摄像头数', value: 0, icon: 'VideoCamera', color: '#F56C6C', bgColor: '#fef0f0' }
])

const announcements = ref([])
const expandedId = ref(null)

const toggleExpand = (id) => {
  expandedId.value = expandedId.value === id ? null : id
}

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

const loadAnnouncements = async () => {
  try {
    const { data } = await getHomeAnnouncements()
    announcements.value = data
  } catch {
    announcements.value = []
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
  loadAnnouncements()
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

.announcement-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .announcement-title-text {
    font-weight: 500;
    font-size: 15px;
    color: #303133;
  }

  .announcement-view-all {
    font-size: 12px;
    color: #909399;
    cursor: pointer;
    transition: color 0.2s ease;

    &:hover {
      color: #409EFF;
    }
  }
}

.announcement-list {
  padding: 8px 0;

  .announcement-group {
    margin-bottom: 4px;
    border-radius: 6px;
    overflow: hidden;

    .announcement-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 16px;
      cursor: pointer;
      transition: background-color 0.25s ease;

      &:hover {
        background-color: #f5f7fa;
      }

      .announcement-main {
        display: flex;
        align-items: center;
        flex: 1;
        min-width: 0;

        .announcement-icon {
          font-size: 16px;
          color: #409EFF;
          margin-right: 10px;
          flex-shrink: 0;
        }

        :deep(.el-tag) {
          margin-right: 10px;
          flex-shrink: 0;
        }

        .announcement-title {
          flex: 1;
          font-size: 14px;
          color: #303133;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }

      .announcement-meta {
        display: flex;
        align-items: center;
        margin-left: 16px;
        flex-shrink: 0;

        .announcement-community {
          font-size: 12px;
          color: #909399;
          margin-right: 16px;
        }

        .announcement-time {
          font-size: 12px;
          color: #909399;
          margin-right: 8px;
        }

        .expand-icon {
          font-size: 14px;
          color: #c0c4cc;
          transition: transform 0.3s ease;

          &.expanded {
            transform: rotate(180deg);
          }
        }
      }
    }

    .announcement-content {
      padding: 16px 16px 20px 52px;
      background-color: #f8f9fa;
      border-top: 1px solid #f0f0f0;

      p {
        margin: 0;
        font-size: 14px;
        color: #606266;
        line-height: 1.6;
        white-space: pre-wrap;
        word-break: break-word;
      }
    }
  }
}

.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
}

.expand-enter-to,
.expand-leave-from {
  max-height: 500px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 0;

  .empty-icon {
    font-size: 48px;
    color: #c0c4cc;
    margin-bottom: 12px;
  }

  .empty-text {
    font-size: 14px;
    color: #909399;
    margin: 0;
  }
}
</style>
