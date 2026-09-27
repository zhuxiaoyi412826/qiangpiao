import { defineStore } from 'pinia'
import { localCache } from '@/utils/cache'
import { fetchStations } from '@/api/station'

/**
 * 车站状态：车站属于极低频变更数据，前端缓存 1 小时，避免每次进页面都打后端
 */
export const useStationStore = defineStore('station', {
    state: () => ({
        stations: localCache.get('stations') || [],
        loadedAt: localCache.get('stationsLoadedAt') || 0
    }),
    actions: {
        async loadStations(force = false) {
            if (!force && this.stations && this.stations.length > 0) {
                return this.stations
            }
            const data = await fetchStations()
            this.stations = data || []
            this.loadedAt = Date.now()
            localCache.set('stations', this.stations, 3600)
            localCache.set('stationsLoadedAt', this.loadedAt, 3600)
            return this.stations
        },
        clear() {
            this.stations = []
            localCache.remove('stations')
            localCache.remove('stationsLoadedAt')
        }
    }
})
