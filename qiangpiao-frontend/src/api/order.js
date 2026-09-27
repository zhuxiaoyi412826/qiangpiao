import request from '@/utils/request'

export function queryOrders(params) {
    return request.get('/orders', { params })
}

export function fetchOrderDetail(orderNo) {
    return request.get(`/orders/${orderNo}`)
}

export function payOrder(orderNo) {
    return request.post('/orders/pay', { orderNo, payType: 'ALIPAY' })
}

export function cancelOrder(orderNo) {
    return request.post(`/orders/${orderNo}/cancel`)
}
