// 动态路由状态管理（避免循环依赖）

let dynamicRoutesAdded = false

export function isDynamicRoutesAdded() {
  return dynamicRoutesAdded
}

export function setDynamicRoutesAdded(value) {
  dynamicRoutesAdded = value
}
