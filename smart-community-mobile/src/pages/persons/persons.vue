<template>
  <view class="page">
    <view class="header-bar">
      <text class="title">居民管理</text>
      <view class="header-actions">
        <view class="action-btn" @click="handleExportAll"><text>📥</text></view>
        <view class="action-btn" @click="handleImport"><text>📤</text></view>
        <view class="action-btn primary" @click="openAdd"><text>＋</text></view>
      </view>
    </view>

    <view class="search-bar">
      <view class="search-input-wrap">
        <text class="search-icon">🔍</text>
        <input class="search-input" v-model="keyword" placeholder="搜索姓名/手机号/门牌号" @confirm="onSearch" @input="onSearchInput" />
        <text v-if="keyword" class="search-clear" @click="clearSearch">✕</text>
      </view>
    </view>

    <view class="filter-row">
      <view class="filter-picker">
        <view class="picker-box" @click="showFilterCommunity = !showFilterCommunity">
          <text>{{ filterCommunityName || '全部小区' }}</text>
          <text class="picker-arrow">▼</text>
        </view>
        <view class="picker-drop" v-if="showFilterCommunity">
          <view :class="['picker-item', { active: !filterCommunityId }]"
            @click="pickFilterCommunity(null, '全部小区')">全部小区</view>
          <view v-for="c in communities" :key="c.communityId"
            :class="['picker-item', { active: filterCommunityId === c.communityId }]"
            @click="pickFilterCommunity(c.communityId, c.name)">{{ c.name }}</view>
        </view>
      </view>
      <view class="type-chips">
        <view :class="['chip', { active: filterPersonType === null }]" @click="setPersonTypeFilter(null)">全部</view>
        <view :class="['chip', { active: filterPersonType === 1 }]" @click="setPersonTypeFilter(1)">业主</view>
        <view :class="['chip', { active: filterPersonType === 2 }]" @click="setPersonTypeFilter(2)">租户</view>
        <view :class="['chip', { active: filterPersonType === 3 }]" @click="setPersonTypeFilter(3)">家属</view>
      </view>
    </view>

    <view class="list">
      <view class="card" v-for="p in list" :key="p.personId" @click="openEdit(p)">
        <view class="avatar-wrap">
          <image v-if="p.faceUrl" :src="p._signedFaceUrl || p.faceUrl" class="avatar-img" mode="aspectFill" />
          <view v-else class="avatar-placeholder"><text>{{ (p.userName || '?')[0] }}</text></view>
        </view>
        <view class="info">
          <view class="info-top">
            <text class="name">{{ p.userName }}</text>
            <text :class="['type-tag', p.personType === 1 ? 'tag-owner' : p.personType === 2 ? 'tag-tenant' : 'tag-family']">
              {{ p.personType === 1 ? '业主' : p.personType === 2 ? '租户' : '家属' }}
            </text>
          </view>
          <text class="sub">📱 {{ p.mobile || '-' }}</text>
          <text class="sub">🏠 {{ p.communityName || '-' }} · {{ p.houseNo || '-' }}</text>
          <view class="info-bottom">
            <text :class="['state-text', p.state === 1 ? 'state-active' : 'state-inactive']">
              {{ p.state === 1 ? '在住' : '迁出' }}
            </text>
            <text class="create-time">{{ p.createTime || '' }}</text>
          </view>
        </view>
        <view class="card-actions">
          <view class="act-btn edit" @click.stop="openEdit(p)"><text>✎</text></view>
          <view class="act-btn del" @click.stop="confirmDelete(p)"><text>✕</text></view>
        </view>
      </view>
      <view v-if="!list.length" class="empty"><text>暂无居民数据</text></view>
      <view v-if="list.length" class="load-more" @click="loadMore">点击加载更多 (当前{{ list.length }}条/共{{ total }}条)</view>
    </view>

    <view class="modal-mask" v-if="formVisible" @click="formVisible = false" />
    <view class="modal" v-if="formVisible">
      <view class="modal-header">
        <text class="modal-title">{{ isEdit ? '编辑' : '新增' }}居民</text>
        <text class="modal-close" @click="formVisible = false">✕</text>
      </view>
      <scroll-view class="modal-body" scroll-y>
        <view class="form-group">
          <text class="form-label">所属小区 <text class="required">*</text></text>
          <view class="picker-box" @click="showCommunityList = !showCommunityList">
            <text>{{ form.communityName || '请选择小区' }}</text>
          </view>
          <view class="picker-drop" v-if="showCommunityList">
            <view v-for="c in communities" :key="c.communityId"
              :class="['picker-item', { active: form.communityId === c.communityId }]"
              @click="pickCommunity(c)">{{ c.name }}</view>
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">姓名 <text class="required">*</text></text>
          <input class="form-input" v-model="form.userName" placeholder="请输入姓名" />
        </view>
        <view class="form-group">
          <text class="form-label">手机号</text>
          <input class="form-input" v-model="form.mobile" placeholder="请输入手机号" />
        </view>
        <view class="form-row">
          <view class="form-group half">
            <text class="form-label">性别</text>
            <view class="radio-group">
              <view :class="['radio-item', { active: form.sex === 1 }]" @click="form.sex = 1"><text>男</text></view>
              <view :class="['radio-item', { active: form.sex === 2 }]" @click="form.sex = 2"><text>女</text></view>
            </view>
          </view>
          <view class="form-group half">
            <text class="form-label">类型</text>
            <view class="radio-group">
              <view :class="['radio-item', { active: form.personType === 1 }]" @click="form.personType = 1"><text>业主</text></view>
              <view :class="['radio-item', { active: form.personType === 2 }]" @click="form.personType = 2"><text>租户</text></view>
              <view :class="['radio-item', { active: form.personType === 3 }]" @click="form.personType = 3"><text>家属</text></view>
            </view>
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">门牌号</text>
          <input class="form-input" v-model="form.houseNo" placeholder="如：1栋501室" />
        </view>
        <view class="form-group">
          <text class="form-label">在住状态</text>
          <view class="radio-group">
            <view :class="['radio-item', { active: form.state === 1 }]" @click="form.state = 1"><text>在住</text></view>
            <view :class="['radio-item', { active: form.state === 0 }]" @click="form.state = 0"><text>迁出</text></view>
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">人脸照片</text>
          <view class="photo-section">
            <view class="photo-preview" v-if="form.faceUrl" @click="removeFacePhoto">
              <image :src="form._signedFaceUrl || form.faceUrl" class="photo-img" mode="aspectFill" />
              <view class="photo-remove">✕</view>
            </view>
            <view class="photo-placeholder" v-else>
              <text class="photo-placeholder-icon">📷</text>
              <text class="photo-placeholder-text">请上传人脸照片</text>
            </view>
            <view class="photo-btns">
              <view class="photo-btn" @click="chooseFromAlbum">
                <text>🖼️ 相册</text>
              </view>
              <view class="photo-btn" @click="chooseFromCamera">
                <text>📸 拍照</text>
              </view>
            </view>
          </view>
        </view>
        <view class="form-group">
          <text class="form-label">备注</text>
          <textarea class="form-textarea" v-model="form.remark" placeholder="可选备注信息" />
        </view>
      </scroll-view>
      <view class="modal-footer">
        <view class="btn-cancel" @click="formVisible = false"><text>取消</text></view>
        <view class="btn-submit" @click="doSubmit"><text>{{ isEdit ? '修改' : '新增' }}</text></view>
      </view>
    </view>

    <view class="modal-mask" v-if="loadingVisible" />
    <view class="loading-overlay" v-if="loadingVisible">
      <view class="loading-card">
        <text class="loading-spinner">⏳</text>
        <text class="loading-text">{{ loadingText }}</text>
      </view>
    </view>

  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { onShow, onHide } from '@dcloudio/uni-app'
import { getPersons, addPerson, updatePerson, deletePerson, getAllCommunities, uploadFile, isTokenExpired, faceSearch, faceCompare, importPersons, getToken, getSignedUrl } from '../../utils/request'
import { BASE } from '../../utils/request'

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const formVisible = ref(false)
const loadingVisible = ref(false)
const loadingText = ref('')
const isEdit = ref(false)
const communities = ref([])
const showCommunityList = ref(false)

const keyword = ref('')
const filterCommunityId = ref(null)
const filterCommunityName = ref('')
const filterPersonType = ref(null)
const showFilterCommunity = ref(false)
let searchTimer = null
let isLeaving = false

const form = reactive({
  personId: '', communityId: null, communityName: '',
  userName: '', mobile: '', sex: 1, houseNo: '', personType: 1, state: 1, remark: '',
  faceUrl: '', _oldFaceUrl: ''
})

const loadData = async (reset) => {
  if (isLeaving) return
  if (reset) pageNum.value = 1
  try {
    const params = { pageNum: pageNum.value, pageSize: 20 }
    if (keyword.value) params.keyword = keyword.value
    if (filterCommunityId.value) params.communityId = filterCommunityId.value
    if (filterPersonType.value) params.personType = filterPersonType.value
    const res = await getPersons(params)
    if (res.code === -1) return
    if (isLeaving) return
    const data = res.data || {}
    const records = JSON.parse(JSON.stringify(data.records || []))
    
    for (const record of records) {
      if (record.faceUrl && record.faceUrl.includes('smart-community-v3')) {
        try {
          const signedRes = await getSignedUrl(record.faceUrl, 3600)
          record._signedFaceUrl = signedRes.data
        } catch (e) {
          console.error('获取人脸照片签名URL失败:', e)
        }
      }
    }
    
    if (reset || pageNum.value === 1) {
      list.value = records
    } else {
      list.value = [...list.value, ...records]
    }
    total.value = data.total || 0
  } catch (e) {
    console.error(e)
    uni.showToast({ title: '加载居民数据失败: ' + (e.message || '网络异常'), icon: 'none' })
  }
}

const loadMore = () => {
  if (list.value.length < total.value) {
    pageNum.value++
    loadData(false)
  }
}

const loadCommunities = async () => {
  if (isLeaving) return
  try {
    const res = await getAllCommunities()
    if (res.code === -1) return
    if (isLeaving) return
    communities.value = JSON.parse(JSON.stringify(res.data || []))
  } catch (e) {
    console.error(e)
    uni.showToast({ title: '加载小区列表失败，筛选不可用', icon: 'none' })
  }
}

const onSearchInput = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    loadData(true)
  }, 400)
}

const onSearch = () => {
  if (searchTimer) clearTimeout(searchTimer)
  loadData(true)
}

const clearSearch = () => {
  keyword.value = ''
  loadData(true)
}

const pickFilterCommunity = (id, name) => {
  filterCommunityId.value = id
  filterCommunityName.value = name || ''
  showFilterCommunity.value = false
  loadData(true)
}

const setPersonTypeFilter = (type) => {
  filterPersonType.value = type
  loadData(true)
}

const resetForm = () => {
  form.personId = ''
  form.communityId = null
  form.communityName = ''
  form.userName = ''
  form.mobile = ''
  form.sex = 1
  form.houseNo = ''
  form.personType = 1
  form.state = 1
  form.remark = ''
  form.faceUrl = ''
  form._oldFaceUrl = ''
  showCommunityList.value = false
}

const openAdd = () => {
  resetForm()
  isEdit.value = false
  formVisible.value = true
}

const openEdit = (p) => {
  const raw = JSON.parse(JSON.stringify(p))
  form.personId = raw.personId
  form.communityId = raw.communityId
  form.communityName = raw.communityName || ''
  form.userName = raw.userName || ''
  form.mobile = raw.mobile || ''
  form.sex = raw.sex || 1
  form.houseNo = raw.houseNo || ''
  form.personType = raw.personType || 1
  form.state = raw.state !== undefined ? raw.state : 1
  form.remark = raw.remark || ''
  form.faceUrl = raw.faceUrl || ''
  form._oldFaceUrl = raw.faceUrl || ''
  isEdit.value = true
  formVisible.value = true
}

const pickCommunity = (c) => {
  form.communityId = c.communityId
  form.communityName = c.name
  showCommunityList.value = false
}

const uploadFaceImage = (filePath) => {
  loadingVisible.value = true
  loadingText.value = '正在上传照片...'
  uploadFile(filePath, 'face')
    .then((uploadRes) => {
      const url = uploadRes.data || ''
      if (!url) {
        uni.showToast({ title: '未获取到图片URL', icon: 'none' })
        loadingVisible.value = false
        return
      }
      loadingText.value = '正在与已有居民进行人脸查重...'
      return faceSearch(url).then(searchRes => ({ url, searchRes }))
    })
    .then(({ url, searchRes }) => {
      const searchData = searchRes.data || {}
      if (searchData.matched && (Number(searchData.score) || 0) > 90) {
        const matchedId = String(searchData.personId)
        const currentId = String(form.personId)
        if (isEdit.value && matchedId === currentId) {
          return proceedUpload(url)
        } else {
          const score = Number(searchData.score) || 0
          uni.showToast({ title: `该人脸已存在（置信度 ${score.toFixed(2)}%），请勿重复录入`, icon: 'none', duration: 3000 })
          loadingVisible.value = false
        }
      } else {
        if (isEdit.value && form._oldFaceUrl) {
          loadingText.value = '正在与原有照片进行本人确认...'
          return faceCompare(url, form._oldFaceUrl).then(compareRes => ({ url, compareRes }))
        }
        return proceedUpload(url)
      }
    })
    .then((result) => {
      if (!result) return
      if (result.compareRes) {
        const cmp = result.compareRes.data || {}
        const score = cmp ? (Number(cmp.score) || 0) : 0
        if (!cmp || !cmp.passed || score < 95) {
          uni.showToast({ title: `本人确认未通过（置信度 ${score.toFixed(2)}%，需要 > 95%），请重新上传本人照片`, icon: 'none', duration: 3000 })
          loadingVisible.value = false
          return
        }
      }
      proceedUpload(result.url)
    })
    .catch((e) => {
      loadingVisible.value = false
      uni.showToast({ title: e.message || '人脸照片处理失败', icon: 'none' })
    })
}

const proceedUpload = (url) => {
  form.faceUrl = url
  loadingVisible.value = false
  uni.showToast({ title: '人脸照片上传成功', icon: 'success' })
}

const chooseFromAlbum = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album'],
    success: (res) => {
      const filePath = res.tempFilePaths[0]
      if (filePath) uploadFaceImage(filePath)
    },
    fail: () => {
      uni.showToast({ title: '选择图片失败', icon: 'none' })
    }
  })
}

const chooseFromCamera = () => {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['camera'],
    success: (res) => {
      const filePath = res.tempFilePaths[0]
      if (filePath) uploadFaceImage(filePath)
    },
    fail: () => {
      uni.showToast({ title: '拍照失败', icon: 'none' })
    }
  })
}

const removeFacePhoto = () => {
  form.faceUrl = ''
}

const doSubmit = async () => {
  if (!form.userName.trim()) {
    uni.showToast({ title: '请输入姓名', icon: 'none' })
    return
  }
  if (!form.communityId) {
    uni.showToast({ title: '请选择所属小区', icon: 'none' })
    return
  }
  const payload = {
    communityId: form.communityId, userName: form.userName.trim(),
    mobile: form.mobile, sex: form.sex, houseNo: form.houseNo,
    personType: form.personType, remark: form.remark, state: form.state,
    faceUrl: form.faceUrl
  }
  if (isEdit.value) payload.personId = form.personId
  try {
    const res = isEdit.value ? await updatePerson(payload) : await addPerson(payload)
    if (res.code === -1) {
      console.warn('请求被中止')
      return
    }
    uni.showToast({ title: isEdit.value ? '修改成功' : '新增成功', icon: 'success' })
    formVisible.value = false
    loadData(true)
  } catch (e) {
    if (e.message === '登录已过期，请重新登录') {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
      setTimeout(() => { uni.redirectTo({ url: '/pages/login/login' }) }, 1500)
      return
    }
    uni.showToast({ title: e.message || '操作失败', icon: 'none' })
  }
}

const confirmDelete = (p) => {
  uni.showModal({
    title: '确认删除',
    content: '确定删除居民「' + p.userName + '」吗？',
    confirmText: '删除',
    confirmColor: '#F56C6C',
    success: (res) => {
      if (res.confirm) {
        deletePerson(p.personId).then((res) => {
          if (res.code === -1) {
            console.warn('请求被中止')
            return
          }
          uni.showToast({ title: '删除成功', icon: 'success' })
          loadData(true)
        }).catch((e) => {
          if (e.message === '登录已过期，请重新登录') {
            localStorage.removeItem('token')
            localStorage.removeItem('userInfo')
            uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
            setTimeout(() => { uni.redirectTo({ url: '/pages/login/login' }) }, 1500)
            return
          }
          uni.showToast({ title: e.message || '删除失败', icon: 'none' })
        })
      }
    }
  })
}

const handleImport = () => {
  uni.chooseMessageFile({
    count: 1,
    type: 'file',
    success: (res) => {
      const file = res.tempFiles[0]
      if (!file) return
      uni.showLoading({ title: '导入中...' })
      importPersons(file.path).then(() => {
        uni.hideLoading()
        uni.showToast({ title: '导入成功', icon: 'success' })
        loadData(true)
      }).catch((e) => {
        uni.hideLoading()
        uni.showToast({ title: e.message || '导入失败', icon: 'none' })
      })
    },
    fail: () => {
      uni.showToast({ title: '选择文件失败', icon: 'none' })
    }
  })
}

const handleExportAll = () => {
  const params = {}
  if (keyword.value) params.keyword = keyword.value
  if (filterCommunityId.value) params.communityId = filterCommunityId.value
  if (filterPersonType.value) params.personType = filterPersonType.value
  uni.showLoading({ title: '导出中...' })
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
  const header = {}
  if (token) header['Authorization'] = `Bearer ${token}`
  uni.downloadFile({
    url: url,
    header: header,
    success: (res) => {
      uni.hideLoading()
      if (res.statusCode === 200) {
        uni.openDocument({
          filePath: res.tempFilePath,
          fileType: 'xlsx',
          success: () => {
            uni.showToast({ title: '导出成功', icon: 'success' })
          },
          fail: () => {
            uni.showToast({ title: '打开文件失败', icon: 'none' })
          }
        })
      } else {
        uni.showToast({ title: '导出失败', icon: 'none' })
      }
    },
    fail: () => {
      uni.hideLoading()
      uni.showToast({ title: '导出失败', icon: 'none' })
    }
  })
}

onHide(() => {
  isLeaving = true
})

onShow(() => {
  if (isTokenExpired()) {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    uni.redirectTo({ url: '/pages/login/login' })
    return
  }
  isLeaving = false
  loadCommunities()
  loadData(true)
})
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; padding-bottom: 40rpx; }

.header-bar { display: flex; justify-content: space-between; align-items: center; padding: 24rpx 32rpx; background: #fff; position: sticky; top: 0; z-index: 50; }
.title { font-size: 34rpx; font-weight: 700; color: #1A1A1A; }
.header-actions { display: flex; gap: 16rpx; }
.action-btn { width: 64rpx; height: 64rpx; background: #f5f7fa; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 28rpx; }
.action-btn.primary { background: #409EFF; color: #fff; font-size: 32rpx; }

.search-bar { padding: 16rpx 24rpx; background: #fff; }
.search-input-wrap { display: flex; align-items: center; height: 72rpx; background: #f5f7fa; border-radius: 36rpx; padding: 0 24rpx; }
.search-icon { font-size: 28rpx; margin-right: 12rpx; flex-shrink: 0; }
.search-input { flex: 1; font-size: 26rpx; height: 100%; }
.search-clear { font-size: 30rpx; color: #c0c4cc; padding: 8rpx; flex-shrink: 0; }

.filter-row { display: flex; align-items: center; gap: 16rpx; padding: 12rpx 24rpx; background: #fff; border-top: 1rpx solid #f0f0f0; }
.filter-picker { position: relative; flex-shrink: 0; }
.filter-picker .picker-box { height: 60rpx; padding: 0 20rpx; background: #f5f7fa; border-radius: 10rpx; display: flex; align-items: center; gap: 8rpx; font-size: 24rpx; color: #303133; white-space: nowrap; }
.picker-arrow { font-size: 18rpx; color: #909399; }
.filter-picker .picker-drop { position: absolute; top: 68rpx; left: 0; min-width: 260rpx; background: #fff; border-radius: 10rpx; border: 1rpx solid #ebeef5; max-height: 280rpx; overflow-y: auto; z-index: 60; box-shadow: 0 4rpx 16rpx rgba(0,0,0,.08); }
.filter-picker .picker-item { padding: 22rpx 24rpx; font-size: 26rpx; color: #303133; border-bottom: 1rpx solid #f5f7fa; }
.filter-picker .picker-item:last-child { border-bottom: none; }
.filter-picker .picker-item.active { color: #409EFF; background: #ecf5ff; }

.type-chips { display: flex; gap: 12rpx; flex: 1; overflow-x: auto; }
.chip { flex-shrink: 0; height: 56rpx; padding: 0 28rpx; border-radius: 28rpx; display: flex; align-items: center; justify-content: center; font-size: 24rpx; color: #606266; background: #f5f7fa; border: 2rpx solid transparent; }
.chip.active { background: #ecf5ff; color: #409EFF; border-color: #409EFF; }

.list { padding: 20rpx 24rpx; }

.card { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 16rpx; display: flex; align-items: center; gap: 20rpx; }
.avatar-wrap { flex-shrink: 0; }
.avatar-img { width: 100rpx; height: 100rpx; border-radius: 50%; object-fit: cover; }
.avatar-placeholder { width: 100rpx; height: 100rpx; border-radius: 50%; background: linear-gradient(135deg, #409EFF, #7B9EFF); display: flex; align-items: center; justify-content: center; }
.avatar-placeholder text { font-size: 44rpx; color: #fff; font-weight: 600; }
.info { flex: 1; overflow: hidden; }
.info-top { display: flex; align-items: center; gap: 12rpx; margin-bottom: 6rpx; }
.name { font-size: 30rpx; font-weight: 600; color: #1A1A1A; display: block; }
.type-tag { font-size: 20rpx; padding: 4rpx 12rpx; border-radius: 8rpx; flex-shrink: 0; }
.tag-owner { background: #f0f9eb; color: #67c23a; }
.tag-tenant { background: #fdf6ec; color: #e6a23c; }
.tag-family { background: #ecf5ff; color: #409eff; }
.sub { font-size: 24rpx; color: #909399; display: block; margin-top: 4rpx; }
.info-bottom { display: flex; align-items: center; gap: 16rpx; margin-top: 8rpx; }
.state-text { font-size: 22rpx; padding: 4rpx 12rpx; border-radius: 8rpx; }
.state-active { background: #f0f9eb; color: #67c23a; }
.state-inactive { background: #f5f5f5; color: #909399; }
.create-time { font-size: 22rpx; color: #c0c4cc; }
.card-actions { display: flex; flex-direction: column; gap: 12rpx; flex-shrink: 0; }
.act-btn { width: 56rpx; height: 56rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 26rpx; }
.act-btn.edit { background: #ecf5ff; color: #409EFF; }
.act-btn.del { background: #fef0f0; color: #F56C6C; }

.empty { text-align: center; padding: 80rpx; color: #909399; font-size: 28rpx; }
.load-more { text-align: center; padding: 24rpx; color: #409EFF; font-size: 28rpx; }

.modal-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,.45); z-index: 200; }
.modal { position: fixed; top: 5%; left: 32rpx; right: 32rpx; bottom: 5%; background: #fff; border-radius: 20rpx; z-index: 201; display: flex; flex-direction: column; overflow: hidden; }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 28rpx 32rpx 20rpx; border-bottom: 1rpx solid #f0f0f0; flex-shrink: 0; }
.modal-title { font-size: 32rpx; font-weight: 700; color: #1A1A1A; }
.modal-close { font-size: 36rpx; color: #c0c4cc; padding: 8rpx; }
.modal-body { flex: 1; padding: 24rpx 32rpx; overflow-y: auto; }
.modal-footer { display: flex; gap: 20rpx; padding: 20rpx 32rpx 32rpx; border-top: 1rpx solid #f0f0f0; flex-shrink: 0; }
.btn-cancel, .btn-submit { flex: 1; height: 80rpx; display: flex; align-items: center; justify-content: center; border-radius: 40rpx; font-size: 28rpx; }
.btn-cancel { background: #f5f7fa; color: #606266; }
.btn-submit { background: #409EFF; color: #fff; font-weight: 600; }

.form-group { margin-bottom: 22rpx; }
.form-label { font-size: 26rpx; color: #606266; margin-bottom: 10rpx; display: block; font-weight: 500; }
.required { color: #F56C6C; }
.form-input { width: 100%; height: 76rpx; background: #f5f7fa; border-radius: 10rpx; padding: 0 24rpx; font-size: 28rpx; box-sizing: border-box; }
.form-textarea { width: 100%; height: 120rpx; background: #f5f7fa; border-radius: 10rpx; padding: 16rpx 24rpx; font-size: 26rpx; box-sizing: border-box; }
.form-row { display: flex; gap: 12rpx; }
.half { flex: 1; }
.picker-box { height: 76rpx; background: #f5f7fa; border-radius: 10rpx; padding: 0 24rpx; display: flex; align-items: center; justify-content: space-between; font-size: 28rpx; color: #303133; }
.picker-drop { margin-top: 8rpx; background: #fff; border-radius: 10rpx; border: 1rpx solid #ebeef5; max-height: 240rpx; overflow-y: auto; }
.picker-item { padding: 22rpx 24rpx; font-size: 26rpx; color: #303133; border-bottom: 1rpx solid #f5f7fa; }
.picker-item:last-child { border-bottom: none; }
.picker-item.active { color: #409EFF; background: #ecf5ff; }
.radio-group { display: flex; gap: 8rpx; }
.radio-item { flex: 1; height: 68rpx; display: flex; align-items: center; justify-content: center; background: #f5f7fa; border-radius: 10rpx; font-size: 24rpx; color: #909399; border: 2rpx solid transparent; }
.radio-item.active { background: #ecf5ff; color: #409EFF; border-color: #409EFF; }

.photo-section { display: flex; gap: 16rpx; align-items: flex-start; }
.photo-preview { position: relative; width: 160rpx; height: 160rpx; border-radius: 12rpx; overflow: hidden; flex-shrink: 0; border: 2rpx solid #ebeef5; }
.photo-img { width: 100%; height: 100%; object-fit: cover; }
.photo-remove { position: absolute; top: 6rpx; right: 6rpx; width: 40rpx; height: 40rpx; border-radius: 50%; background: rgba(0,0,0,.55); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 22rpx; }
.photo-placeholder { width: 160rpx; height: 160rpx; border-radius: 12rpx; background: #f5f7fa; border: 2rpx dashed #dcdfe6; display: flex; flex-direction: column; align-items: center; justify-content: center; flex-shrink: 0; }
.photo-placeholder-icon { font-size: 40rpx; }
.photo-placeholder-text { font-size: 20rpx; color: #c0c4cc; margin-top: 6rpx; }
.photo-btns { display: flex; flex-direction: column; gap: 12rpx; }
.photo-btn { height: 74rpx; padding: 0 28rpx; background: #f5f7fa; border-radius: 10rpx; display: flex; align-items: center; justify-content: center; font-size: 24rpx; color: #409EFF; border: 2rpx solid #d9ecff; }

.loading-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; display: flex; align-items: center; justify-content: center; z-index: 300; }
.loading-card { background: rgba(0,0,0,.7); border-radius: 16rpx; padding: 48rpx 64rpx; display: flex; flex-direction: column; align-items: center; }
.loading-spinner { font-size: 64rpx; animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.loading-text { font-size: 28rpx; color: #fff; margin-top: 20rpx; }
</style>