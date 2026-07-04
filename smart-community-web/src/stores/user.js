import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getUserInfo, getUserMenus } from '@/api/auth'
import { setToken, removeToken, getToken } from '@/utils/auth'
import { useAppStore } from '@/stores/app'
import { setDynamicRoutesAdded } from '@/utils/dynamicRoutes'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userId: '',
    username: '',
    realName: '',
    avatar: '',
    mobile: '',
    email: '',
    roles: [],
    permissions: []
  }),
  actions: {
    async login(loginForm) {
      const { data } = await loginApi(loginForm)
      this.token = data.token
      this.userId = data.userId
      this.username = data.username
      this.realName = data.realName
      this.avatar = data.avatar
      this.roles = data.roles
      this.permissions = data.permissions
      setToken(data.token)
      // 登录后预加载菜单到 store（动态路由由 router 守卫负责添加）
      await this.fetchMenus()
    },
    async getInfo() {
      const { data } = await getUserInfo()
      this.userId = data.userId
      this.username = data.username
      this.realName = data.realName
      this.avatar = data.avatar
      this.mobile = data.mobile || ''
      this.email = data.email || ''
      this.roles = data.roles
      this.permissions = data.permissions
    },
    async fetchMenus() {
      try {
        const { data } = await getUserMenus()
        const appStore = useAppStore()
        appStore.setMenuTree(data || [])
      } catch {
        // 菜单获取失败不影响登录
      }
    },
    async logout() {
      await logoutApi()
      this.resetState()
      removeToken()
    },
    resetState() {
      this.token = ''
      this.userId = ''
      this.username = ''
      this.realName = ''
      this.avatar = ''
      this.roles = []
      this.permissions = []
      const appStore = useAppStore()
      appStore.setMenuTree([])
      setDynamicRoutesAdded(false)
    }
  }
})
