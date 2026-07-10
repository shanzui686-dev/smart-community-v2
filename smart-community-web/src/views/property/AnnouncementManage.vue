<template>
  <div class="page-container">
    <div class="page-header">
      <h2>公告管理</h2>
    </div>

    <div class="search-toolbar">
      <div class="toolbar-left">
        <el-select v-model="searchForm.communityId" placeholder="选择小区" clearable style="width:160px" @change="handleSearch">
          <el-option v-for="c in communityList" :key="c.communityId" :label="c.name" :value="c.communityId" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="处理状态" clearable style="width:120px" @change="handleSearch">
          <el-option label="未处理" :value="0" />
          <el-option label="已处理" :value="1" />
        </el-select>
        <el-select v-model="searchForm.expired" placeholder="过期状态" clearable style="width:120px" @change="handleSearch">
          <el-option label="未过期" :value="0" />
          <el-option label="已过期" :value="1" />
        </el-select>
        <el-select v-model="searchForm.top" placeholder="置顶状态" clearable style="width:100px" @change="handleSearch">
          <el-option label="置顶" :value="1" />
          <el-option label="普通" :value="0" />
        </el-select>
        <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
        <el-button icon="Refresh" @click="handleReset">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" icon="Plus" @click="openAddDialog">发布公告</el-button>
      </div>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe style="width:100%">
      <el-table-column prop="title" label="公告标题" min-width="200" />
      <el-table-column prop="communityName" label="所属小区" min-width="140" />
      <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="处理状态" width="90">
        <template #default="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="(val) => handleStatusChange(row, val)" />
        </template>
      </el-table-column>
      <el-table-column prop="expired" label="过期状态" width="90">
        <template #default="{ row }">
          <el-switch v-model="row.expired" :active-value="1" :inactive-value="0" @change="(val) => handleExpiredChange(row, val)" />
        </template>
      </el-table-column>
      <el-table-column prop="top" label="置顶" width="70">
        <template #default="{ row }">
          <el-tag v-if="row.top===1" type="danger">置顶</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createByName" label="发布人" width="90" />
      <el-table-column prop="createTime" label="发布时间" width="170" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.announcementId)">删除</el-button>
          <el-button v-if="row.top===0" size="small" type="success" @click="handleTop(row)">置顶</el-button>
          <el-button v-else size="small" type="warning" @click="handleCancelTop(row)">取消置顶</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top:16px;display:flex;justify-content:flex-end">
      <el-pagination v-model:current-page="searchForm.pageNum" v-model:page-size="searchForm.pageSize"
        :page-sizes="[10,20,50]" :total="total" layout="total,sizes,prev,pager,next" @change="loadData" />
    </div>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="所属小区" required>
          <el-select v-model="form.communityId" placeholder="请选择小区" style="width:100%">
            <el-option v-for="c in communityList" :key="c.communityId" :label="c.name" :value="c.communityId" />
          </el-select>
        </el-form-item>
        <el-form-item label="公告标题" required>
          <el-input v-model="form.title" placeholder="请输入公告标题" style="width:100%" />
        </el-form-item>
        <el-form-item label="公告内容" required>
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告内容" style="width:100%" />
        </el-form-item>
        <el-form-item label="处理状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">未处理</el-radio>
            <el-radio :value="1">已处理</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="过期状态">
          <el-radio-group v-model="form.expired">
            <el-radio :value="0">未过期</el-radio>
            <el-radio :value="1">已过期</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.top" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAnnouncements, addAnnouncement, updateAnnouncement, deleteAnnouncement } from '@/api/property'
import { getAllCommunities } from '@/api/property'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const communityList = ref([])

const searchForm = reactive({
  communityId: null, status: null, expired: null, top: null, pageNum: 1, pageSize: 10
})

const dialogVisible = ref(false)
const dialogTitle = ref('发布公告')
const isEdit = ref(false)

const form = reactive({
  announcementId: null, communityId: null, title: '', content: '', status: 0, expired: 0, top: 0
})

const loadCommunities = async () => {
  const { data } = await getAllCommunities()
  communityList.value = data
}

const loadData = async () => {
  loading.value = true
  try {
    const { data } = await getAnnouncements(searchForm)
    tableData.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

const openAddDialog = () => {
  isEdit.value = false
  dialogTitle.value = '发布公告'
  Object.assign(form, { announcementId: null, communityId: null, title: '', content: '', status: 0, expired: 0, top: 0 })
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑公告'
  Object.assign(form, {
    announcementId: row.announcementId, communityId: row.communityId, title: row.title,
    content: row.content, status: row.status, expired: row.expired, top: row.top
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.communityId) {
    ElMessage.warning('请选择小区')
    return
  }
  if (!form.title) {
    ElMessage.warning('请输入公告标题')
    return
  }
  if (!form.content) {
    ElMessage.warning('请输入公告内容')
    return
  }
  try {
    if (isEdit.value) {
      await updateAnnouncement(form)
      ElMessage.success('修改成功')
    } else {
      await addAnnouncement(form)
      ElMessage.success('发布成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error(isEdit.value ? '修改失败' : '发布失败')
  }
}

const handleDelete = async (id) => {
  await ElMessage.confirm('确定删除该公告吗？', '提示', { type: 'warning' })
  try {
    await deleteAnnouncement(id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    ElMessage.error('删除失败')
  }
}

const handleTop = async (row) => {
  try {
    await updateAnnouncement({ announcementId: row.announcementId, top: 1 })
    ElMessage.success('已置顶')
    loadData()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleCancelTop = async (row) => {
  try {
    await updateAnnouncement({ announcementId: row.announcementId, top: 0 })
    ElMessage.success('已取消置顶')
    loadData()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleStatusChange = async (row, val) => {
  try {
    await updateAnnouncement({ announcementId: row.announcementId, status: val })
    ElMessage.success(val === 1 ? '已标记为已处理' : '已标记为未处理')
    loadData()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleExpiredChange = async (row, val) => {
  try {
    await updateAnnouncement({ announcementId: row.announcementId, expired: val })
    ElMessage.success(val === 1 ? '已标记为已过期' : '已标记为未过期')
    loadData()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleSearch = () => { searchForm.pageNum = 1; loadData() }

const handleReset = () => {
  Object.assign(searchForm, { communityId: null, status: null, expired: null, top: null, pageNum: 1 })
  loadData()
}

onMounted(() => { loadCommunities(); loadData() })
</script>
