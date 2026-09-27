import request from '@/utils/request'

export function fetchWallet() {
    return request.get('/wallet')
}

export function queryWalletFlows(params) {
    return request.get('/wallet/flows', { params })
}

export function rechargeWallet(amount, remark) {
    return request.post('/wallet/recharge', { amount, remark })
}
