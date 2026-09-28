import request from '@/utils/request'

export function queryTrains(params) {
    return request.get('/trains', { params })
}

export function fetchTrainDetail(trainId) {
    return request.get(`/trains/${trainId}`)
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
