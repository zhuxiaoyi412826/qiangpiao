import request from '@/utils/request'

export function fetchPayment(payNo) {
    return request.get(`/payments/${payNo}`)
}

// 模拟第三方渠道推送支付结果（演示两阶段支付用）
export function mockPayCallback(payNo, result = 'SUCCESS') {
    return request.post(`/payments/${payNo}/mock-callback`, null, { params: { result } })
}
