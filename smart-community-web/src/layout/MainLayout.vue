<template>
  <el-container class="main-layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <span v-if="!isCollapse" style="font-size:18px;font-weight:bold;">智慧小区</span>
        <el-icon v-else :size="28"><HomeFilled /></el-icon>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>

        <SidebarMenu :menu-list="appStore.menuTree" />
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleSidebar" :size="22">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="signedAvatar" />
              <span class="username">{{ userStore.realName || userStore.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>

    <!-- 个人信息对话框 -->
    <el-dialog v-model="profileVisible" title="个人信息" width="420px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ userStore.username }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ userStore.realName }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ userStore.mobile || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ userStore.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ userStore.roles?.join(', ') || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="420px" @close="resetPwdForm">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangePwd">确定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref, reactive, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { ElMessage } from 'element-plus'
import { updatePassword } from '@/api/system'
import { getSignedUrl } from '@/utils/oss'
import SidebarMenu from '@/components/SidebarMenu.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

const isCollapse = computed(() => appStore.sidebarCollapse)
const activeMenu = computed(() => route.path)
const profileVisible = ref(false)
const pwdVisible = ref(false)
const pwdFormRef = ref(null)

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 头像签名 URL
const signedAvatar = ref('')

watch(() => userStore.avatar, async (url) => {
  if (url) {
    signedAvatar.value = await getSignedUrl(url)
  } else {
    signedAvatar.value = ''
  }
}, { immediate: true })

const validateConfirmPwd = (rule, value, callback) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' }
  ]
}

onMounted(async () => {
  if (!userStore.realName) {
    try { await userStore.getInfo() } catch {}
  }
  if (!appStore.menuTree.length) {
    try { await userStore.fetchMenus() } catch {}
  }
})

const toggleSidebar = () => { appStore.toggleSidebar() }

const handleCommand = async (command) => {
  if (command === 'logout') {
    await userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    profileVisible.value = true
  } else if (command === 'password') {
    pwdVisible.value = true
  }
}

const resetPwdForm = () => {
  pwdFormRef.value?.resetFields()
}

const handleChangePwd = async () => {
  console.log('[修改密码] 点击确定按钮')
  if (!pwdFormRef.value) {
    console.log('[修改密码] pwdFormRef 为空')
    return ElMessage.error('请先打开表单')
  }

  try {
    await pwdFormRef.value.validate()
  } catch {
    console.log('[修改密码] 表单校验未通过')
    return
  }

  if (!userStore.userId) {
    console.log('[修改密码] userId 为空:', userStore.userId)
    ElMessage.error('用户信息异常，请重新登录')
    return
  }

  console.log('[修改密码] 开始调用接口, userId:', userStore.userId)
  try {
    await updatePassword({
      userId: String(userStore.userId),
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    console.log('[修改密码] 接口调用成功')
    ElMessage.success('密码修改成功，请重新登录')
    pwdVisible.value = false
    await userStore.logout()
    router.push('/login')
  } catch (e) {
    console.error('[修改密码] 接口调用失败:', e)
    const msg = e?.response?.data?.message || e?.message || '密码修改失败'
    if (msg !== 'Error') {
      ElMessage.error(msg)
    }
  }
}
</script>

<style lang="scss" scoped>
.main-layout {
  height: 100vh;

  .sidebar {
    background-color: #304156;
    overflow-y: auto;
    overflow-x: hidden;

    .logo {
      height: 60px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 16px;
      font-weight: bold;
      gap: 8px;
      border-bottom: 1px solid rgba(255,255,255,0.1);
    }
  }

  .header {
    background: #fff;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid #e6e6e6;
    padding: 0 20px;
    height: 60px;

    .header-left {
      .collapse-btn {
        cursor: pointer;
        &:hover { color: #409EFF; }
      }
    }

    .header-right {
      .user-info {
        display: flex;
        align-items: center;
        gap: 8px;
        cursor: pointer;
        .username { font-size: 14px; color: #333; }
      }
    }
  }

  .main-content {
    background: #f5f7fa;
    overflow-y: auto;
  }
}

.el-menu {
  border-right: none;
}
</style>
