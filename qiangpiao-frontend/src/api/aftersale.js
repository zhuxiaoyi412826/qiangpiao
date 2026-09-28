import request from '@/utils/request'

export function refundOrder(orderNo, reason) {
    return request.post('/after-sale/refund', { orderNo, reason })
}

export function changeOrder(orderNo, newTrainId, seatType, reason) {
    return request.post('/after-sale/change', { orderNo, newTrainId, seatType, reason })
}

export function fetchTimeline(orderNo) {
    return request.get(`/after-sale/${orderNo}/timeline`)
}

export function fetchChanges(orderNo) {
    return request.get(`/after-sale/${orderNo}/changes`)
}
