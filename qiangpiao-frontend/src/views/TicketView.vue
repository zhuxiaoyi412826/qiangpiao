<template>
  <div class="page-container">
    <div class="card-panel">
      <el-radio-group v-model="type" size="large" @change="onTypeChange">
        <el-radio-button value="upcoming">未开车</el-radio-button>
        <el-radio-button value="history">历史车票</el-radio-button>
      </el-radio-group>
      <span class="muted type-tip">{{ typeTip }}</span>
    </div>

    <el-alert v-if="hint" :title="hint" type="info" :closable="false" class="hint" show-icon/>

    <div>
      <SkeletonCard v-if="loading" :rows="4" show-table/>
      <ErrorRetry v-else-if="errorMsg" :message="errorMsg" @retry="load"/>
      <template v-else>
      <div v-for="t in list" :key="t.id" class="ticket-card" :class="{ 'ticket-invalid': t.ticketStatus === 3 }">
        <div class="ticket-body">
          <div class="route">
            <div class="station">
              <div class="time">{{ t.departTime }}</div>
              <div class="name">{{ t.fromStationName }}</div>
            </div>
            <div class="middle">
              <div class="train">{{ t.trainNo }}<span class="train-type">{{ t.trainType }}</span></div>
              <div class="line"/>
              <div class="duration">{{ t.durationText }}</div>
            </div>
            <div class="station">
              <div class="time">{{ t.arriveTime }}</div>
              <div class="name">{{ t.toStationName }}</div>
            </div>
          </div>
          <div class="seat-row">
            <el-tag size="small" type="primary">{{ t.seatTypeName }}</el-tag>
            <span class="seat">{{ t.carriageNo }}车{{ t.seatNo }}</span>
            <span class="muted">乘客：{{ t.passengerName }}</span>
            <span class="muted">票号：{{ t.orderNo }}</span>
          </div>
        </div>

        <div class="ticket-stub">
          <div class="date">{{ t.departDate }}</div>
          <div class="price">¥{{ fmt(t.price) }}</div>
          <el-tag size="small" :type="statusTag(t.ticketStatus)">{{ t.ticketStatusText }}</el-tag>
          <div v-if="t.ticketStatus === 1" class="countdown">{{ countdown(t) }}</div>
          <div v-else-if="t.status !== 1" class="muted">{{ t.statusText }}</div>
        </div>
      </div>

      <el-empty v-if="!list.length && !loading" :description="emptyText"/>

      <el-pagination v-if="total > 0" class="pager" background layout="total, prev, pager, next"
                     :current-page="pageNum" :page-size="pageSize" :total="total"
                     @current-change="onPageChange"/>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { queryMyTickets } from '@/api/ticket'
import SkeletonCard from '@/components/SkeletonCard.vue'
import ErrorRetry from '@/components/ErrorRetry.vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const type = ref('upcoming')
const errorMsg = ref('')

const typeTip = computed(() => type.value === 'upcoming'
    ? '已支付且尚未发车的车票'
    : '已乘车的车票，以及已取消 / 退票 / 超时的记录')

const hint = computed(() => type.value === 'upcoming'
    ? '待支付的订单还不是车票，请在「我的订单」完成支付后到这里查看'
    : '')

const emptyText = computed(() => type.value === 'upcoming' ? '暂无待出行车票' : '暂无历史车票')

onMounted(load)

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await queryMyTickets({
      type: type.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    errorMsg.value = e.message || '加载车票失败'
  } finally {
    loading.value = false
  }
}

function onTypeChange() {
  pageNum.value = 1
  load()
}

function onPageChange(page) {
  pageNum.value = page
  load()
}

function statusTag(s) {
  return s === 1 ? 'success' : 'info'
}

function countdown(t) {
  const d = Number(t.daysFromNow || 0)
  if (d <= 0) return '今天发车'
  if (d === 1) return '明天发车'
  return `还有 ${d} 天发车`
}

function fmt(v) {
  return v === undefined || v === null ? '--' : Number(v).toFixed(2)
}
</script>

<style scoped>
.ticket-card {
  display: flex;
  align-items: stretch;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, .06);
  margin-bottom: 16px;
  overflow: hidden;
  border-left: 4px solid #1a73e8;
}

.ticket-invalid {
  border-left-color: #c0c4cc;
  opacity: .78;
}

.ticket-body {
  flex: 1;
  padding: 18px 20px;
}

.route {
  display: flex;
  align-items: center;
  gap: 20px;
}

.station .time {
  font-size: 24px;
  font-weight: 700;
  color: #1f2933;
}

.station .name {
  font-size: 14px;
  color: #52606d;
  margin-top: 2px;
}

.middle {
  flex: 1;
  text-align: center;
  min-width: 160px;
}

.middle .train {
  font-size: 14px;
  font-weight: 600;
  color: #1a73e8;
}

.train-type {
  font-size: 12px;
  color: #7b8794;
  margin-left: 6px;
}

.middle .line {
  height: 1px;
  background: linear-gradient(90deg, #cbd2d9, #9aa5b1);
  margin: 6px 0;
}

.middle .duration {
  font-size: 12px;
  color: #7b8794;
}

.seat-row {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}

.seat {
  font-weight: 600;
}

.ticket-stub {
  width: 150px;
  padding: 18px 16px;
  border-left: 1px dashed #cbd2d9;
  text-align: center;
  background: #fafbfc;
}

.ticket-stub .date {
  font-size: 13px;
  color: #52606d;
}

.ticket-stub .price {
  font-size: 22px;
  font-weight: 700;
  color: #e03131;
  margin: 6px 0 8px;
}

.ticket-stub .countdown {
  margin-top: 10px;
  font-size: 12px;
  color: #f2994a;
}

.type-tip {
  margin-left: 12px;
  font-size: 13px;
}

.hint {
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
}
</style>
