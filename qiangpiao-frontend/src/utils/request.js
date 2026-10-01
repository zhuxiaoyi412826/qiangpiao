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

/** 链路追踪头：与后端 TraceIdInterceptor.TRACE_HEADER 保持一致 */
const TRACE_HEADER = 'X-Trace-Id'
/** 最近一次请求的 traceId：报错弹窗 / 用户报障时可直接给出，与服务端日志一一对应 */
let lastTraceId = ''

/** 生成 16 位 traceId；同一次请求的重试复用同一个，便于把重试日志串成一条链路 */
function genTraceId () {
    let r = ''
    for (let i = 0; i < 12; i++) {
        r += Math.floor(Math.random() * 16).toString(16)
    }
    return Date.now().toString(16).slice(-4) + r
}

export function getLastTraceId () {
    return lastTraceId
}

/** 错误对象带上业务码 / HTTP 状态，方便调用方（如 store）判断是不是「登录失效」 */
function buildError (message, code, traceId) {
    const err = new Error(message)
    err.code = code
    err.traceId = traceId || ''
    return err
}

/**
 * 登录失效统一处理：登出 / 被管理端强制下线 / 账号被封都会走到这里。
 * 注意 Spring Security 的 authenticationEntryPoint 返回的是 HTTP 401 + JSON body，
 * axios 会把它当「请求错误」，所以 success 分支和 error 分支都要覆盖。
 */
let authExpiredFired = false
function handleAuthExpired (message) {
    localCache.remove('token')
    localCache.remove('userInfo')
    if (authExpiredFired) {
        return
    }
    authExpiredFired = true
    // 交给 App.vue 清理 Pinia 并跳转：避免在 request 层反向依赖 router / store
    window.dispatchEvent(new CustomEvent('auth:expired', { detail: message || '登录已失效' }))
    // 2 秒内同一波并发 401 只处理一次；用户重新登录后恢复
    setTimeout(() => {
        authExpiredFired = false
    }, 2000)
}

/** 失败提示带上 traceId：用户截图报障时，后端可直接按这个 ID 捞全链路日志 */
function withTrace (message, traceId) {
    return traceId ? `${message}（traceId：${traceId}）` : message
}

service.interceptors.request.use(config => {
    const token = localCache.get('token')
    if (token) {
        config.headers.Authorization = 'Bearer ' + token
    }
    // 前端生成的 traceId 透传给后端：后端优先采用，前后端日志即可对齐
    if (!config.headers[TRACE_HEADER]) {
        config.headers[TRACE_HEADER] = config.__traceId || genTraceId()
    }
    config.__traceId = config.headers[TRACE_HEADER]
    return config
}, error => Promise.reject(error))

service.interceptors.response.use(response => {
    const body = response.data
    const traceId = (response.headers && response.headers['x-trace-id'])
        || (body && body.traceId) || ''
    if (traceId) {
        lastTraceId = traceId
    }
    // 后端统一出参：{ code, message, data, timestamp, traceId }
    if (body && body.code === 200) {
        return body.data
    }
    const message = (body && body.message) || '请求失败'
    // HTTP 200 但业务码 401（如自定义拦截器返回的未登录）
    if (body && body.code === 401) {
        handleAuthExpired(message)
        return Promise.reject(buildError(message, 401, traceId))
    }
    ElMessage.error(withTrace(message, traceId))
    return Promise.reject(buildError(message, body && body.code, traceId))
}, error => {
    if (error.response) {
        const { status, data } = error.response
        const traceId = (error.response.headers && error.response.headers['x-trace-id'])
            || (data && data.traceId) || ''
        if (traceId) {
            lastTraceId = traceId
        }
        const message = (data && data.message) || `请求异常（${status}）`
        // 被踢下线 / 封号后，受保护接口会被 Spring Security 拦成 HTTP 401
        if (status === 401 || (data && data.code === 401)) {
            handleAuthExpired(message)
            return Promise.reject(buildError(message, 401, traceId))
        }
        ElMessage.error(withTrace(message, traceId))
        return Promise.reject(buildError(message, (data && data.code) || status, traceId))
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
