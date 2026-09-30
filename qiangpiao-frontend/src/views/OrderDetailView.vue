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
          <el-button v-if="order.status === 0" type="primary" size="small" :loading="paying" @click="doPay">
            立即支付
          </el-button>
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
              <div class="log-title">
                <el-tag size="small" :type="timelineType(log.action)">{{ timelineTag(log.action) }}</el-tag>
                <span class="log-action">{{ log.actionText }}</span>
              </div>
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

    <!-- 支付中：第一阶段已发起支付，等待渠道异步回调 -->
    <el-dialog v-model="payVisible" title="等待支付结果" width="420px" :close-on-click-modal="false"
               @close="onPayDialogClose">
      <template v-if="payment">
        <el-descriptions :column="1" size="small" border>
          <el-descriptions-item label="支付单号">{{ payment.payNo }}</el-descriptions-item>
          <el-descriptions-item label="支付金额">¥{{ payment.amount }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ payment.payType }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="payTagType(payment.status)">{{ payment.statusText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="渠道交易号">{{ payment.tradeNo || '回调后生成' }}</el-descriptions-item>
        </el-descriptions>
        <div class="pay-tip">
          已发起支付，等待渠道回调（模拟渠道约 3 秒后回调）。回调验签 + 幂等通过后才会扣款并入账。
        </div>
      </template>
      <template #footer>
        <el-button size="small" @click="payVisible = false">稍后查看</el-button>
        <el-button size="small" type="danger" @click="mockResult('FAIL')">模拟支付失败</el-button>
        <el-button size="small" type="success" @click="mockResult('SUCCESS')">模拟支付成功</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchOrderDetail, payOrder, cancelOrder } from '@/api/order'
import { fetchPayment, mockPayCallback } from '@/api/payment'
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

// 两阶段支付：发起支付后拿 payNo 轮询支付单，回调成功才扣款
const paying = ref(false)
const payVisible = ref(false)
const payment = ref(null)
let payTimer = null
let pollTimes = 0

onMounted(load)
onBeforeUnmount(stopPolling)

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
  paying.value = true
  try {
    // 第一阶段：只发起支付，不扣款
    payment.value = await payOrder(order.value.orderNo)
    payVisible.value = true
    startPolling(payment.value.payNo)
  } catch (e) {
    await handlePayError(e)
  } finally {
    paying.value = false
  }
}

function startPolling(payNo) {
  stopPolling()
  pollTimes = 0
  payTimer = setInterval(async () => {
    pollTimes++
    try {
      const p = await fetchPayment(payNo)
      payment.value = p
      if (p.status !== 0) {
        stopPolling()
        payVisible.value = false
        if (p.status === 1) {
          ElMessage.success('支付成功')
        } else {
          ElMessage.warning(p.failReason || '支付未完成，可重新发起支付')
        }
        load()
      } else if (pollTimes >= 40) {
        stopPolling()
        ElMessage.info('支付结果确认中，请稍后刷新查看')
      }
    } catch (e) {
      stopPolling()
    }
  }, 1500)
}

function stopPolling() {
  if (payTimer) {
    clearInterval(payTimer)
    payTimer = null
  }
}

function onPayDialogClose() {
  // 关闭弹窗不停止等待：渠道回调仍会到达，刷新详情即可看到结果
  payment.value = null
}

async function mockResult(result) {
  if (!payment.value) return
  await mockPayCallback(payment.value.payNo, result)
  ElMessage.info(result === 'SUCCESS' ? '已模拟渠道成功回调' : '已模拟渠道失败回调')
}

async function handlePayError(e) {
  const msg = e?.message || ''
  if (msg.includes('余额不足')) {
    try {
      await ElMessageBox.confirm(msg + '，是否立即前往钱包充值？', '提示', {
        type: 'warning',
        confirmButtonText: '去充值',
        cancelButtonText: '稍后再说'
      })
      router.push('/wallet')
    } catch {
      // 用户取消
    }
  } else {
    ElMessage.error(msg || '发起支付失败')
  }
}

function payTagType(status) {
  if (status === 1) return 'success'
  if (status === 0) return 'warning'
  return 'danger'
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

/** 时间轴节点配色：成功绿 / 失败红 / 变更橙 / 进行中蓝 */
function timelineType(action) {
  if (action === 'PAY' || action === 'CREATE') return 'success'
  if (action === 'REFUND' || action === 'CANCEL' || action === 'EXPIRE'
    || action === 'PAY_FAIL' || action === 'SECKILL_FAIL') return 'danger'
  if (action === 'CHANGE' || action === 'PAY_ABNORMAL' || action === 'PAY_CLOSE') return 'warning'
  // PAY_CREATE / SECKILL_ACCEPT 属于进行中
  return 'primary'
}

/** 节点语义标签：让用户一眼看懂这一步是干嘛的 */
function timelineTag(action) {
  const map = {
    SECKILL_ACCEPT: '抢票受理',
    SECKILL_FAIL: '抢票失败',
    CREATE: '下单',
    PAY_CREATE: '发起支付',
    PAY: '支付',
    PAY_FAIL: '支付失败',
    PAY_ABNORMAL: '支付异常',
    PAY_CLOSE: '支付单关闭',
    CANCEL: '取消',
    EXPIRE: '超时',
    REFUND: '退票',
    CHANGE: '改签'
  }
  return map[action] || '流转'
}
</script>

<style scoped>
.detail-head { display: flex; align-items: center; justify-content: space-between; }
.page-title { margin: 0 0 12px; font-size: 18px; }
.op-bar { margin: 14px 0 4px; display: flex; gap: 10px; }
.log-card { padding: 6px 10px; }
.pay-tip { margin-top: 10px; font-size: 12px; color: #909399; line-height: 1.6; }
.log-title { font-weight: 600; }
.log-action { margin-left: 8px; }
.log-meta { font-size: 12px; margin-top: 2px; }
</style>
