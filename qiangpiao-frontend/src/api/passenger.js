import request from '@/utils/request'

export function fetchPassengers() {
    return request.get('/passengers')
}

export function addPassenger(data) {
    return request.post('/passengers', data)
}

export function updatePassenger(id, data) {
    return request.put(`/passengers/${id}`, data)
}

export function deletePassenger(id) {
    return request.delete(`/passengers/${id}`)
}
