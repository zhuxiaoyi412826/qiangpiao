import { defineStore } from 'pinia'
import { localCache } from '@/utils/cache'
import { login, register, fetchUserInfo } from '@/api/auth'

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
        async loadUserInfo() {
            if (!this.token) {
                return null
            }
            try {
                this.userInfo = await fetchUserInfo()
                localCache.set('userInfo', this.userInfo, 7200)
                return this.userInfo
            } catch (e) {
                this.logout()
                return null
            }
        },
        logout() {
            this.token = ''
            this.userInfo = null
            localCache.remove('token')
            localCache.remove('userInfo')
        }
    }
})
