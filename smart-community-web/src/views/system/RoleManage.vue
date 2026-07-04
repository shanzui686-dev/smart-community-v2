<template>
  <div class="page-container">
    <div class="page-header">
      <h2>角色管理</h2>
      <el-button type="primary" icon="Plus" @click="handleAdd">新增角色</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="roleId" label="ID" width="80" />
      <el-table-column prop="roleName" label="角色名称" width="150" />
      <el-table-column prop="roleCode" label="角色编码" width="150" />
      <el-table-column prop="description" label="描述" min-width="200" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleAssignMenu(row)">分配权限</el-button>
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="menuDialogVisible" title="分配权限" width="450px">
      <el-tree ref="menuTreeRef" :data="menuTree" show-checkbox node-key="menuId"
        :default-checked-keys="checkedMenuIds" :props="{ label: 'menuName', children: 'children' }" />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveMenus">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoles, addRole, updateRole, deleteRole, getMenus, assignRoleMenus, getRoleMenuIds } from '@/api/system'

const loading = ref(false)
const dialogVisible = ref(false)
const menuDialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = ref('')
const tableData = ref([])
const menuTree = ref([])
const checkedMenuIds = ref([])
const currentRoleId = ref(null)
const menuTreeRef = ref(null)
const formRef = ref(null)

const form = reactive({ roleId: '', roleName: '', roleCode: '', description: '', status: 1 })

const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  const { data } = await getRoles()
  tableData.value = data
  loading.value = false
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增角色'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑角色'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该角色？', '提示', { type: 'warning' })
  await deleteRole(row.roleId)
  ElMessage.success('删除成功')
  loadData()
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, { roleId: '', roleName: '', roleCode: '', description: '', status: 1 })
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (isEdit.value) {
    await updateRole(form)
  } else {
    await addRole(form)
  }
  ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
  dialogVisible.value = false
  loadData()
}

const handleAssignMenu = async (row) => {
  currentRoleId.value = row.roleId
  const { data: menus } = await getMenus()
  menuTree.value = buildTree(menus, 0)
  const { data: ids } = await getRoleMenuIds(row.roleId)
  checkedMenuIds.value = ids
  menuDialogVisible.value = true
}

const buildTree = (list, parentId) => {
  return list.filter(m => m.parentId === parentId).map(m => ({
    ...m,
    children: buildTree(list, m.menuId)
  }))
}

const handleSaveMenus = async () => {
  const keys = [...menuTreeRef.value.getCheckedKeys(), ...menuTreeRef.value.getHalfCheckedKeys()]
  await assignRoleMenus(currentRoleId.value, keys)
  ElMessage.success('权限分配成功')
  menuDialogVisible.value = false
}

onMounted(() => loadData())
</script>
