<template>
  <div class="page-container">
    <div class="card-panel">
      <el-radio-group v-model="status" @change="loadOrders">
        <el-radio-button :value="null">全部</el-radio-button>
        <el-radio-button :value="0">待支付</el-radio-button>
        <el-radio-button :value="1">已支付</el-radio-button>
        <el-radio-button :value="2">已取消</el-radio-button>
        <el-radio-button :value="4">已超时</el-radio-button>
      </el-radio-group>
      <el-button style="margin-left: 12px" @click="loadOrders">刷新</el-button>
    </div>

    <div class="card-panel">
      <el-table :data="list" v-loading="loading" border>
        <el-table-column label="订单号" prop="orderNo" min-width="200"/>
        <el-table-column label="乘车日期" width="115">
          <template #default="{ row }">{{ fmtDate(row.departTime) }}</template>
        </el-table-column>
        <el-table-column label="车次" min-width="220">
          <template #default="{ row }">
            <div>{{ row.trainNo }} {{ row.fromStationName }} → {{ row.toStationName }}</div>
            <div class="muted">{{ fmtTime(row.departTime) }} 出发</div>
          </template>
        </el-table-column>
        <el-table-column label="席位" min-width="140">
          <template #default="{ row }">
            {{ row.seatTypeName }}
            <span v-if="row.carriageNo"> · {{ row.carriageNo }}车{{ row.seatNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="乘客" prop="passengerName" width="100"/>
        <el-table-column label="票价" width="100">
          <template #default="{ row }"><span class="price">¥{{ row.price }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付截止" prop="expireTime" min-width="180"/>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="primary" size="small" @click="pay(row)">支付</el-button>
            <el-button v-if="row.status === 0" type="danger" size="small" @click="cancel(row)">取消</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无订单"/>
        </template>
      </el-table>

      <el-pagination class="pager" background layout="total, prev, pager, next"
                     :current-page="pageNum" :page-size="pageSize" :total="total"
                     @current-change="onPageChange"/>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { queryOrders, payOrder, cancelOrder } from '@/api/order'

const router = useRouter()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const status = ref(null)

onMounted(loadOrders)

async function loadOrders() {
  loading.value = true
  try {
    const data = await queryOrders({
      status: status.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function onPageChange(page) {
  pageNum.value = page
  loadOrders()
}

function statusTag(s) {
  if (s === 1) return 'success'
  if (s === 0) return 'warning'
  return 'info'
}

// 出发时间兼容两种序列化格式：ISO 字符串 / [y,m,d,h,m] 数组
function toText(v) {
  if (!v) return '--'
  if (typeof v === 'string') return v.replace('T', ' ')
  if (Array.isArray(v)) {
    const [y, m, d, h = 0, mi = 0] = v
    const p = n => String(n).padStart(2, '0')
    return `${y}-${p(m)}-${p(d)} ${p(h)}:${p(mi)}`
  }
  return '--'
}

function fmtDate(v) {
  return toText(v) === '--' ? '--' : toText(v).slice(0, 10)
}

function fmtTime(v) {
  return toText(v) === '--' ? '--' : toText(v).slice(11, 16)
}

async function pay(row) {
  try {
    await payOrder(row.orderNo)
    ElMessage.success('支付成功')
    loadOrders()
  } catch (e) {
    // 余额不足时引导去钱包充值
    if (e.message && e.message.indexOf('余额不足') >= 0) {
      ElMessageBox.confirm(e.message + '，是否立即前往钱包充值？', '提示', {
        type: 'warning',
        confirmButtonText: '去充值',
        cancelButtonText: '稍后再说'
      }).then(() => router.push('/wallet')).catch(() => {
      })
    }
  }
}

async function cancel(row) {
  await ElMessageBox.confirm('取消后座位将释放，确定取消订单？', '提示', { type: 'warning' })
  await cancelOrder(row.orderNo)
  ElMessage.success('订单已取消')
  loadOrders()
}
</script>

<style scoped>
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
