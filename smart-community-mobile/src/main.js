import App from './App.vue'
import { createSSRApp } from 'vue'

// 拦截 console.error 过滤 Vue 内部无害错误（uni-app 插件会覆盖 errorHandler）
const _origConsoleError = console.error
console.error = function (...args) {
  const msg = args[0] && (args[0].message || String(args[0]))
  if (msg && (
    msg.includes("Cannot assign to read only property '_'") ||
    msg.includes("Cannot read properties of null (reading 'type')") ||
    msg.includes("Cannot read properties of null (reading 'subTree')") ||
    msg.includes("Cannot read properties of null (reading 'parentNode')") ||
    msg.includes("Cannot read properties of null (reading 'emitsOptions')")
  )) {
    return
  }
  return _origConsoleError.apply(this, args)
}

export function createApp() {
  const app = createSSRApp(App)
  return { app }
}
