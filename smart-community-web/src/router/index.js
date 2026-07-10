import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { isDynamicRoutesAdded, setDynamicRoutesAdded } from '@/utils/dynamicRoutes'

// 静态路由（所有用户都有）
const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    name: 'Main',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes
})

// 组件映射：menu.component → 动态 import
const componentMap = {
  'system/UserManage': () => import('@/views/system/UserManage.vue'),
  'system/RoleManage': () => import('@/views/system/RoleManage.vue'),
  'system/MenuManage': () => import('@/views/system/MenuManage.vue'),
  'system/LogManage': () => import('@/views/system/LogManage.vue'),
  'property/CommunityManage': () => import('@/views/property/CommunityManage.vue'),
  'property/PersonManage': () => import('@/views/property/PersonManage.vue'),
  'property/CameraManage': () => import('@/views/property/CameraManage.vue'),
  'property/MapManage': () => import('@/views/property/MapManage.vue'),
  'property/VehicleManage': () => import('@/views/property/VehicleManage.vue'),
  'property/AnnouncementManage': () => import('@/views/property/AnnouncementManage.vue'),
  'property/FinancialManage': () => import('@/views/property/FinancialManage.vue'),
  'property/FeeStandardManage': () => import('@/views/property/FeeStandardManage.vue'),
  'property/FinanceFlowManage': () => import('@/views/property/FinanceFlowManage.vue'),
  'property/FinanceReportManage': () => import('@/views/property/FinanceReportManage.vue'),
  'access/RecordManage': () => import('@/views/access/RecordManage.vue'),
  'access/VisitorManage': () => import('@/views/access/VisitorManage.vue'),
  'access/FaceManage': () => import('@/views/access/FaceManage.vue')
}

/**
 * 将菜单树扁平化，提取 type=2 的菜单项生成路由
 */
function generateRoutes(menuTree) {
  const routes = []
  function walk(nodes) {
    if (!nodes) return
    nodes.forEach(node => {
      if (node.type === 2 && node.path && node.component && componentMap[node.component]) {
        routes.push({
          path: node.path.startsWith('/') ? node.path.substring(1) : node.path,
          name: node.menuName,
          component: componentMap[node.component],
          meta: { title: node.menuName, icon: node.icon, permission: node.permission }
        })
      }
      if (node.children) {
        walk(node.children)
      }
    })
  }
  walk(menuTree)
  return routes
}

/**
 * 动态添加路由
 */
export async function addDynamicRoutes() {
  if (isDynamicRoutesAdded()) return

  const userStore = useUserStore()
  const appStore = useAppStore()

  try {
    await userStore.fetchMenus()
    const menuTree = appStore.menuTree
    if (!menuTree || !menuTree.length) return

    const routes = generateRoutes(menuTree)
    routes.forEach(route => {
      router.addRoute('Main', route)
    })
    setDynamicRoutesAdded(true)
  } catch {
    // 菜单获取失败
  }
}

// 路由守卫
router.beforeEach(async (to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 智慧小区管理系统` : '智慧小区管理系统'

  if (to.path === '/login') {
    next()
    return
  }

  const token = getToken()
  if (!token) {
    next('/login')
    return
  }

  // 如果动态路由未添加，先获取菜单再放行
  if (!isDynamicRoutesAdded()) {
    try {
      await addDynamicRoutes()
      next({ ...to, replace: true })
      return
    } catch {
      next('/login')
      return
    }
  }

  next()
})

export default router
