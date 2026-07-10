<template>
  <div class="page-container">
    <div class="page-header">
      <h2>用户管理</h2>
    </div>

    <div class="search-toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchForm.keyword" placeholder="用户名/姓名/手机号" clearable style="width:220px" @change="handleSearch" />
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width:120px" @change="handleSearch">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
      <el-button icon="Refresh" @click="handleReset">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" icon="Plus" @click="handleAdd">新增用户</el-button>
      </div>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="userId" label="ID" width="80" />
      <el-table-column label="头像" width="70">
        <template #default="{ row }">
          <el-avatar :src="row._signedAvatar || row.avatar" :size="40" />
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="mobile" label="手机号" width="130" />
      <el-table-column prop="email" label="邮箱" min-width="160" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="(val) => handleStatusChange(row, val)" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="hasPermission" type="warning" link @click="handleResetPwd(row)">重置密码</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top:16px;display:flex;justify-content:flex-end">
      <el-pagination v-model:current-page="searchForm.pageNum" v-model:page-size="searchForm.pageSize"
        :page-sizes="[10,20,50]" :total="total" layout="total,sizes,prev,pager,next" @change="loadData" />
    </div>

    <!-- 新增/编辑用户 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="mobile">
          <el-input v-model="form.mobile" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width:100%">
            <el-option
              v-for="role in roleList"
              :key="role.roleId"
              :label="role.roleName"
              :value="role.roleId"
              :disabled="role.status !== 1"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <el-upload
            class="avatar-uploader"
            :show-file-list="false"
            :http-request="handleAvatarUpload"
            accept="image/*"
          >
            <el-avatar v-if="form.avatar" :src="form._signedAvatar || form.avatar" :size="80" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="400px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePwdSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, addUser, updateUser, deleteUser, getRoles, getUser, uploadFile } from '@/api/system'
import { useUserStore } from '@/stores/user'
import { getSignedUrl } from '@/utils/oss'
import request from '@/utils/request'

const userStore = useUserStore()
const hasPermission = computed(() => userStore.permissions.includes('system:user:edit'))

const loading = ref(false)
const dialogVisible = ref(false)
const pwdDialogVisible = ref(false)
const dialogTitle = ref('')
const isEdit = ref(false)
const formRef = ref(null)
const pwdFormRef = ref(null)
const tableData = ref([])
const total = ref(0)
const currentUserId = ref(null)
const roleList = ref([])

const searchForm = reactive({ keyword: '', status: null, pageNum: 1, pageSize: 10 })

const form = reactive({
  userId: '', username: '', password: '123456', realName: '', mobile: '', email: '', avatar: '', status: 1, roleIds: []
})

const pwdForm = reactive({ newPassword: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  mobile: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
}

const pwdRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

const loadData = async () => {
  loading.value = true
  try {
    const { data } = await getUsers(searchForm)
    for (const record of data.records) {
      if (record.avatar) {
        record._signedAvatar = await getSignedUrl(record.avatar)
      }
    }
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { searchForm.pageNum = 1; loadData() }
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.status = null
  searchForm.pageNum = 1
  loadData()
}

const handleAdd = async () => {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  await fetchRoles()
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  const { data } = await getUser(row.userId)
  Object.assign(form, data)
  if (form.avatar) {
    form._signedAvatar = await getSignedUrl(form.avatar)
  }
  await fetchRoles()
  dialogVisible.value = true
}

const fetchRoles = async () => {
  try {
    const { data } = await getRoles()
    roleList.value = data
  } catch {
    roleList.value = []
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该用户？', '提示', { type: 'warning' })
  await deleteUser(row.userId)
  ElMessage.success('删除成功')
  loadData()
}

const handleAvatarUpload = async (options) => {
  const formData = new FormData()
  formData.append('file', options.file)
  formData.append('dir', 'avatar')
  try {
    const { data } = await uploadFile(formData)
    form.avatar = data
    form._signedAvatar = await getSignedUrl(data)
    ElMessage.success('头像上传成功')
  } catch {
    ElMessage.error('头像上传失败')
  }
}

const handleResetPwd = (row) => {
  currentUserId.value = row.userId
  pwdForm.newPassword = ''
  pwdDialogVisible.value = true
}

const handlePwdSubmit = async () => {
  await pwdFormRef.value.validate()
  await request.put(`/system/user/${currentUserId.value}/reset-password`, { newPassword: pwdForm.newPassword })
  ElMessage.success('密码重置成功')
  pwdDialogVisible.value = false
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, { userId: '', username: '', password: '123456', realName: '', mobile: '', email: '', avatar: '', status: 1, roleIds: [] })
}

const handleStatusChange = async (row, val) => {
  try {
    await updateUser({ userId: row.userId, status: val })
    ElMessage.success(val === 1 ? '已启用' : '已禁用')
    loadData()
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (isEdit.value) {
    await updateUser(form)
  } else {
    await addUser(form)
  }
  // 如果编辑的是当前登录用户，同步更新 store 中的头像
  if (isEdit.value && form.userId === userStore.userId) {
    userStore.avatar = form.avatar
  }
  ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
  dialogVisible.value = false
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped>
.avatar-uploader {
  cursor: pointer;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 80px;
  height: 80px;
  line-height: 80px;
  text-align: center;
  border: 1px dashed #d9d9d9;
  border-radius: 50%;
}
.avatar-uploader-icon:hover {
  border-color: #409eff;
}
</style>
