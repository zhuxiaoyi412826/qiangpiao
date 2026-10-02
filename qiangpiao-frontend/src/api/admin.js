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
/** 初始化区间库存（相邻站单段，并自动按里程折算段价）并预热到 Redis */
export const initSegmentStock = id => request.post(`/admin/trains/${id}/segment/init`)
/** 区间库存与各段段价 */
export const adminSegments = id => request.get(`/admin/trains/${id}/segments`)
/** 保存段价：segments = [{seatType, segIndex, price}]，price 为 null 表示清空后按里程自动折算 */
export const saveAdminSegments = (id, segments) =>
    request.post(`/admin/trains/${id}/segment/price`, segments)

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

// ============ 资金对账（Job 发现差异 → 落单据 → 人工审核 → 退补账） ============
/** 异常单据列表：status 为空查全部；0 待审核 1 已通过 2 已驳回 3 已关闭 */
export const adminReconBills = params => request.get('/admin/recon/bills', { params })
/** 待审核数量：用于页签上的红点提示 */
export const adminReconPendingCount = () => request.get('/admin/recon/bills/pending-count')
/** 审核：approve=true 通过后按建议动作执行退补账；remark 选填，为空时不传 */
export const auditReconBill = (billNo, approve, remark) =>
    request.post(`/admin/recon/bills/${billNo}/audit`, null, {
        params: { approve, ...(remark ? { remark } : {}) }
    })
/** 手动跑一轮对账（不等定时任务） */
export const runRecon = () => request.post('/admin/recon/run')
