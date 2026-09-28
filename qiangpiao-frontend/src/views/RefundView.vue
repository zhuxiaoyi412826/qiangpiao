<template>
  <div class="page-container">
    <div class="card-panel">
      <h3 class="page-title">退票 / 改签 / 售后</h3>
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="订单号">
          <el-input v-model="orderNo" placeholder="输入订单号，如 QP20260927..." clearable style="width: 320px"/>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="search">查询</el-button>
        </el-form-item>
      </el-form>
      <p class="muted">
        退票规则：仅限已支付且尚未发车的订单，票款原路退回钱包；改签需改到未发车且有余票的车次，差额多退少补。
      </p>
    </div>

    <SkeletonCard v-if="loading" :rows="4"/>
    <ErrorRetry v-else-if="errorMsg" :message="errorMsg" @retry="search"/>

    <div v-if="order" class="card-panel">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small" :type="order.status === 1 ? 'success' : 'info'">{{ order.statusText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="车次">
          {{ order.trainNo }} {{ order.fromStationName }} → {{ order.toStationName }}
        </el-descriptions-item>
        <el-descriptions-item label="席位">
          {{ order.seatTypeName }} {{ order.carriageNo }}车{{ order.seatNo }}座
        </el-descriptions-item>
        <el-descriptions-item label="乘客">{{ order.passengerName }}</el-descriptions-item>
        <el-descriptions-item label="票价">¥{{ order.price }}</el-descriptions-item>
        <el-descriptions-item label="出发时间">{{ fmt(order.departTime) }}</el-descriptions-item>
        <el-descriptions-item label="支付截止">{{ fmt(order.expireTime) }}</el-descriptions-item>
      </el-descriptions>

      <div class="op-bar">
        <el-button type="danger" :disabled="order.status !== 1" @click="doRefund">申请退票</el-button>
        <el-button type="warning" :disabled="order.status !== 1" @click="openChange">申请改签</el-button>
      </div>

      <el-divider content-position="left">流转时间轴</el-divider>
      <el-timeline>
        <el-timeline-item v-for="log in logs" :key="log.id" :timestamp="log.createTime"
                          :type="timelineType(log.action)" placement="top">
          <div class="log-title">{{ log.actionText }}</div>
          <div class="muted">{{ log.detail }}</div>
        </el-timeline-item>
        <el-timeline-item v-if="!logs.length" timestamp="暂无记录" type="info">订单暂无流转记录</el-timeline-item>
      </el-timeline>

      <template v-if="changes.length">
        <el-divider content-position="left">改签历史</el-divider>
        <el-table :data="changes" border size="small">
          <el-table-column prop="oldTrainId" label="原车次" width="100"/>
          <el-table-column prop="newTrainId" label="新车次" width="100"/>
          <el-table-column prop="oldSeatNo" label="原座位" width="110"/>
          <el-table-column prop="newSeatNo" label="新座位" width="110"/>
          <el-table-column prop="diffAmount" label="差额" width="90"/>
          <el-table-column prop="createTime" label="改签时间" width="180"/>
        </el-table>
      </template>
    </div>

    <!-- 改签 -->
    <el-dialog v-model="changeVisible" title="选择改签车次" width="760px">
      <el-form :inline="true" size="small" @submit.prevent>
        <el-form-item label="出发站">
          <el-select-v2 v-model="tq.fromStation" :options="stationOptions" filterable
                        placeholder="输入或选择" style="width: 180px"/>
        </el-form-item>
        <el-form-item label="到达站">
          <el-select-v2 v-model="tq.toStation" :options="stationOptions" filterable
                        placeholder="输入或选择" style="width: 180px"/>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="tq.departDate" type="date" value-format="YYYY-MM-DD" style="width: 150px"/>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTrains">查询车次</el-button>
        </el-form-item>
      </el-form>
      <ErrorRetry v-if="trainError" :message="trainError" :loading="trainLoading" @retry="loadTrains"/>
      <el-table v-else :data="trains" border size="small" v-loading="trainLoading">
        <el-table-column prop="trainNo" label="车次" width="100"/>
        <el-table-column label="区间" min-width="180">
          <template #default="{ row }">{{ row.fromStationName }} → {{ row.toStationName }}</template>
        </el-table-column>
        <el-table-column prop="departDate" label="日期" width="120"/>
        <el-table-column prop="departTime" label="发车" width="90"/>
        <el-table-column label="席位 / 余票" min-width="240">
          <template #default="{ row }">
            <el-button v-for="s in row.stocks" :key="s.seatType" size="small" class="seat-btn"
                       :type="target && target.trainId === row.id && target.seatType === s.seatType ? 'primary' : 'default'"
                       :disabled="s.availableCount <= 0 || !!row.sellTip"
                       @click="target = { trainId: row.id, seatType: s.seatType, trainNo: row.trainNo }">
              {{ s.seatTypeName }} ¥{{ s.price }} · 余{{ s.availableCount }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <span class="muted" style="float: left">
          {{ target ? `已选：${target.trainNo} ${seatName(target.seatType)}` : '请选择目标席位' }}
        </span>
        <el-button @click="changeVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!target" @click="submitChange">确认改签</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchOrderDetail } from '@/api/order'
import { fetchTimeline, fetchChanges, refundOrder, changeOrder } from '@/api/aftersale'
import { queryTrains } from '@/api/train'
import { useStationStore } from '@/store/station'
import SkeletonCard from '@/components/SkeletonCard.vue'
import ErrorRetry from '@/components/ErrorRetry.vue'
import dayjs from '@/utils/dayjs'

const route = useRoute()
const stationStore = useStationStore()
const stations = ref([])
/** 供 el-select-v2 使用（虚拟滚动，避免渲染上千 el-option） */
const stationOptions = computed(() =>
  stations.value.map(s => ({ value: s.stationName, label: s.stationName }))
)

const orderNo = ref(route.query.orderNo || '')
const order = ref(null)
const logs = ref([])
const changes = ref([])
const loading = ref(false)
const errorMsg = ref('')

const changeVisible = ref(false)
const trains = ref([])
const trainLoading = ref(false)
const trainError = ref('')
const target = ref(null)
const tq = ref({ fromStation: '', toStation: '', departDate: dayjs.today(), pageNum: 1, pageSize: 10 })

onMounted(async () => {
  await stationStore.loadStations()
  stations.value = stationStore.stations
  if (orderNo.value) await search()
})

async function search() {
  if (!orderNo.value) return ElMessage.warning('请输入订单号')
  loading.value = true
  errorMsg.value = ''
  try {
    order.value = await fetchOrderDetail(orderNo.value.trim())
    logs.value = await fetchTimeline(orderNo.value.trim())
    changes.value = await fetchChanges(orderNo.value.trim())
  } catch (e) {
    errorMsg.value = e.message || '查询订单失败'
    order.value = null
  } finally {
    loading.value = false
  }
}

async function doRefund() {
  await ElMessageBox.prompt('请输入退票原因（可留空）', '退票确认', {
    inputPlaceholder: '如：行程有变',
    type: 'warning'
  })
  await refundOrder(order.value.orderNo, '')
  ElMessage.success('退票成功，票款已退回钱包')
  search()
}

async function openChange() {
  target.value = null
  trains.value = []
  if (order.value) {
    tq.value.fromStation = order.value.fromStationName
    tq.value.toStation = order.value.toStationName
  }
  changeVisible.value = true
  loadTrains()
}

async function loadTrains() {
  trainLoading.value = true
  trainError.value = ''
  try {
    const data = await queryTrains(tq.value)
    trains.value = (data.list || []).filter(t => t.id !== (order.value && order.value.trainId))
  } catch (e) {
    trainError.value = e.message || '加载可改签车次失败'
  } finally {
    trainLoading.value = false
  }
}

async function submitChange() {
  if (!target.value) return
  await ElMessageBox.confirm(
      `确认将订单 ${order.value.orderNo} 改签至 ${target.value.trainNo}？差额将多退少补。`,
      '改签确认', { type: 'warning' })
  await changeOrder(order.value.orderNo, target.value.trainId, target.value.seatType, '用户自助改签')
  ElMessage.success('改签成功')
  changeVisible.value = false
  search()
}

function seatName(t) {
  return { 1: '商务座', 2: '一等座', 3: '二等座', 4: '软卧', 5: '硬卧' }[t] || ''
}

function fmt(v) {
  if (!v) return '-'
  if (typeof v === 'string') return v.replace('T', ' ').slice(0, 16)
  if (Array.isArray(v)) {
    const [y, m, d, h = 0, mi = 0] = v
    const p = n => String(n).padStart(2, '0')
    return `${y}-${p(m)}-${p(d)} ${p(h)}:${p(mi)}`
  }
  return '-'
}

function timelineType(action) {
  if (action === 'PAY') return 'success'
  if (action === 'REFUND' || action === 'CANCEL' || action === 'EXPIRE') return 'danger'
  if (action === 'CHANGE') return 'warning'
  return 'primary'
}
</script>

<style scoped>
.page-title { margin: 0 0 12px; font-size: 18px; }
.op-bar { margin: 14px 0 4px; display: flex; gap: 10px; }
.log-title { font-weight: 600; }
.seat-btn { margin-right: 6px; margin-bottom: 4px; }
</style>
