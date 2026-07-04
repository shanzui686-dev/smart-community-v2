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
