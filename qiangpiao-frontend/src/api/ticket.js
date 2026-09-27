import request from '@/utils/request'

export function queryMyTickets(params) {
    return request.get('/tickets', { params })
}
