<template>
  <div class="page-container">
    <div class="card-panel">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item label="出发站">
          <el-select v-model="query.fromStation" filterable clearable placeholder="请选择" style="width: 150px">
            <el-option v-for="s in stations" :key="s.id" :label="s.stationName" :value="s.stationName"/>
          </el-select>
        </el-form-item>
        <el-form-item label="到达站">
          <el-select v-model="query.toStation" filterable clearable placeholder="请选择" style="width: 150px">
            <el-option v-for="s in stations" :key="s.id" :label="s.stationName" :value="s.stationName"/>
          </el-select>
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
      <el-table :data="list" v-loading="loading" border style="width: 100%">
        <el-table-column label="车次" width="130">
          <template #default="{ row }">
            <span class="train-no">{{ row.trainNo }}</span>
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
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { queryTrains } from '@/api/train'
import { useStationStore } from '@/store/station'
import { localCache } from '@/utils/cache'
import dayjs from '@/utils/dayjs'

const router = useRouter()
const stationStore = useStationStore()

const stations = computed(() => stationStore.stations)
const loading = ref(false)
const list = ref([])
const total = ref(0)
const fromCache = ref(false)

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
    ElMessage.error(e.message)
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
  router.push(`/trains/${trainId}`)
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
</style>
