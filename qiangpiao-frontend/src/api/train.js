import request from '@/utils/request'

export function queryTrains(params) {
    return request.get('/trains', { params })
}

export function fetchTrainDetail(trainId) {
    return request.get(`/trains/${trainId}`)
}

export function fetchSeatMap(trainId, seatType) {
    return request.get(`/seats/${trainId}/${seatType}`)
}
