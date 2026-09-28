<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="detail-head">
        <h3 class="page-title">订单详情</h3>
        <el-button size="small" @click="$router.push('/orders')">返回订单列表</el-button>
      </div>

      <el-skeleton v-if="loading" :rows="4" animated/>
      <ErrorRetry v-else-if="errorMsg" :message="errorMsg" @retry="load"/>

      <template v-if="!loading && order">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="order.status === 1 ? 'success' : 'info'">{{ order.statusText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="车次">
            {{ order.trainNo }} {{ order.fromStationName }} → {{ order.toStationName }}
          </el-descriptions-item>
          <el-descriptions-item label="出发时间">{{ fmt(order.departTime) }}</el-descriptions-item>
          <el-descriptions-item label="席位">
            {{ order.seatTypeName }} {{ order.carriageNo }}车{{ order.seatNo }}座
          </el-descriptions-item>
          <el-descriptions-item label="乘客">{{ order.passengerName }}</el-descriptions-item>
          <el-descriptions-item label="票价">¥{{ order.price }}</el-descriptions-item>
          <el-descriptions-item label="支付截止">{{ fmt(order.expireTime) }}</el-descriptions-item>
        </el-descriptions>

        <div class="op-bar">
          <el-button v-if="order.status === 0" type="primary" size="small" @click="doPay">立即支付</el-button>
          <el-button v-if="order.status === 0" type="danger" size="small" @click="doCancel">取消订单</el-button>
          <el-button v-if="order.status === 1" type="warning" size="small"
                     @click="$router.push(`/refund?orderNo=${order.orderNo}`)">退票 / 改签
          </el-button>
        </div>

        <el-divider content-position="left">物流式时间轴</el-divider>
        <el-timeline>
          <el-timeline-item
              v-for="log in logs"
              :key="log.id"
              :timestamp="log.createTime"
              :type="timelineType(log.action)"
              :hollow="false"
              placement="top">
            <el-card shadow="never" class="log-card">
              <div class="log-title">{{ log.actionText }}</div>
              <div class="muted">{{ log.detail }}</div>
              <div class="muted log-meta">操作人 {{ log.operator }} · traceId {{ log.traceId }}</div>
            </el-card>
          </el-timeline-item>
          <el-timeline-item v-if="!logs.length" timestamp="暂无记录" type="info">
            该订单暂无流转记录（老订单在接入日志后产生的新订单才会有）
          </el-timeline-item>
        </el-timeline>

        <template v-if="changes.length">
          <el-divider content-position="left">改签历史</el-divider>
          <el-table :data="changes" border size="small">
            <el-table-column prop="oldTrainId" label="原车次" width="90"/>
            <el-table-column prop="newTrainId" label="新车次" width="90"/>
            <el-table-column prop="oldSeatNo" label="原座位" width="100"/>
            <el-table-column prop="newSeatNo" label="新座位" width="100"/>
            <el-table-column prop="diffAmount" label="差额" width="80"/>
            <el-table-column prop="createTime" label="时间" width="180"/>
          </el-table>
        </template>
      </template>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchOrderDetail, payOrder, cancelOrder } from '@/api/order'
import { fetchTimeline, fetchChanges } from '@/api/aftersale'
import ErrorRetry from '@/components/ErrorRetry.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const order = ref(null)
const logs = ref([])
const changes = ref([])
const orderNo = route.params.orderNo
const errorMsg = ref('')

onMounted(load)

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    order.value = await fetchOrderDetail(orderNo)
    logs.value = await fetchTimeline(orderNo)
    changes.value = await fetchChanges(orderNo)
  } catch (e) {
    errorMsg.value = e.message || '加载订单详情失败'
  } finally {
    loading.value = false
  }
}

async function doPay() {
  await payOrder(order.value.orderNo)
  ElMessage.success('支付成功')
  load()
}

async function doCancel() {
  await ElMessageBox.confirm('取消后座位将释放，确定取消订单？', '提示', { type: 'warning' })
  await cancelOrder(order.value.orderNo)
  ElMessage.success('订单已取消')
  load()
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
.detail-head { display: flex; align-items: center; justify-content: space-between; }
.page-title { margin: 0 0 12px; font-size: 18px; }
.op-bar { margin: 14px 0 4px; display: flex; gap: 10px; }
.log-card { padding: 6px 10px; }
.log-title { font-weight: 600; }
.log-meta { font-size: 12px; margin-top: 2px; }
</style>
