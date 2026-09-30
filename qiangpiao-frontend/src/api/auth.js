import request from '@/utils/request'

export function login(data) {
    return request.post('/auth/login', data)
}

export function register(data) {
    return request.post('/auth/register', data)
}

export function fetchUserInfo() {
    return request.get('/auth/info')
}

/** 登出：后端把 token 拉黑，前端随后清本地缓存 */
export function logout() {
    return request.post('/auth/logout')
}
