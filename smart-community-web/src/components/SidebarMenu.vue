<template>
  <template v-for="item in menuList" :key="item.menuId">
    <!-- 目录：有子菜单 -->
    <el-sub-menu v-if="item.type === 1 && item.children?.length" :index="item.path">
      <template #title>
        <el-icon v-if="item.icon && iconMap[item.icon]">
          <component :is="iconMap[item.icon]" />
        </el-icon>
        <span>{{ item.menuName }}</span>
      </template>
      <SidebarMenu :menu-list="item.children" />
    </el-sub-menu>

    <!-- 菜单项：无子菜单 -->
    <el-menu-item v-else-if="item.type === 2" :index="item.path">
      <el-icon v-if="item.icon && iconMap[item.icon]">
        <component :is="iconMap[item.icon]" />
      </el-icon>
      <span>{{ item.menuName }}</span>
    </el-menu-item>
  </template>
</template>

<script setup>
import { markRaw } from 'vue'
import {
  Setting, OfficeBuilding, Key, HomeFilled, User, Avatar, Menu,
  Document, VideoCamera, List, UserFilled, View, DataAnalysis, MapLocation, Bicycle, Bell,
  Money, DataLine
} from '@element-plus/icons-vue'

defineProps({
  menuList: { type: Array, default: () => [] }
})

// 字符串图标名 → 组件映射
const iconMap = {
  Setting: markRaw(Setting),
  OfficeBuilding: markRaw(OfficeBuilding),
  Key: markRaw(Key),
  HomeFilled: markRaw(HomeFilled),
  User: markRaw(User),
  Avatar: markRaw(Avatar),
  Menu: markRaw(Menu),
  Document: markRaw(Document),
  VideoCamera: markRaw(VideoCamera),
  List: markRaw(List),
  UserFilled: markRaw(UserFilled),
  View: markRaw(View),
  DataAnalysis: markRaw(DataAnalysis),
  MapLocation: markRaw(MapLocation),
  Bicycle: markRaw(Bicycle),
  Bell: markRaw(Bell),
  Money: markRaw(Money),
  DataLine: markRaw(DataLine)
}
</script>
