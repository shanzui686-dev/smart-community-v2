<template>
  <div class="page-container">
    <div class="page-header">
      <h2>菜单管理</h2>
      <el-button type="primary" icon="Plus" @click="handleAdd(0)">新增菜单</el-button>
    </div>

    <el-table :data="menuTree" v-loading="loading" border stripe row-key="menuId" default-expand-all>
      <el-table-column prop="menuName" label="菜单名称" min-width="200" />
      <el-table-column prop="icon" label="图标" width="80" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="permission" label="权限标识" min-width="160" />
      <el-table-column prop="path" label="路由路径" width="180" />
      <el-table-column prop="type" label="类型" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.type===1" type="info">目录</el-tag>
          <el-tag v-else-if="row.type===2" type="primary">菜单</el-tag>
          <el-tag v-else type="warning">按钮</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.type!==3" type="primary" link @click="handleAdd(row.menuId)">添加子菜单</el-button>
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="上级菜单">
          <el-input :model-value="parentMenuName" disabled />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">目录</el-radio>
            <el-radio :value="2">菜单</el-radio>
            <el-radio :value="3">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" />
        </el-form-item>
        <el-form-item label="图标" v-if="form.type!==3">
          <el-input v-model="form.icon" placeholder="Element图标名" />
        </el-form-item>
        <el-form-item label="路由路径" v-if="form.type===2">
          <el-input v-model="form.path" />
        </el-form-item>
        <el-form-item label="组件路径" v-if="form.type===2">
          <el-input v-model="form.component" />
        </el-form-item>
        <el-form-item label="权限标识" prop="permission" v-if="form.type!==1">
          <el-input v-model="form.permission" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMenus, addMenu, updateMenu, deleteMenu } from '@/api/system'

const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const rawMenus = ref([])
const menuTree = ref([])
const parentMenuName = ref('顶级菜单')

const form = reactive({
  menuId: '', menuName: '', parentId: 0, path: '', component: '', icon: '', sort: 0, type: 1, permission: ''
})

const rules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  permission: [{ required: true, message: '请输入权限标识', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  const { data } = await getMenus()
  rawMenus.value = data
  menuTree.value = buildTree(data, 0)
  loading.value = false
}

const buildTree = (list, parentId) => {
  return list.filter(m => m.parentId === parentId).map(m => ({
    ...m,
    children: buildTree(list, m.menuId)
  }))
}

const handleAdd = (parentId) => {
  isEdit.value = false
  dialogTitle.value = '新增菜单'
  parentMenuName.value = parentId === 0 ? '顶级菜单' : rawMenus.value.find(m => m.menuId === parentId)?.menuName || ''
  Object.assign(form, { menuId: '', menuName: '', parentId, path: '', component: '', icon: '', sort: 0, type: 2, permission: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑菜单'
  parentMenuName.value = row.parentId === 0 ? '顶级菜单' : rawMenus.value.find(m => m.menuId === row.parentId)?.menuName || ''
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该菜单？', '提示', { type: 'warning' })
  await deleteMenu(row.menuId)
  ElMessage.success('删除成功')
  loadData()
}

const resetForm = () => {
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (isEdit.value) {
    await updateMenu(form)
  } else {
    await addMenu(form)
  }
  ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
  dialogVisible.value = false
  loadData()
}

onMounted(() => loadData())
</script>
