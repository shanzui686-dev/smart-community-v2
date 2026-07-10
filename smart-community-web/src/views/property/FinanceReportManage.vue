<template>
  <div class="page-container">
    <div class="page-header">
      <h2>财务统计报表</h2>
    </div>

    <!-- 顶部核心指标卡片 -->
    <div class="kpi-cards">
      <div class="kpi-card" style="--card-accent: #409eff">
        <div class="kpi-icon" style="background:#ecf5ff">
          <el-icon :size="28" color="#409eff"><Coin /></el-icon>
        </div>
        <div class="kpi-body">
          <div class="kpi-label">本月总应收</div>
          <div class="kpi-value">￥{{ kpiData.totalReceivable.toLocaleString() }}</div>
          <div class="kpi-sub">含{{ kpiData.billCount }}笔待缴账单</div>
        </div>
      </div>
      <div class="kpi-card" style="--card-accent: #67c23a">
        <div class="kpi-icon" style="background:#f0f9eb">
          <el-icon :size="28" color="#67c23a"><Money /></el-icon>
        </div>
        <div class="kpi-body">
          <div class="kpi-label">本月实收</div>
          <div class="kpi-value">￥{{ kpiData.actualReceived.toLocaleString() }}</div>
          <div class="kpi-sub">已缴{{ kpiData.paidCount }}笔</div>
        </div>
      </div>
      <div class="kpi-card" style="--card-accent: #e6a23c">
        <div class="kpi-icon" style="background:#fdf6ec">
          <el-icon :size="28" color="#e6a23c"><TrendCharts /></el-icon>
        </div>
        <div class="kpi-body">
          <div class="kpi-label">本月收缴率</div>
          <div class="kpi-value" :style="{ color: kpiData.collectionRate >= 80 ? '#67c23a' : '#e6a23c' }">
            {{ kpiData.collectionRate }}%
          </div>
          <el-progress
            :percentage="kpiData.collectionRate"
            :stroke-width="8"
            :color="kpiData.collectionRate >= 80 ? '#67c23a' : '#e6a23c'"
            style="margin-top:4px"
          />
        </div>
      </div>
      <div class="kpi-card" style="--card-accent: #f56c6c">
        <div class="kpi-icon" style="background:#fef0f0">
          <el-icon :size="28" color="#f56c6c"><WarningFilled /></el-icon>
        </div>
        <div class="kpi-body">
          <div class="kpi-label">历史欠费总额</div>
          <div class="kpi-value" style="color:#f56c6c">￥{{ kpiData.historyArrears.toLocaleString() }}</div>
          <div class="kpi-sub">逾期{{ kpiData.overdueCount }}笔</div>
        </div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="chart-row">
      <div class="chart-box">
        <div class="chart-header">
          <span class="chart-title">近 6 个月各费用类型收入趋势</span>
          <el-tag size="small" type="info">单位：元</el-tag>
        </div>
        <div ref="barChartRef" class="chart-container" />
      </div>
      <div class="chart-box">
        <div class="chart-header">
          <span class="chart-title">本年度各项费用收入占比</span>
          <el-tag size="small" type="info">按实收金额</el-tag>
        </div>
        <div ref="pieChartRef" class="chart-container" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { Coin, Money, TrendCharts, WarningFilled } from '@element-plus/icons-vue'
import { getFinancialReport } from '@/api/payment'

// ==================== 数据状态 ====================
const loading = ref(false)
const kpiData = reactive({
  totalReceivable: 0,
  actualReceived: 0,
  collectionRate: 0,
  historyArrears: 0,
  billCount: 0,
  paidCount: 0,
  overdueCount: 0
})

const monthlyIncomeList = ref([])
const feeTypeIncomeList = ref([])

// ==================== ECharts 引用 ====================
const barChartRef = ref(null)
const pieChartRef = ref(null)
let barChart = null
let pieChart = null

// ==================== 加载报表数据 ====================
const loadReportData = async () => {
  loading.value = true
  try {
    const { data } = await getFinancialReport()
    
    kpiData.totalReceivable = Number(data.totalReceivable) || 0
    kpiData.actualReceived = Number(data.actualReceived) || 0
    kpiData.collectionRate = Number(data.collectionRate) || 0
    kpiData.historyArrears = Number(data.historyArrears) || 0
    kpiData.billCount = data.billCount || 0
    kpiData.paidCount = data.paidCount || 0
    kpiData.overdueCount = data.overdueCount || 0

    monthlyIncomeList.value = data.monthlyIncomeList || []
    feeTypeIncomeList.value = data.feeTypeIncomeList || []

    initBarChart()
    initPieChart()
  } catch (error) {
    console.error('加载报表数据失败:', error)
  } finally {
    loading.value = false
  }
}

// ==================== 初始化图表 ====================
const initBarChart = () => {
  if (!barChartRef.value || monthlyIncomeList.value.length === 0) return
  barChart = echarts.init(barChartRef.value)

  const months = monthlyIncomeList.value.map(item => item.month.replace(/^\d{4}-/, ''))
  const propertyFee = monthlyIncomeList.value.map(item => Number(item.propertyFee) || 0)
  const parkingFee = monthlyIncomeList.value.map(item => Number(item.parkingFee) || 0)
  const utilityFee = monthlyIncomeList.value.map(item => Number(item.utilityFee) || 0)
  const otherFee = monthlyIncomeList.value.map(item => Number(item.otherFee) || 0)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        let total = 0
        let html = `<strong>${params[0].axisValue}</strong><br/>`
        params.forEach(p => {
          total += p.value
          html += `${p.marker} ${p.seriesName}：￥${p.value.toLocaleString()}<br/>`
        })
        html += `<hr style="margin:4px 0"/><span style="font-weight:600">合计：￥${total.toLocaleString()}</span>`
        return html
      }
    },
    legend: {
      data: ['物业费', '停车费', '水电公摊', '其他'],
      top: 0,
      itemWidth: 12,
      itemHeight: 12,
      textStyle: { fontSize: 12 }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '14%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: months,
      axisLabel: { fontWeight: 500 },
      axisLine: { lineStyle: { color: '#e6e6e6' } }
    },
    yAxis: {
      type: 'value',
      name: '金额（元）',
      nameTextStyle: { fontSize: 11, color: '#909399' },
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
      axisLabel: {
        formatter: (v) => v >= 10000 ? (v / 10000).toFixed(0) + '万' : v
      }
    },
    series: [
      {
        name: '物业费',
        type: 'bar',
        stack: 'total',
        barWidth: 40,
        itemStyle: { color: '#409eff', borderRadius: [0, 0, 0, 0] },
        data: propertyFee
      },
      {
        name: '停车费',
        type: 'bar',
        stack: 'total',
        itemStyle: { color: '#67c23a' },
        data: parkingFee
      },
      {
        name: '水电公摊',
        type: 'bar',
        stack: 'total',
        itemStyle: { color: '#e6a23c' },
        data: utilityFee
      },
      {
        name: '其他',
        type: 'bar',
        stack: 'total',
        itemStyle: { color: '#909399' },
        data: otherFee
      }
    ]
  }

  barChart.setOption(option)
}

const initPieChart = () => {
  if (!pieChartRef.value || feeTypeIncomeList.value.length === 0) return
  pieChart = echarts.init(pieChartRef.value)

  const pieData = feeTypeIncomeList.value.map(item => ({
    name: item.feeName,
    value: Number(item.amount) || 0
  }))

  const total = pieData.reduce((s, d) => s + d.value, 0)
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399']

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        const pct = total > 0 ? ((params.value / total) * 100).toFixed(1) : '0'
        return `<strong>${params.name}</strong><br/>金额：￥${params.value.toLocaleString()}<br/>占比：${pct}%`
      }
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      itemWidth: 12,
      itemHeight: 12,
      textStyle: { fontSize: 12 },
      formatter: (name) => {
        const item = pieData.find(d => d.name === name)
        const pct = total > 0 ? ((item.value / total) * 100).toFixed(1) : '0'
        return `${name}  ${pct}%`
      }
    },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: true,
        padAngle: 2,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.15)'
          }
        },
        labelLine: { show: false },
        data: pieData.map((item, index) => ({
          value: item.value,
          name: item.name,
          itemStyle: { color: colors[index % colors.length] }
        }))
      }
    ]
  }

  pieChart.setOption(option)
}

// ==================== 自适应 resize ====================
const handleResize = () => {
  barChart?.resize()
  pieChart?.resize()
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadReportData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
/* ========== KPI 卡片 ========== */
.kpi-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.kpi-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  border-left: 4px solid var(--card-accent, #409eff);
  transition: box-shadow 0.25s, transform 0.25s;
}

.kpi-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.kpi-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 12px;
}

.kpi-body {
  flex: 1;
  min-width: 0;
}

.kpi-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.kpi-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  font-family: 'Helvetica Neue', monospace;
  line-height: 1.3;
}

.kpi-sub {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 2px;
}

/* ========== 图表区 ========== */
.chart-row {
  display: flex;
  gap: 16px;
}

.chart-box {
  flex: 1;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  padding: 16px;
  min-width: 0;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.chart-container {
  width: 100%;
  height: 360px;
}
</style>
