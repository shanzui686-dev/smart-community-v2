<template>
  <div class="page-container">
    <div class="page-header">
      <h2>操作日志</h2>
    </div>

    <div class="search-toolbar">
      <el-input v-model="searchForm.keyword" placeholder="用户名/模块" clearable style="width:220px" @change="handleSearch" />
      <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
      <el-button icon="Refresh" @click="handleReset">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="logId" label="ID" width="80" />
      <el-table-column prop="username" label="操作用户" width="100" />
      <el-table-column prop="module" label="模块" width="100" />
      <el-table-column prop="operation" label="操作类型" width="100" />
      <el-table-column prop="method" label="请求方法" min-width="200" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP地址" width="140" />
      <el-table-column prop="duration" label="耗时(ms)" width="90" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status===1?'success':'danger'">{{ row.status===1?'成功':'失败' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="操作时间" width="170" />
    </el-table>

    <div style="margin-top:16px;display:flex;justify-content:flex-end">
      <el-pagination v-model:current-page="searchForm.pageNum" v-model:page-size="searchForm.pageSize"
        :page-sizes="[10,20,50]" :total="total" layout="total,sizes,prev,pager,next" @change="loadData" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getOperationLogs } from '@/api/system'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)

const searchForm = reactive({ keyword: '', pageNum: 1, pageSize: 10 })

const loadData = async () => {
  loading.value = true
  try {
    const { data } = await getOperationLogs(searchForm)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { searchForm.pageNum = 1; loadData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.pageNum = 1; loadData() }

onMounted(() => loadData())
</script>
