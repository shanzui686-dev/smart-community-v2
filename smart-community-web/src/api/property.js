import request from '@/utils/request'

// 小区管理
export function getCommunities(params) { return request.get('/property/community/list', { params }) }
export function getAllCommunities() { return request.get('/property/community/all') }
export function getCommunity(id) { return request.get(`/property/community/${id}`) }
export function addCommunity(data) { return request.post('/property/community', data) }
export function updateCommunity(data) { return request.put('/property/community', data) }
export function deleteCommunity(id) { return request.delete(`/property/community/${id}`) }

// 居民管理
export function getPersons(params) { return request.get('/property/person/list', { params }) }
export function getPerson(id) { return request.get(`/property/person/${id}`) }
export function addPerson(data) { return request.post('/property/person', data) }
export function updatePerson(data) { return request.put('/property/person', data) }
export function deletePerson(id) { return request.delete(`/property/person/${id}`) }
export function importPersons(data) { return request.post('/property/person/import', data, { headers: { 'Content-Type': 'multipart/form-data' } }) }

// 摄像头管理
export function getCameras(params) { return request.get('/property/camera/list', { params }) }
export function addCamera(data) { return request.post('/property/camera', data) }
export function updateCamera(data) { return request.put('/property/camera', data) }
export function deleteCamera(id) { return request.delete(`/property/camera/${id}`) }

// 车辆管理
export function getVehicles(params) { return request.get('/property/vehicle/list', { params }) }
export function getVehicle(id) { return request.get(`/property/vehicle/${id}`) }
export function addVehicle(data) { return request.post('/property/vehicle', data) }
export function updateVehicle(data) { return request.put('/property/vehicle', data) }
export function deleteVehicle(id) { return request.delete(`/property/vehicle/${id}`) }

// 公告管理
export function getAnnouncements(params) { return request.get('/announcement/page', { params }) }
export function addAnnouncement(data) { return request.post('/announcement', data) }
export function updateAnnouncement(data) { return request.put('/announcement', data) }
export function deleteAnnouncement(id) { return request.delete(`/announcement/${id}`) }
export function getHomeAnnouncements() { return request.get('/announcement/home') }

// 人脸识别
export function faceSearch(imageUrl) { return request.post('/face/search', null, { params: { imageUrl } }) }
export function faceCompare(imageUrl1, imageUrl2) { return request.post('/face/compare', null, { params: { imageUrl1, imageUrl2 } }) }
