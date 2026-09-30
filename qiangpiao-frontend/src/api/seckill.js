import request from '@/utils/request'

export function doSeckill(data) {
    return request.post('/seckill/do', data)
}

export function fetchSeckillResult(trainId, seatType) {
    return request.get('/seckill/result', { params: { trainId, seatType } })
}

/** 批量抢票结果（一次买多张，逐张给状态） */
export function fetchBatchSeckillResult(batchNo) {
    return request.get('/seckill/batch/result', { params: { batchNo } })
}

export function fetchStock(trainId, seatType) {
    return request.get('/seckill/stock', { params: { trainId, seatType } })
}
