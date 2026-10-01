import { defineStore } from 'pinia'
import { localCache } from '@/utils/cache'
import { login, register, fetchUserInfo, logout as reqLogout } from '@/api/auth'

/**
 * 用户状态（Pinia）：登录态 + 个人信息，token 落前端缓存
 */
export const useUserStore = defineStore('user', {
    state: () => ({
        token: localCache.get('token') || '',
        userInfo: localCache.get('userInfo') || null
    }),
    getters: {
        isLogin: state => !!state.token,
        username: state => (state.userInfo && state.userInfo.username) || ''
    },
    actions: {
        async doLogin(payload) {
            const vo = await login(payload)
            this.applyLogin(vo)
            return vo
        },
        async doRegister(payload) {
            const vo = await register(payload)
            this.applyLogin(vo)
            return vo
        },
        applyLogin(vo) {
            this.token = vo.token
            localCache.set('token', vo.token, vo.expiresIn || 7200)
            this.userInfo = {
                userId: vo.userId,
                username: vo.username,
                realName: vo.realName,
                roles: vo.roles
            }
            localCache.set('userInfo', this.userInfo, vo.expiresIn || 7200)
        },
        /**
         * 拉取用户信息（登录态心跳也走这里）。
         * 401 = 已登出 / 被管理端强制下线 / 账号被封：只清本地，别再调 logout 接口（它自己也会 401）。
         */
        async loadUserInfo() {
            if (!this.token) {
                return null
            }
            try {
                this.userInfo = await fetchUserInfo()
                localCache.set('userInfo', this.userInfo, 7200)
                return this.userInfo
            } catch (e) {
                if (e && (e.code === 401 || e.status === 401)) {
                    this.clearAuth()
                    return null
                }
                this.logout()
                return null
            }
        },
        /** 只清本地登录态：token 失效 / 被踢下线时用，不再请求后端 */
        clearAuth() {
            this.token = ''
            this.userInfo = null
            localCache.remove('token')
            localCache.remove('userInfo')
        },
        /** 登出：先通知后端把 token 拉黑（失败也要清本地，不能卡住用户退出） */
        async logout() {
            try {
                if (this.token) {
                    await reqLogout()
                }
            } catch (e) {
                console.warn('[user] 登出接口调用失败，已清理本地登录态', e.message)
            }
            this.clearAuth()
        }
    }
})
