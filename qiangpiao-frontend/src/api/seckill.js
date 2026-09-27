import request from '@/utils/request'

export function doSeckill(data) {
    return request.post('/seckill/do', data)
}

export function fetchSeckillResult(trainId, seatType) {
    return request.get('/seckill/result', { params: { trainId, seatType } })
}

export function fetchStock(trainId, seatType) {
    return request.get('/seckill/stock', { params: { trainId, seatType } })
}
