<template>
  <view class="page">
    <!-- 用户信息头部 -->
    <view class="header">
      <image v-if="user.avatar" class="avatar-img" :src="user.avatar" mode="aspectFill" />
      <view v-else class="avatar-fallback">{{ initial }}</view>
      <text class="name">{{ user.realName || user.username || '未登录' }}</text>
      <text class="sub" v-if="user.mobile">{{ user.mobile }}</text>
      <text class="sub" v-if="user.username && user.realName">账号: {{ user.username }}</text>
    </view>

    <!-- 功能菜单 -->
    <view class="menu">
      <view class="mi" @click="go('/pages/persons/persons')">
        <text>👥 居民管理</text>
        <text class="ar">›</text>
      </view>
      <view class="mi" @click="go('/pages/vehicles/vehicles')">
        <text>🚗 车辆管理</text>
        <text class="ar">›</text>
      </view>
      <view class="mi" @click="go('/pages/records/records')">
        <text>📋 出入记录</text>
        <text class="ar">›</text>
      </view>
      <view class="mi" @click="go('/pages/visitors/visitors')">
        <text>🚶 访客管理</text>
        <text class="ar">›</text>
      </view>
      <view class="mi" @click="go('/pages/bills/bills')">
        <text>💰 物业缴费</text>
        <text class="ar">›</text>
      </view>
      <view class="mi" @click="go('/pages/announcements/announcements')">
        <text>📢 小区公告</text>
        <text class="ar">›</text>
      </view>
      <view class="mi" @click="go('/pages/face/face')">
        <text>📷 人脸门禁</text>
        <text class="ar">›</text>
      </view>
      <view class="mi" @click="go('/pages/scan/scan')">
        <text>🔍 扫码查询</text>
        <text class="ar">›</text>
      </view>
    </view>

    <!-- 修改密码 -->
    <view class="section">
      <view class="section-btn" @click="openPwdModal">
        <text>🔒 修改密码</text>
        <text class="ar">›</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout" @click="doLogout">
      <text>退出登录</text>
    </view>

    <view style="height:120rpx"></view>
    <TabBar current="/pages/mine/mine" />

    <!-- 修改密码弹窗 -->
    <view v-if="pwdVisible" class="modal-mask" @click="closePwdModal">
      <view class="modal-box" @click.stop>
        <text class="modal-title">修改密码</text>

        <view class="form-item">
          <text class="label">旧密码</text>
          <input
            class="input"
            type="password"
            v-model="pwdForm.oldPassword"
            placeholder="请输入旧密码"
            maxlength="20"
          />
        </view>

        <view class="form-item">
          <text class="label">新密码</text>
          <input
            class="input"
            type="password"
            v-model="pwdForm.newPassword"
            placeholder="请输入新密码"
            maxlength="20"
          />
        </view>

        <view class="form-item">
          <text class="label">确认密码</text>
          <input
            class="input"
            type="password"
            v-model="pwdForm.confirmPassword"
            placeholder="请再次输入新密码"
            maxlength="20"
          />
        </view>

        <view class="modal-btns">
          <button class="btn-cancel" @click="closePwdModal">取消</button>
          <button class="btn-submit" @click="submitChangePwd" :disabled="pwdSubmitting">
            {{ pwdSubmitting ? '提交中...' : '确认修改' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import TabBar from '../../components/TabBar.vue'
import { changePassword, getUserInfo } from '../../utils/request'

const user = ref({})

/** 加载本地缓存的用户信息，可选用户信息接口刷新 */
const loadUser = async () => {
  // 1. 从 localStorage 读取
  try {
    const raw = localStorage.getItem('userInfo')
    if (raw) {
      user.value = JSON.parse(raw)
    }
  } catch {
    user.value = {}
  }

  // 2. 可选：从服务端拉取最新用户信息
  try {
    const res = await getUserInfo()
    if (res && res.data) {
      user.value = res.data
      localStorage.setItem('userInfo', JSON.stringify(res.data))
    }
  } catch {
    // 静默失败，使用本地缓存
  }
}

onShow(() => {
  loadUser()
})

/** 头像首字母 */
const initial = computed(() =>
  (user.value.realName || user.value.username || 'U').substring(0, 2).toUpperCase()
)

/** 页面跳转 */
const go = (url) => {
  uni.navigateTo({ url })
}

// ---------- 退出登录 ----------
const doLogout = () => {
  uni.showModal({
    title: '提示',
    content: '确定退出登录？',
    success: (res) => {
      if (res.confirm) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        setTimeout(() => {
          uni.redirectTo({ url: '/pages/login/login' })
        }, 100)
      }
    }
  })
}

// ---------- 修改密码 ----------
const pwdVisible = ref(false)
const pwdSubmitting = ref(false)
const pwdForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const openPwdModal = () => {
  pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  pwdVisible.value = true
}

const closePwdModal = () => {
  pwdVisible.value = false
}

const submitChangePwd = async () => {
  const { oldPassword, newPassword, confirmPassword } = pwdForm.value

  if (!oldPassword) {
    uni.showToast({ title: '请输入旧密码', icon: 'none' })
    return
  }
  if (!newPassword) {
    uni.showToast({ title: '请输入新密码', icon: 'none' })
    return
  }
  if (newPassword.length < 6) {
    uni.showToast({ title: '新密码不能少于6位', icon: 'none' })
    return
  }
  if (newPassword !== confirmPassword) {
    uni.showToast({ title: '两次密码输入不一致', icon: 'none' })
    return
  }

  pwdSubmitting.value = true
  try {
    await changePassword({ oldPassword, newPassword })
    uni.showToast({ title: '密码修改成功', icon: 'success' })
    closePwdModal()
  } catch (e) {
    uni.showToast({ title: e.message || '修改失败', icon: 'none' })
  } finally {
    pwdSubmitting.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f7fa;
}

/* ---- 头部 ---- */
.header {
  background: linear-gradient(135deg, #409EFF, #2060c0);
  padding: 60rpx 30rpx 50rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255,255,255,0.3);
}

.avatar-fallback {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: rgba(255,255,255,0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
  color: #fff;
  font-weight: bold;
  border: 4rpx solid rgba(255,255,255,0.3);
}

.name {
  font-size: 36rpx;
  color: #fff;
  font-weight: bold;
  margin-top: 18rpx;
}

.sub {
  font-size: 26rpx;
  color: rgba(255,255,255,0.8);
  margin-top: 6rpx;
}

/* ---- 菜单 ---- */
.menu {
  background: #fff;
  margin: 20rpx;
  border-radius: 12rpx;
  overflow: hidden;
}

.mi {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 30rpx;
  border-bottom: 1px solid #f5f5f5;
  font-size: 28rpx;
  color: #303133;
}

.mi:last-child {
  border-bottom: none;
}

.ar {
  font-size: 32rpx;
  color: #ccc;
}

/* ---- 修改密码入口 ---- */
.section {
  background: #fff;
  margin: 20rpx;
  border-radius: 12rpx;
  overflow: hidden;
}

.section-btn {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 30rpx;
  font-size: 28rpx;
  color: #303133;
}

/* ---- 退出登录 ---- */
.logout {
  margin: 40rpx 20rpx;
  background: #fff;
  border-radius: 12rpx;
  padding: 26rpx;
  text-align: center;
  color: #F56C6C;
  font-size: 30rpx;
}

/* ---- 修改密码弹窗 ---- */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-box {
  width: 600rpx;
  background: #fff;
  border-radius: 16rpx;
  padding: 40rpx 36rpx 30rpx;
}

.modal-title {
  font-size: 34rpx;
  font-weight: bold;
  color: #303133;
  text-align: center;
  display: block;
  margin-bottom: 30rpx;
}

.form-item {
  margin-bottom: 24rpx;
}

.label {
  font-size: 26rpx;
  color: #606266;
  margin-bottom: 10rpx;
  display: block;
}

.input {
  width: 100%;
  height: 76rpx;
  border: 1px solid #e4e7ed;
  border-radius: 8rpx;
  padding: 0 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
  color: #303133;
  background: #fafafa;
}

.modal-btns {
  display: flex;
  justify-content: space-between;
  margin-top: 36rpx;
  gap: 24rpx;
}

.btn-cancel {
  flex: 1;
  height: 76rpx;
  line-height: 76rpx;
  border-radius: 8rpx;
  background: #f5f5f5;
  color: #606266;
  font-size: 28rpx;
  border: none;
  padding: 0;
  text-align: center;
}

.btn-submit {
  flex: 1;
  height: 76rpx;
  line-height: 76rpx;
  border-radius: 8rpx;
  background: #409EFF;
  color: #fff;
  font-size: 28rpx;
  border: none;
  padding: 0;
  text-align: center;
}

.btn-submit[disabled] {
  opacity: 0.6;
}
</style>
