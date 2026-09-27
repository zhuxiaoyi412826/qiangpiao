import request from '@/utils/request'

export function fetchStations() {
    return request.get('/stations')
}

export function searchStations(keyword) {
    return request.get('/stations/search', { params: { keyword } })
}
