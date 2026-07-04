// API请求封装
const BASE_URL = 'http://localhost:8080/api'

const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        if (res.data.code === 200) {
          resolve(res.data)
        } else if (res.data.code === 401) {
          uni.removeStorageSync('token')
          uni.reLaunch({ url: '/pages/login/login' })
          reject(res.data)
        } else {
          uni.showToast({ title: res.data.message || '请求失败', icon: 'none' })
          reject(res.data)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

// API方法
export const login = (data) => request({ url: '/auth/login', method: 'POST', data })

export const getUserInfo = () => request({ url: '/auth/user-info' })

export const getStatistics = () => request({ url: '/statistics/dashboard' })

export const faceVerify = (data) => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.uploadFile({
      url: BASE_URL + '/access/face/verify',
      filePath: data.imagePath,
      name: 'file',
      header: { 'Authorization': token ? `Bearer ${token}` : '' },
      success: (res) => resolve(JSON.parse(res.data)),
      fail: reject
    })
  })
}

export const scanPerson = (personId) => request({ url: `/property/person/${personId}` })

export const getCommunities = () => request({ url: '/property/community/all' })

export const getRecords = (data) => request({ url: '/access/record/list', method: 'GET', data })
