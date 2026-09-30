import request from '@/utils/request'

export function refundOrder(orderNo, reason) {
    return request.post('/after-sale/refund', { orderNo, reason })
}

export function changeOrder(orderNo, newTrainId, seatType, reason) {
    return request.post('/after-sale/change', { orderNo, newTrainId, seatType, reason })
}

/** 退票费用试算：票价 / 手续费 / 实退金额 / 计费档位 */
export function previewRefund(orderNo) {
    return request.get(`/after-sale/${orderNo}/refund-preview`)
}

/** 改签费用试算：差额 / 手续费 / 实退或实补 */
export function previewChange(orderNo, newTrainId, seatType) {
    return request.get(`/after-sale/${orderNo}/change-preview`, { params: { newTrainId, seatType } })
}

export function fetchTimeline(orderNo) {
    return request.get(`/after-sale/${orderNo}/timeline`)
}

export function fetchChanges(orderNo) {
    return request.get(`/after-sale/${orderNo}/changes`)
}
