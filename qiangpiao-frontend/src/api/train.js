import request from '@/utils/request'

export function queryTrains(params) {
    return request.get('/trains', { params })
}

/** 车次详情；params 可带 { from, to } —— 带上后余票与座位图按该乘车区间计算 */
export function fetchTrainDetail(trainId, params) {
    return request.get(`/trains/${trainId}`, { params })
}

export function fetchBuyBlock(trainId) {
    return request.get(`/trains/${trainId}/buy-block`)
}

export function fetchSeatMap(trainId, seatType) {
    return request.get(`/seats/${trainId}/${seatType}`)
}

/** 车次时刻表（站点时序） */
export function fetchTrainStops(trainId) {
    return request.get(`/trains/${trainId}/stops`)
}
