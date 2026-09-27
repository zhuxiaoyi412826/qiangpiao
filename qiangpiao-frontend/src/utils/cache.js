/**
 * 前端缓存（localStorage 封装，支持 TTL）
 *
 * 用途：缓存车站列表、查询结果、用户信息等低频变更数据，
 * 与后端 L1/L2/L3 三级缓存配合，进一步降低后端压力。
 */
const PREFIX = 'qp:'

function now() {
    return Date.now()
}

export const localCache = {
    /**
     * 写入缓存
     * @param {string} key 键
     * @param {*} value 值
     * @param {number} ttlSeconds 过期时间（秒），默认 5 分钟
     */
    set(key, value, ttlSeconds = 300) {
        try {
            const item = {
                value,
                expireAt: ttlSeconds > 0 ? now() + ttlSeconds * 1000 : 0
            }
            localStorage.setItem(PREFIX + key, JSON.stringify(item))
            return true
        } catch (e) {
            console.warn('[cache] 写入失败', e)
            return false
        }
    },

    get(key) {
        try {
            const raw = localStorage.getItem(PREFIX + key)
            if (!raw) {
                return null
            }
            const item = JSON.parse(raw)
            if (item.expireAt && item.expireAt < now()) {
                localStorage.removeItem(PREFIX + key)
                return null
            }
            return item.value
        } catch (e) {
            return null
        }
    },

    remove(key) {
        localStorage.removeItem(PREFIX + key)
    },

    clearAll() {
        Object.keys(localStorage)
            .filter(k => k.startsWith(PREFIX))
            .forEach(k => localStorage.removeItem(k))
    }
}

/** 会话级缓存（刷新丢失，适合 token 之外的临时状态） */
export const sessionCache = {
    set(key, value) {
        sessionStorage.setItem(PREFIX + key, JSON.stringify(value))
    },
    get(key) {
        const raw = sessionStorage.getItem(PREFIX + key)
        return raw ? JSON.parse(raw) : null
    },
    remove(key) {
        sessionStorage.removeItem(PREFIX + key)
    }
}
