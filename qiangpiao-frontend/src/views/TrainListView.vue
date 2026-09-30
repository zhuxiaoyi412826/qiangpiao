<template>
  <div class="page-container">
    <div class="card-panel">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="出发站">
          <!-- 站点 3000+：虚拟滚动选择器；输入时走后端搜索（城市 / 拼音简码 / 站名） -->
          <el-select-v2 v-model="query.fromStation" :options="stationOptions" filterable remote clearable
                        :remote-method="onStationSearch" :loading="stationLoading"
                        @visible-change="onStationVisible"
                        placeholder="城市 / 拼音简码 / 站名" style="width: 200px"/>
        </el-form-item>
        <el-form-item label="到达站">
          <el-select-v2 v-model="query.toStation" :options="stationOptions" filterable remote clearable
                        :remote-method="onStationSearch" :loading="stationLoading"
                        @visible-change="onStationVisible"
                        placeholder="城市 / 拼音简码 / 站名" style="width: 200px"/>
        </el-form-item>
        <el-form-item label="出发日期">
          <el-date-picker v-model="query.departDate" type="date" value-format="YYYY-MM-DD"
                          :disabled-date="disabledDate" placeholder="选择日期" style="width: 160px"/>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="search(false)">查询</el-button>
          <el-button @click="search(true)">强制刷新</el-button>
        </el-form-item>
      </el-form>
      <div class="muted">
        提示：仅展示未发车车次；可查询今天起 30 天内，预售期 14 天，开车前 20 分钟停止售票。
        <el-tag v-if="fromCache" size="small" type="info" style="margin-left: 8px">来自前端缓存</el-tag>
      </div>
    </div>

    <div class="card-panel">
      <SkeletonCard v-if="loading" :rows="4" show-table/>
      <ErrorRetry v-else-if="errorMsg" :message="errorMsg" @retry="search(false)"/>
      <template v-else>
      <el-table :data="list" border style="width: 100%">
        <el-table-column label="车次" width="130">
          <template #default="{ row }">
            <!-- 点击车次号查看时刻表（站点时序） -->
            <a class="train-no train-link" title="点击查看时刻表" @click="openStops(row)">{{ row.trainNo }}</a>
            <div class="muted">{{ row.trainType }}</div>
          </template>
        </el-table-column>
        <el-table-column label="出发 / 到达" min-width="200">
          <template #default="{ row }">
            <div>{{ row.fromStationName }} → {{ row.toStationName }}</div>
            <div class="muted">{{ row.departTime }} ~ {{ row.arriveTime }}（{{ row.durationText }}）</div>
          </template>
        </el-table-column>
        <el-table-column label="出发日期" prop="departDate" width="120"/>
        <el-table-column label="余票 / 票价" min-width="300">
          <template #default="{ row }">
            <div v-for="s in row.stocks" :key="s.seatType" class="stock-row">
              <span>{{ s.seatTypeName }}</span>
              <span class="price">¥{{ s.price }}</span>
              <el-tag :type="s.availableCount > 0 ? 'success' : 'danger'" size="small">
                {{ s.availableCount > 0 ? '余票 ' + s.availableCount : '无票' }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="售票状态" width="160">
          <template #default="{ row }">
            <el-tag v-if="row.sellTip" type="info" size="small">{{ row.sellTip }}</el-tag>
            <el-tag v-else type="success" size="small">可购票</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" :disabled="!!row.sellTip" @click="goDetail(row.id)">抢票</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="当日没有符合条件的未发车车次">
            <el-button type="primary" size="small" @click="pickTomorrow">查看明天的车次</el-button>
          </el-empty>
        </template>
      </el-table>

      <el-pagination class="pager" background layout="total, prev, pager, next"
                     :current-page="query.pageNum" :page-size="query.pageSize" :total="total"
                     @current-change="onPageChange"/>
      </template>
    </div>

    <!-- 时刻表（站点时序）弹窗：点击车次号查看，12306 同款样式 -->
    <el-dialog v-model="stopsVisible" title="时刻表" width="560px"
               :close-on-click-modal="false" append-to-body>
      <div v-loading="stopsLoading" style="min-height: 160px">
        <el-table :data="stops" border size="small"
                  :header-cell-style="{ background: '#3a8ee6', color: '#fff', fontWeight: 600 }"
                  max-height="420">
          <el-table-column label="站序" width="70" align="center">
            <template #default="{ row }">{{ String(row.stopOrder).padStart(2, '0') }}</template>
          </el-table-column>
          <el-table-column label="站名" min-width="120">
            <template #default="{ row }">
              <span :class="{ 'stop-current': row.stationName === activeTrain.fromStationName }">
                {{ row.stationName }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="到站时间" width="110" align="center">
            <template #default="{ row }">{{ fmtTime(row.arriveTime) }}</template>
          </el-table-column>
          <el-table-column label="出发时间" width="110" align="center">
            <template #default="{ row }">{{ fmtTime(row.departTime) }}</template>
          </el-table-column>
          <el-table-column label="停留时间" width="100" align="center">
            <template #default="{ row }">{{ row.stopMinutes > 0 ? row.stopMinutes + '分钟' : '----' }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!stopsLoading && !stops.length" description="该车次未配置时刻表" :image-size="60"/>
        <div v-if="stops.length" class="stops-footer">
          <strong class="stops-train">{{ activeTrain.trainNo }}次</strong>
          <span>{{ activeTrain.fromStationName }} --&gt; {{ activeTrain.toStationName }}</span>
          <span>{{ activeTrain.trainType }}</span>
          <el-tag type="success" size="small">有空调</el-tag>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="stopsVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { queryTrains, fetchTrainStops } from '@/api/train'
import { searchStations } from '@/api/station'
import SkeletonCard from '@/components/SkeletonCard.vue'
import ErrorRetry from '@/components/ErrorRetry.vue'
import { useStationStore } from '@/store/station'
import { localCache } from '@/utils/cache'
import dayjs from '@/utils/dayjs'

const router = useRouter()
const stationStore = useStationStore()

const stations = computed(() => stationStore.stations)

/** option 文案：站名 + 城市，方便一眼区分同城多个车站 */
function stationLabel(s) {
  const city = s.city && s.city !== s.stationName ? s.city : ''
  return city ? `${s.stationName}（${city}）` : s.stationName
}

/** 未输入关键词时的全量列表（虚拟滚动，只渲染可视项） */
const baseOptions = computed(() => stations.value.map(s => ({ value: s.stationName, label: stationLabel(s) })))

const remoteKeyword = ref('')
const remoteOptions = ref([])
const stationLoading = ref(false)

/**
 * 输入关键词时走后端搜索：支持城市名 / 拼音简码 / 站名
 * - 搜城市（北京 / bj）→ 返回该城市全部车站
 * - 搜具体站名 → 只返回那一个车站
 */
let stationTimer = null
async function onStationSearch(keyword) {
  const kw = (keyword || '').trim()
  remoteKeyword.value = kw
  if (stationTimer) {
    clearTimeout(stationTimer)
    stationTimer = null
  }
  if (!kw) {
    remoteOptions.value = []
    stationLoading.value = false
    return
  }
  stationLoading.value = true
  // 简单防抖：连续输入时只在停手 250ms 后请求一次
  stationTimer = setTimeout(async () => {
    try {
      const data = await searchStations(kw)
      remoteOptions.value = (data || []).map(s => ({ value: s.stationName, label: stationLabel(s) }))
    } catch {
      remoteOptions.value = []
    } finally {
      stationLoading.value = false
    }
  }, 250)
}

/** 下拉关闭后恢复全量，下次打开还能看到全部车站 */
function onStationVisible(visible) {
  if (!visible) {
    remoteKeyword.value = ''
    remoteOptions.value = []
  }
}

/** 远程模式下只显示搜索结果，补上已选项避免回显成裸站名 */
const stationOptions = computed(() => {
  if (!remoteKeyword.value) {
    return baseOptions.value
  }
  const map = new Map(remoteOptions.value.map(o => [o.value, o]))
  ;[query.value.fromStation, query.value.toStation].forEach(v => {
    if (v && !map.has(v)) {
      map.set(v, { value: v, label: v })
    }
  })
  return Array.from(map.values())
})
const loading = ref(false)
const list = ref([])
const total = ref(0)
const fromCache = ref(false)
const errorMsg = ref('')

const query = ref({
  fromStation: '',
  toStation: '',
  departDate: dayjs.today(),
  pageNum: 1,
  pageSize: 10
})

onMounted(async () => {
  await stationStore.loadStations()
  if (stations.value.length > 0) {
    query.value.fromStation = stations.value[0].stationName
    query.value.toStation = stations.value[1] ? stations.value[1].stationName : ''
  }
  search(false)
})

function cacheKey() {
  return `trains:${query.value.fromStation}:${query.value.toStation}:${query.value.departDate}:${query.value.pageNum}:${query.value.pageSize}`
}

async function search(force) {
  loading.value = true
  errorMsg.value = ''
  try {
    if (!force) {
      const cached = localCache.get(cacheKey())
      if (cached) {
        list.value = cached.list
        total.value = cached.total
        fromCache.value = true
        return
      }
    }
    fromCache.value = false
    const data = await queryTrains(query.value)
    list.value = data.list || []
    total.value = data.total || 0
    // 前端缓存 30 秒，降低后端 QPS
    localCache.set(cacheKey(), { list: list.value, total: total.value }, 30)
  } catch (e) {
    // 展示「加载失败 + 重试」而不是只弹一条消息，弱网下用户可一键重试
    errorMsg.value = e.message || '查询车次失败'
  } finally {
    loading.value = false
  }
}

function onPageChange(page) {
  query.value.pageNum = page
  search(false)
}

// 只能选今天起 30 天内（与后端 ticket.query-days 一致）
function disabledDate(d) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const max = today.getTime() + 29 * 86400000
  return d.getTime() < today.getTime() || d.getTime() > max
}

function dateAfter(days) {
  const d = new Date()
  d.setDate(d.getDate() + days)
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

function pickTomorrow() {
  query.value.departDate = dateAfter(1)
  query.value.pageNum = 1
  search(false)
}

function goDetail(trainId) {
  // 带上查询条件：详情据此按「上车站 → 下车站」算区间余票与可选座位
  const params = new URLSearchParams()
  if (query.value.fromStation) params.set('from', query.value.fromStation)
  if (query.value.toStation) params.set('to', query.value.toStation)
  if (query.value.departDate) params.set('date', query.value.departDate)
  const qs = params.toString()
  router.push(qs ? `/trains/${trainId}?${qs}` : `/trains/${trainId}`)
}

// ---------- 时刻表（站点时序）弹窗 ----------
const stopsVisible = ref(false)
const stopsLoading = ref(false)
const stops = ref([])
const activeTrain = ref({ trainNo: '', fromStationName: '', toStationName: '', trainType: '' })

/** 点击车次号：打开弹窗并加载该车次时刻表 */
function openStops(row) {
  activeTrain.value = row
  stops.value = []
  stopsVisible.value = true
  loadStops()
}

async function loadStops() {
  stopsLoading.value = true
  try {
    stops.value = await fetchTrainStops(activeTrain.value.id) || []
  } catch (e) {
    ElMessage.error(e.message || '加载时刻表失败')
  } finally {
    stopsLoading.value = false
  }
}

/** LocalTime 序列化成 HH:mm:ss，展示取 HH:mm */
function fmtTime(t) {
  return t ? String(t).slice(0, 5) : '----'
}
</script>

<style scoped>
.stock-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 0;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}

.train-link {
  cursor: pointer;
}

.train-link:hover {
  text-decoration: underline;
}

/* ---------- 时刻表弹窗 ---------- */
.stop-current {
  color: #3a8ee6;
  font-weight: 600;
}

.stops-footer {
  margin-top: 12px;
  padding: 10px 12px;
  background: #f0f7ff;
  border: 1px solid #d4e7f7;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #303133;
}

.stops-train {
  color: #3a8ee6;
}
</style>
