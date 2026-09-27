import axios from 'axios'
import { ElMessage } from 'element-plus'
import { localCache } from './cache'

/**
 * axios 封装：统一 baseURL / Token / 响应解包 / 错误提示
 */
const service = axios.create({
    baseURL: '/api',
    timeout: 20000,
    headers: { 'Content-Type': 'application/json;charset=UTF-8' }
})

service.interceptors.request.use(config => {
    const token = localCache.get('token')
    if (token) {
        config.headers.Authorization = 'Bearer ' + token
    }
    return config
}, error => Promise.reject(error))

service.interceptors.response.use(response => {
    const body = response.data
    // 后端统一出参：{ code, message, data, timestamp }
    if (body && body.code === 200) {
        return body.data
    }
    const message = (body && body.message) || '请求失败'
    ElMessage.error(message)
    if (body && body.code === 401) {
        localCache.remove('token')
        localCache.remove('userInfo')
        setTimeout(() => {
            window.location.href = '/login'
        }, 300)
    }
    return Promise.reject(new Error(message))
}, error => {
    if (error.response) {
        const { status, data } = error.response
        const message = (data && data.message) || `请求异常（${status}）`
        ElMessage.error(message)
        return Promise.reject(new Error(message))
    }
    ElMessage.error('网络异常，请检查后端服务是否启动')
    return Promise.reject(error)
})

export default service
