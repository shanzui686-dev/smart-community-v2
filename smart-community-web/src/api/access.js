import request from '@/utils/request'

// 出入记录
export function getRecords(params) { return request.get('/access/record/list', { params }) }

// 访客管理
export function getVisitors(params) { return request.get('/access/visitor/list', { params }) }
export function addVisitor(data) { return request.post('/access/visitor', data) }
export function updateVisitor(data) { return request.put('/access/visitor', data) }
export function cancelVisitor(id) { return request.put(`/access/visitor/${id}/cancel`) }
export function checkInVisitor(id) { return request.put(`/access/visitor/${id}/check-in`) }
export function checkOutVisitor(id) { return request.put(`/access/visitor/${id}/check-out`) }

// 数据统计
export function getStatistics() { return request.get('/statistics/dashboard') }
