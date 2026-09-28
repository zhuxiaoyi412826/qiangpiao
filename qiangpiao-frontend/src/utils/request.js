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
    // 幂等请求自动重试后再判定失败
    if (canRetry(error)) {
        const config = error.config
        config.__retryCount = (config.__retryCount || 0) + 1
        if (config.__retryCount <= RETRY_LIMIT) {
            const delay = RETRY_DELAY * config.__retryCount
            console.warn(`[request] 第 ${config.__retryCount} 次重试：${config.url}（${delay}ms 后）`)
            return new Promise(resolve => {
                setTimeout(() => resolve(service(config)), delay)
            })
        }
    }
    if (error.code === 'ECONNABORTED') {
        const message = '请求超时，请检查网络后重试'
        ElMessage.error(message)
        return Promise.reject(new Error(message))
    }
    ElMessage.error('网络异常，请检查后端服务是否启动')
    return Promise.reject(error)
})

/** 自动重试次数（仅幂等 GET） */
const RETRY_LIMIT = 2
/** 重试退避基数（ms） */
const RETRY_DELAY = 400

/**
 * 判定是否值得重试：只对 GET 幂等请求重试，避免重复下单 / 重复支付。
 * 触发条件：网络中断、超时、5xx。
 */
function canRetry(error) {
    if (!error || !error.config) {
        return false
    }
    const method = (error.config.method || '').toLowerCase()
    if (method !== 'get') {
        return false
    }
    if (error.code === 'ECONNABORTED') {
        return true
    }
    if (!error.response) {
        return true
    }
    return error.response.status >= 500
}

export default service
