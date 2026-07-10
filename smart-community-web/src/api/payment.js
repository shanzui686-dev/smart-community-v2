import request from '@/utils/request'

// 账单管理
export function getBillList(params) { return request.get('/financial/bill/list', { params }) }
export function getBillDetail(id) { return request.get(`/financial/bill/${id}`) }
export function payBill(billId, amount, payMethod) {
  return request.post(`/financial/bill/${billId}/pay`, null, { params: { amount, payMethod } })
}
export function sendDunning(billId) { return request.post(`/financial/bill/${billId}/dunning`) }
export function generateMonthlyBills() { return request.post('/financial/bill/generate') }

// 缴费流水
export function getPaymentRecords(params) { return request.get('/financial/payment-record/list', { params }) }
export function getPaymentRecordsByBill(billId) { return request.get(`/financial/payment-record/by-bill/${billId}`) }

// 计费标准
export function getFeeStandardList(params) { return request.get('/financial/fee-standard/list', { params }) }
export function getEnabledFeeStandards() { return request.get('/financial/fee-standard/enabled') }
export function getFeeStandard(id) { return request.get(`/financial/fee-standard/${id}`) }
export function addFeeStandard(data) { return request.post('/financial/fee-standard', data) }
export function updateFeeStandard(data) { return request.put('/financial/fee-standard', data) }
export function deleteFeeStandard(id) { return request.delete(`/financial/fee-standard/${id}`) }

// 历史缴费明细
export function getBillHistory(personId) { return request.get(`/financial/bill/history/${personId}`) }

// 财务统计报表
export function getFinancialReport() { return request.get('/financial/report') }
