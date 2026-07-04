<template>
  <div class="page-container">
    <div class="page-header">
      <h2>小区管理</h2>
      <el-button type="primary" icon="Plus" @click="handleAdd">新增小区</el-button>
    </div>

    <div class="search-toolbar">
      <el-input v-model="searchForm.keyword" placeholder="小区名称/地址" clearable style="width:220px" @change="handleSearch" />
      <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
      <el-button icon="Refresh" @click="handleReset">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="communityId" label="ID" width="70" />
      <el-table-column prop="name" label="小区名称" width="180" />
      <el-table-column prop="address" label="地址" min-width="250" show-overflow-tooltip />
      <el-table-column label="地图坐标" width="170">
        <template #default="{ row }">
          <span v-if="row.mapLng">{{ row.mapLat }}, {{ row.mapLng }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="totalBuilding" label="楼栋数" width="80" />
      <el-table-column prop="totalHouse" label="房屋数" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top:16px;display:flex;justify-content:flex-end">
      <el-pagination v-model:current-page="searchForm.pageNum" v-model:page-size="searchForm.pageSize"
        :page-sizes="[10,20,50]" :total="total" layout="total,sizes,prev,pager,next" @change="loadData" />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="小区名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <div style="display:flex;gap:8px;width:100%">
            <el-input v-model="form.address" placeholder="请输入小区地址" style="flex:1" />
            <el-button type="primary" icon="Search" @click="handleSearchLocation" :loading="addressSearching">搜索定位</el-button>
          </div>
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="经度">
              <el-input v-model="form.mapLng" placeholder="高德地图经度" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度">
              <el-input v-model="form.mapLat" placeholder="高德地图纬度" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地图选点">
          <div id="amap-container" style="width:100%;height:300px"></div>
        </el-form-item>
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="楼栋数">
              <el-input-number v-model="form.totalBuilding" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="房屋数">
              <el-input-number v-model="form.totalHouse" :min="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCommunities, addCommunity, updateCommunity, deleteCommunity } from '@/api/property'

// ==================== 数据状态 ====================
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogTitle = ref('')
const formRef = ref(null)
const tableData = ref([])
const total = ref(0)

const searchForm = reactive({ keyword: '', pageNum: 1, pageSize: 10 })

const form = reactive({
  communityId: '', name: '', address: '', mapLng: null, mapLat: null,
  totalBuilding: 0, totalHouse: 0, description: '', status: 1
})

const rules = {
  name: [{ required: true, message: '请输入小区名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }]
}

// ==================== 地图相关状态 ====================
let mapInstance = null      // 高德地图实例
let mapMarker = null        // 当前标记点
const addressSearching = ref(false)  // 搜索按钮 loading 状态

// ==================== 地图初始化 ====================
/**
 * 初始化高德地图实例
 * 在对话框渲染完成后(nextTick)调用
 */
const initMap = () => {
  // 销毁旧地图实例，避免重复创建
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
    mapMarker = null
  }

  if (!window.AMap) {
    ElMessage.warning('高德地图加载失败，请刷新页面')
    return
  }

  // 已有坐标则以坐标为中心，否则默认北京天安门
  const lng = form.mapLng ? parseFloat(form.mapLng) : 116.397428
  const lat = form.mapLat ? parseFloat(form.mapLat) : 39.90923

  mapInstance = new AMap.Map('amap-container', {
    center: [lng, lat],
    zoom: 15,
    resizeEnable: true
  })

  // 点击地图任意位置 → 更新经纬度并移动标记
  mapInstance.on('click', (e) => {
    const clickLng = e.lnglat.getLng().toFixed(6)
    const clickLat = e.lnglat.getLat().toFixed(6)
    form.mapLng = clickLng
    form.mapLat = clickLat
    updateMarker(clickLng, clickLat)
  })

  // 编辑时如果已有坐标，放置初始标记
  if (form.mapLng && form.mapLat) {
    updateMarker(lng, lat)
  }
}

// ==================== 标记点管理 ====================
/**
 * 更新或创建地图标记点
 * 如果已存在旧标记则更新其位置，否则创建新标记
 */
const updateMarker = (lng, lat) => {
  if (mapMarker) {
    // 已存在标记 → 直接移动
    mapMarker.setPosition([lng, lat])
  } else {
    // 不存在标记 → 创建新标记
    mapMarker = new AMap.Marker({
      position: [lng, lat],
      map: mapInstance,
      animation: 'AMAP_ANIMATION_DROP' // 掉落动画
    })
  }
}

// ==================== 地址解析与地图定位 (升级版) ==================== 
const handleSearchLocation = () => { 
  const address = (form.address || '').trim() 
  const name = (form.name || '').trim() 
  
  if (!address) { 
    ElMessage.warning('请输入需要定位的地址') 
    return 
  } 
  if (!window.AMap) { 
    ElMessage.warning('地图组件初始化失败，请刷新页面重试') 
    return 
  } 

  // 核心优化：将"地址"和"小区名称"拼接起来搜索，大幅提高准确率 
  const searchKeyword = name ? `${address} ${name}` : address 
  addressSearching.value = true 
  console.log('[搜索定位] 开始搜索关键字:', searchKeyword) 

  let isResolved = false 

  const fallbackTimer = setTimeout(() => { 
    if (!isResolved) { 
      isResolved = true 
      addressSearching.value = false 
      ElMessage.error('请求超时：请检查网络或高德服务状态') 
    } 
  }, 8000) 

  try { 
    // 同时加载 PlaceSearch (POI搜索) 和 Geocoder (地理编码) 
    window.AMap.plugin(['AMap.PlaceSearch', 'AMap.Geocoder'], () => { 
      if (isResolved) return 

      // 1. 优先使用 PlaceSearch 进行精准匹配（最适合找小区、大厦等具体名字） 
      const placeSearch = new window.AMap.PlaceSearch({ 
        citylimit: false, // 全国范围搜索 
        pageSize: 1 
      }) 

      placeSearch.search(searchKeyword, (status, result) => { 
        if (isResolved) return 

        if (status === 'complete' && result.info === 'OK' && result.poiList && result.poiList.pois.length > 0) { 
          // 找到了具体的 POI（如：龙江明珠） 
          isResolved = true 
          clearTimeout(fallbackTimer) 
          addressSearching.value = false 

          const poi = result.poiList.pois[0] 
          updateMapAndForm(poi.location.getLng(), poi.location.getLat(), 'POI搜索') 
        } else { 
          // 2. 如果 POI 没搜到，降级使用 Geocoder（适合纯街道地址，如：xx路xx号） 
          const geocoder = new window.AMap.Geocoder() 
          geocoder.getLocation(address, (geoStatus, geoResult) => { 
            if (isResolved) return 
            isResolved = true 
            clearTimeout(fallbackTimer) 
            addressSearching.value = false 

            if (geoStatus === 'complete' && geoResult.info === 'OK' && geoResult.geocodes.length > 0) { 
              const loc = geoResult.geocodes[0].location 
              updateMapAndForm(loc.getLng(), loc.getLat(), '地址解析') 
            } else { 
              ElMessage.warning('未找到精确坐标，请尝试输入更详细的地址或小区名') 
            } 
          }) 
        } 
      }) 
    }) 
  } catch (err) { 
    if (!isResolved) { 
      isResolved = true 
      clearTimeout(fallbackTimer) 
      addressSearching.value = false 
    } 
    console.error('[搜索定位] 插件加载异常:', err) 
    ElMessage.error('高德地图搜索组件出错') 
  } 

  // 提取公共的更新状态与地图视图的方法 
  function updateMapAndForm(lng, lat, source) { 
    console.log(`[搜索定位] 成功 (${source}) → 经度: ${lng}, 纬度: ${lat}`) 
    form.mapLng = lng.toFixed(6) 
    form.mapLat = lat.toFixed(6) 

    if (mapInstance) { 
      mapInstance.setCenter([lng, lat]) 
      mapInstance.setZoom(16) // 放大层级，看小区更清晰 
      updateMarker(lng, lat) 
    } 
  } 
}
 

// ==================== 业务 CRUD ====================
const loadData = async () => {
  loading.value = true
  try {
    const { data } = await getCommunities(searchForm)
    tableData.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

const handleSearch = () => { searchForm.pageNum = 1; loadData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.pageNum = 1; loadData() }

const handleAdd = () => {
  isEdit.value = false; dialogTitle.value = '新增小区'; dialogVisible.value = true
  nextTick(() => initMap())
}

const handleEdit = (row) => {
  isEdit.value = true; dialogTitle.value = '编辑小区'
  Object.assign(form, row)
  dialogVisible.value = true
  nextTick(() => initMap())
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该小区？', '提示', { type: 'warning' })
  await deleteCommunity(row.communityId)
  ElMessage.success('删除成功'); loadData()
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, {
    communityId: '', name: '', address: '', mapLng: null, mapLat: null,
    totalBuilding: 0, totalHouse: 0, description: '', status: 1
  })
  // 销毁地图实例，下次打开重新初始化
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
    mapMarker = null
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (isEdit.value) { await updateCommunity(form) } else { await addCommunity(form) }
  ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
  dialogVisible.value = false; loadData()
}

onMounted(() => loadData())
</script>
