<template>
  <div class="page-container">
    <div class="page-header">
      <h2>出入记录</h2>
    </div>

    <div class="search-toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchForm.personName" placeholder="姓名" clearable style="width:160px" @change="handleSearch" />
        <el-select v-model="searchForm.communityId" placeholder="选择小区" clearable style="width:160px" @change="handleSearch">
          <el-option v-for="c in communityList" :key="c.communityId" :label="c.name" :value="c.communityId" />
        </el-select>
        <el-select v-model="searchForm.type" placeholder="出入类型" clearable style="width:130px" @change="handleSearch">
          <el-option label="进入" :value="1" />
          <el-option label="外出" :value="2" />
        </el-select>
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
          end-placeholder="结束日期" value-format="YYYY-MM-DD" @change="handleDateChange" style="width:260px" />
        <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
        <el-button icon="Refresh" @click="handleReset">重置</el-button>
      </div>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe style="width:100%">
      <el-table-column prop="personName" label="姓名" min-width="100" />
      <el-table-column prop="communityName" label="小区" min-width="140" />
      <el-table-column label="门牌号" min-width="120">
        <template #default="{ row }">
          {{ row.houseNo || row.location || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="verifyType" label="验证方式" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.verifyType===1" type="primary">人脸识别</el-tag>
          <el-tag v-else-if="row.verifyType===2" type="warning">门禁钥匙</el-tag>
          <el-tag v-else-if="row.verifyType===3" type="info">访客登记</el-tag>
          <el-tag v-else-if="row.verifyType===4" type="success">车牌识别</el-tag>
          <el-tag v-else type="info">其他</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="verified" label="验证结果" width="90">
        <template #default="{ row }">
          <el-tag :type="row.verified===1?'success':'danger'">{{ row.verified===1?'通过':'未通过' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="进入" min-width="170">
        <template #default="{ row }">
          {{ row.time || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="外出" min-width="170">
        <template #default="{ row }">
          {{ row.outTime || '-' }}
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top:16px;display:flex;justify-content:flex-end">
      <el-pagination v-model:current-page="searchForm.pageNum" v-model:page-size="searchForm.pageSize"
        :page-sizes="[10,20,50]" :total="total" layout="total,sizes,prev,pager,next" @change="loadData" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getRecords } from '@/api/access'
import { getAllCommunities } from '@/api/property'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const communityList = ref([])
const dateRange = ref([])

const searchForm = reactive({
  personName: '', communityId: null, type: null, startTime: '', endTime: '', pageNum: 1, pageSize: 10
})

const loadCommunities = async () => {
  const { data } = await getAllCommunities()
  communityList.value = data
}

const loadData = async () => {
  loading.value = true
  try {
    const { data } = await getRecords(searchForm)
    tableData.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

const handleSearch = () => { searchForm.pageNum = 1; loadData() }

const handleReset = () => {
  searchForm.personName = ''; searchForm.communityId = null; searchForm.type = null
  searchForm.startTime = ''; searchForm.endTime = ''
  dateRange.value = []
  searchForm.pageNum = 1; loadData()
}

const handleDateChange = (val) => {
  if (val) {
    searchForm.startTime = val[0]
    searchForm.endTime = val[1]
  } else {
    searchForm.startTime = ''
    searchForm.endTime = ''
  }
  handleSearch()
}

onMounted(() => { loadCommunities(); loadData() })
</script>
