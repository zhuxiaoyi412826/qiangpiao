import request from '@/utils/request'

// ============ 车站 ============
export const adminStations = () => request.get('/admin/stations')
export const saveAdminStation = data => request.post('/admin/stations', data)
export const updateStationStatus = (id, status) =>
    request.put(`/admin/stations/${id}/status`, null, { params: { status } })

// ============ 线路 ============
export const adminLines = () => request.get('/admin/lines')
export const saveAdminLine = data => request.post('/admin/lines', data)
export const deleteAdminLine = id => request.delete(`/admin/lines/${id}`)
export const adminLineStations = lineId => request.get(`/admin/lines/${lineId}/stations`)
export const saveAdminLineStations = (lineId, stations) =>
    request.post(`/admin/lines/${lineId}/stations`, stations)

// ============ 车次 / 排班 ============
export const adminTrains = params => request.get('/admin/trains', { params })
export const updateTrainStatus = (id, status) =>
    request.put(`/admin/trains/${id}/status`, null, { params: { status } })
export const adminStops = id => request.get(`/admin/trains/${id}/stops`)
export const saveAdminStops = (id, stops) => request.post(`/admin/trains/${id}/stops`, stops)
export const adminCarriages = id => request.get(`/admin/trains/${id}/carriages`)
export const saveAdminCarriages = (id, carriages) => request.post(`/admin/trains/${id}/carriages`, carriages)
export const generateDailyTrain = (id, date) =>
    request.post(`/admin/trains/${id}/schedule`, null, { params: { date } })
/** 售卖时间窗口：start / end 传空字符串表示不限时 */
export const updateSaleWindow = (id, start, end) =>
    request.post(`/admin/trains/${id}/sale-window`, null, { params: { start, end } })
/** 初始化区间库存（相邻站单段）并预热到 Redis */
export const initSegmentStock = id => request.post(`/admin/trains/${id}/segment/init`)

// ============ 票价 ============
export const updateStockPrice = (id, price) =>
    request.put(`/admin/stocks/${id}/price`, null, { params: { price } })
export const updateStockTotal = (id, totalCount) =>
    request.put(`/admin/stocks/${id}/total`, null, { params: { totalCount } })

// ============ 订单 ============
export const adminOrders = params => request.get('/admin/orders', { params })
export const adminOrderDetail = orderNo => request.get(`/admin/orders/${orderNo}`)
export const adminOrderLogs = orderNo => request.get(`/admin/orders/${orderNo}/logs`)
export const adminOrderChanges = orderNo => request.get(`/admin/orders/${orderNo}/changes`)
export const adminRefundOrder = (orderNo, reason) => request.post(`/admin/orders/${orderNo}/refund`, { reason })

/** 抢票流水：排查超卖与抢票失败 */
export const adminSeckillFlows = params => request.get('/admin/seckill/flows', { params })

// ============ 用户 ============
export const adminUsers = params => request.get('/admin/users', { params })
export const updateUserStatus = (id, status) =>
    request.put(`/admin/users/${id}/status`, null, { params: { status } })
/** 强制下线：拉黑该用户所有 token，返回失效 token 数量 */
export const kickUser = id => request.post(`/admin/users/${id}/kick`)

// ============ 公告 ============
export const adminAnnouncements = params => request.get('/admin/announcements', { params })
export const saveAdminAnnouncement = data => request.post('/admin/announcements', data)
export const deleteAdminAnnouncement = id => request.delete(`/admin/announcements/${id}`)

// ============ 风控 / 限流 ============
/** 风控事件（同一 IP 多账号、极短耗时请求命中记录） */
export const adminRiskEvents = limit => request.get('/admin/risk/events', { params: { limit } })
/** IP 黑名单：返回 { ip: 原因 } */
export const adminBlacklist = () => request.get('/admin/risk/blacklist')
export const adminBlockIp = (ip, seconds, reason) =>
    request.post('/admin/risk/blacklist', null, { params: { ip, seconds, reason } })
export const adminUnblockIp = ip => request.delete(`/admin/risk/blacklist/${ip}`)

// ============ 监控 / 报表 ============
export const adminStockMonitor = params => request.get('/admin/monitor/stock', { params })
export const adminLockedSeats = limit => request.get('/admin/monitor/locked-seats', { params: { limit } })
export const adminStats = () => request.get('/admin/stats')
export const adminDailySales = () => request.get('/admin/stats/daily-sales')
export const adminTrainSales = () => request.get('/admin/stats/train-sales')
export const adminUserGrowth = () => request.get('/admin/stats/user-growth')

export function exportReport(type) {
    return request.get(`/admin/stats/export`, { params: { type }, responseType: 'blob' })
}
