// API 请求封装 - 使用 fetch 避免 uni.request 只读对象 bug
export const BASE = '/api'

export function getToken() {
  try { return localStorage.getItem('token') || '' } catch { return '' }
}

/** 检查 JWT token 是否已过期，避免无效请求被页面重定向中止 */
function isTokenExpired() {
  const token = getToken()
  if (!token) return true
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return true
    const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')))
    if (!payload.exp) return true
    return payload.exp * 1000 < Date.now()
  } catch {
    return true
  }
}

const PUBLIC_URLS = ['/auth/captcha', '/auth/login']

async function request(url, options = {}) {
  const currentToken = getToken()
  const isPublic = PUBLIC_URLS.some(p => url.startsWith(p))
  if (!isPublic && isTokenExpired()) {
    throw new Error('登录已过期，请重新登录')
  }

  const method = options.method || 'GET'
  const headers = {}
  if (currentToken) {
    headers['Authorization'] = `Bearer ${currentToken}`
  }

  let body = null
  if (options.data && method !== 'GET') {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(options.data)
  }

  let fullUrl = BASE + url
  if (options.params) {
    const clean = {}
    for (const [k, v] of Object.entries(options.params)) {
      if (v !== undefined && v !== null && v !== '') clean[k] = v
    }
    const qs = new URLSearchParams(clean).toString()
    if (qs) fullUrl += '?' + qs
  }

  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const controller = new AbortController()
      const timeoutId = setTimeout(() => controller.abort(), 15000)
      
      const res = await fetch(fullUrl, { method, headers, body, signal: controller.signal })
      clearTimeout(timeoutId)
      
      const raw = await res.json()
      const data = JSON.parse(JSON.stringify(raw))

      if (data.code === 200) return data
      if (data.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        throw new Error(data.message || '未登录')
      }
      throw new Error(data.message || '请求失败')
    } catch (e) {
      const errorMsg = String(e.message || '')
      const errorName = String(e.name || '')
      if (errorName === 'AbortError' || errorMsg.includes('ERR_ABORTED')) {
        console.debug('请求已中止:', fullUrl)
        return { code: -1, message: '请求已中止' }
      }
      if (
        e.message === 'Failed to fetch' ||
        errorMsg.includes('NetworkError') ||
        errorMsg.includes('ERR_CONNECTION_REFUSED')
      ) {
        console.warn(`request: attempt ${attempt + 1} failed for ${fullUrl} (${errorName}: ${errorMsg}), retrying...`)
        await new Promise(r => setTimeout(r, 500))
        continue
      }
      throw e
    }
  }
  console.warn('request: all retries exhausted for', fullUrl)
  throw new Error('网络连接失败，请检查后端服务是否运行')
}

// Auth
export const getCaptcha = () => request('/auth/captcha')
export const login = (data) => request('/auth/login', { method: 'POST', data })
export const getUserInfo = () => request('/auth/user-info')

// Statistics
export const getStatistics = () => request('/statistics/dashboard')

// Records
export const getRecords = (params) => request('/access/record/list', { params })

// Visitors
export const getVisitors = (params) => request('/access/visitor/list', { params })

// Communities
export const getAllCommunities = () => request('/property/community/all')
export { isTokenExpired }

// Persons
export const getPersons = (params) => request('/property/person/list', { params })
export const getPerson = (id) => request(`/property/person/${id}`)
export const addPerson = (data) => request('/property/person', { method: 'POST', data })
export const updatePerson = (data) => request('/property/person', { method: 'PUT', data })
export const deletePerson = (id) => request(`/property/person/${id}`, { method: 'DELETE' })
export const uploadPersonFace = (id, file) => {
  return new Promise((resolve, reject) => {
    const formData = new FormData()
    formData.append('file', file)
    const headers = {}
    const token = getToken()
    if (token) headers['Authorization'] = `Bearer ${token}`
    fetch(BASE + `/property/person/${id}/face`, {
      method: 'POST',
      headers,
      body: formData
    }).then(r => r.json()).then(d => {
      if (d.code === 200) resolve(d)
      else reject(new Error(d.message))
    }).catch(reject)
  })
}

// OSS signed URL
export const getSignedUrl = (url, expire) => request('/common/signed-url', { params: { url, expire } })

// File upload - supports both uni-app native and H5 (data URL)
export const uploadFile = (filePath, dir = 'face') => {
  const isDataUrl = typeof filePath === 'string' && filePath.startsWith('data:image')
  
  if (isDataUrl) {
    return uploadDataUrl(filePath, dir)
  }
  
  return new Promise((resolve, reject) => {
    const token = getToken()
    const header = {}
    if (token) header['Authorization'] = `Bearer ${token}`
    uni.uploadFile({
      url: BASE + '/common/upload',
      filePath: filePath,
      name: 'file',
      header: header,
      formData: { dir, bucket: dir },
      success: (res) => {
        try {
          const d = JSON.parse(res.data)
          if (d.code === 200) resolve(d)
          else reject(new Error(d.message))
        } catch (e) {
          reject(new Error('解析上传结果失败'))
        }
      },
      fail: (err) => {
        reject(new Error(err.errMsg || '文件上传失败'))
      }
    })
  })
}

// Upload data URL in H5 mode
const uploadDataUrl = (dataUrl, dir = 'face') => {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const header = {}
    if (token) header['Authorization'] = `Bearer ${token}`
    
    const arr = dataUrl.split(',')
    const mime = arr[0].match(/:(.*?);/)[1]
    const bstr = atob(arr[1])
    let n = bstr.length
    const u8arr = new Uint8Array(n)
    while (n--) {
      u8arr[n] = bstr.charCodeAt(n)
    }
    
    const blob = new Blob([u8arr], { type: mime })
    const formData = new FormData()
    formData.append('file', blob, `face_${Date.now()}.jpg`)
    formData.append('dir', dir)
    formData.append('bucket', dir)
    
    const xhr = new XMLHttpRequest()
    xhr.open('POST', BASE + '/common/upload')
    for (const key in header) {
      xhr.setRequestHeader(key, header[key])
    }
    xhr.onload = () => {
      if (xhr.status === 200) {
        try {
          const d = JSON.parse(xhr.responseText)
          if (d.code === 200) resolve(d)
          else reject(new Error(d.message))
        } catch (e) {
          reject(new Error('解析上传结果失败'))
        }
      } else {
        reject(new Error('上传失败，HTTP状态码: ' + xhr.status))
      }
    }
    xhr.onerror = () => {
      reject(new Error('网络上传失败'))
    }
    xhr.send(formData)
  })
}

// Announcements
export const getAnnouncements = (params) => request('/announcement/page', { params })
export const getAnnouncement = (id) => request(`/announcement/${id}`)
export const getHomeAnnouncements = () => request('/announcement/home')

// Visitors - CRUD + actions
export const getVisitor = (id) => request(`/access/visitor/${id}`)
export const addVisitor = (data) => request('/access/visitor', { method: 'POST', data })
export const updateVisitor = (data) => request('/access/visitor', { method: 'PUT', data })
export const cancelVisitor = (id) => request(`/access/visitor/${id}/cancel`, { method: 'PUT' })
export const checkInVisitor = (id) => request(`/access/visitor/${id}/check-in`, { method: 'PUT' })
export const checkOutVisitor = (id) => request(`/access/visitor/${id}/check-out`, { method: 'PUT' })

// Vehicles - CRUD
export const getVehicles = (params) => request('/property/vehicle/list', { params })
export const getVehicle = (id) => request(`/property/vehicle/${id}`)
export const addVehicle = (data) => request('/property/vehicle', { method: 'POST', data })
export const updateVehicle = (data) => request('/property/vehicle', { method: 'PUT', data })
export const deleteVehicle = (id) => request(`/property/vehicle/${id}`, { method: 'DELETE' })

// Cameras
export const getCameras = (params) => request('/property/camera/list', { params })
export const getCamera = (id) => request(`/property/camera/${id}`)

// Bills & Financial
export const getBillList = (params) => request('/financial/bill/list', { params })
export const getBill = (id) => request(`/financial/bill/${id}`)
export const getBillHistory = (personId) => request(`/financial/bill/history/${personId}`)
export const payBill = (billId, amount, payMethod) =>
  request(`/financial/bill/${billId}/pay`, { method: 'POST', params: { amount, payMethod } })
export const getFinancialReport = () => request('/financial/report')

// Fee Standards
export const getEnabledFeeStandards = () => request('/financial/fee-standard/enabled')

// Payment Records
export const getPaymentRecords = (params) => request('/financial/payment-record/list', { params })

// Face Recognition
export const faceSearch = (imageUrl) =>
  request('/face/search', { method: 'POST', params: { imageUrl } })
export const faceCompare = (imageUrl1, imageUrl2) =>
  request('/face/compare', { method: 'POST', params: { imageUrl1, imageUrl2 } })
export const faceRecord = (params) =>
  request('/face/record', { method: 'POST', params })
export const faceVerify = (file) => {
  return new Promise((resolve, reject) => {
    const formData = new FormData()
    formData.append('file', file)
    const headers = {}
    const token = getToken()
    if (token) headers['Authorization'] = `Bearer ${token}`
    fetch(BASE + '/face/search', {
      method: 'POST',
      headers,
      body: formData
    }).then(r => r.json()).then(d => {
      if (d.code === 200) resolve(d)
      else reject(new Error(d.message))
    }).catch(reject)
  })
}

// Person Excel Import/Export - uni-app version
export const importPersons = (filePath) => {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const header = {}
    if (token) header['Authorization'] = `Bearer ${token}`
    uni.uploadFile({
      url: BASE + '/property/person/import',
      filePath: filePath,
      name: 'file',
      header: header,
      success: (res) => {
        try {
          const d = JSON.parse(res.data)
          if (d.code === 200) resolve(d)
          else reject(new Error(d.message))
        } catch (e) {
          reject(new Error('解析导入结果失败'))
        }
      },
      fail: (err) => {
        reject(new Error(err.errMsg || '导入失败'))
      }
    })
  })
}

export const exportPersons = (params) => {
  return new Promise((resolve, reject) => {
    const token = getToken()
    let url = BASE + '/property/person/export'
    if (params) {
      const clean = {}
      for (const [k, v] of Object.entries(params)) {
        if (v !== undefined && v !== null && v !== '') clean[k] = v
      }
      const qs = new URLSearchParams(clean).toString()
      if (qs) url += '?' + qs
    }
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`
    fetch(url, {
      method: 'GET',
      headers,
      responseType: 'blob'
    }).then(r => r.blob()).then(blob => {
      resolve(blob)
    }).catch(reject)
  })
}

// Plate Recognition
export const plateRecognize = (imageUrl) =>
  request('/plate/recognize', { method: 'POST', params: { imageUrl } })
export const plateRecord = (params) =>
  request('/plate/record', { method: 'POST', params })

// User Profile
export const changePassword = (data) => request('/auth/change-password', { method: 'PUT', data })
export const updateProfile = (data) => request('/auth/profile', { method: 'PUT', data })
