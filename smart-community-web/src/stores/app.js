import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapse: false,
    menuTree: [] // 用户菜单树
  }),
  actions: {
    toggleSidebar() {
      this.sidebarCollapse = !this.sidebarCollapse
    },
    setMenuTree(tree) {
      this.menuTree = tree
    }
  }
})
