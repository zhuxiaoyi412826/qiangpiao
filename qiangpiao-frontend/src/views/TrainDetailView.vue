<template>
  <div class="page-container" v-loading="loading">
    <div class="card-panel">
      <div class="train-header">
        <div>
          <span class="train-no">{{ detail.trainNo }}</span>
          <el-tag size="small" style="margin-left: 8px">{{ detail.trainType }}</el-tag>
        </div>
        <div class="route">
          <strong>{{ detail.fromStationName }}</strong>
          <span class="muted"> —— {{ detail.durationText }} —— </span>
          <strong>{{ detail.toStationName }}</strong>
        </div>
        <div class="muted">{{ detail.departDate }} {{ detail.departTime }} 开 / {{ detail.arriveTime }} 到</div>
        <el-button size="small" @click="loadDetail(true)">刷新余票</el-button>
      </div>
    </div>

    <div class="card-panel">
      <h3>1. 选择席别</h3>
      <el-radio-group v-model="seatType" @change="onSeatTypeChange">
        <el-radio-button v-for="s in detail.stocks" :key="s.seatType" :value="s.seatType" :disabled="s.availableCount <= 0">
          {{ s.seatTypeName }} ¥{{ s.price }}（余 {{ s.availableCount }}）
        </el-radio-button>
      </el-radio-group>
    </div>

    <div class="card-panel" v-if="currentSeatMap">
      <h3>2. 选择座位<span class="muted">（不选则由系统自动分配；灰色为已售）</span></h3>
      <div v-for="(group, carriage) in groupedSeats" :key="carriage" class="carriage">
        <div class="muted">{{ carriage }} 号车厢</div>
        <div class="seat-grid">
          <div v-for="seat in group" :key="seat.seatId"
               class="seat-item"
               :class="seatClass(seat)"
               @click="toggleSeat(seat)">
            {{ seat.seatNo }}
          </div>
        </div>
      </div>
      <div class="muted" style="margin-top: 8px">
        已选座位：{{ selectedSeatNo || '未选择（系统自动分配）' }}
        <el-button link type="primary" v-if="selectedSeatId" @click="clearSeat">取消选择</el-button>
      </div>
    </div>

    <div class="card-panel">
      <h3>3. 乘客信息</h3>
      <el-form :model="passenger" label-width="90px" style="max-width: 460px">
        <el-form-item label="乘客姓名">
          <el-input v-model="passenger.passengerName" placeholder="请输入乘客姓名"/>
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="passenger.idCard" maxlength="18" placeholder="请输入身份证号"/>
        </el-form-item>
      </el-form>
      <el-button type="danger" size="large" :loading="seckilling" :disabled="!canSeckill" @click="submitSeckill">
        {{ seckillBtnText }}
      </el-button>
      <span class="muted" style="margin-left: 12px">未登录请先登录，每人每车次每席别限购 1 张</span>
    </div>

    <div class="card-panel" v-if="result">
      <h3>抢票结果</h3>
      <el-alert :title="resultText" :type="resultType" :closable="false" show-icon/>
      <div v-if="result.status === 1" style="margin-top: 12px">
        <p>订单号：{{ result.orderNo }}</p>
        <p>座位：{{ result.carriageNo }} 车 {{ result.seatNo }}</p>
        <el-button type="primary" @click="$router.push('/orders')">去支付</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchTrainDetail } from '@/api/train'
import { doSeckill, fetchSeckillResult } from '@/api/seckill'
import { useUserStore } from '@/store/user'

const route = useRoute()
const userStore = useUserStore()

const trainId = Number(route.params.trainId)
const loading = ref(false)
const seckilling = ref(false)
const detail = ref({})
const seatType = ref(null)
const selectedSeatId = ref(null)
const result = ref(null)
const polling = ref(false)

const passenger = ref({
  passengerName: (userStore.userInfo && userStore.userInfo.realName) || '',
  idCard: ''
})

const canSeckill = computed(() => userStore.isLogin && !!seatType.value && !seckilling.value)
const seckillBtnText = computed(() => (userStore.isLogin ? '立即抢票' : '请先登录'))

const currentSeatMap = computed(() => {
  if (!detail.value.seatMaps || !seatType.value) {
    return null
  }
  return detail.value.seatMaps.find(m => m.seatType === seatType.value) || null
})

const groupedSeats = computed(() => {
  if (!currentSeatMap.value) {
    return {}
  }
  const groups = {}
  currentSeatMap.value.seats.forEach(seat => {
    const key = seat.carriageNo
    if (!groups[key]) {
      groups[key] = []
    }
    groups[key].push(seat)
  })
  return groups
})

const selectedSeatNo = computed(() => {
  if (!selectedSeatId.value || !currentSeatMap.value) {
    return ''
  }
  const seat = currentSeatMap.value.seats.find(s => s.seatId === selectedSeatId.value)
  return seat ? `${seat.carriageNo}车${seat.seatNo}` : ''
})

const resultText = computed(() => {
  if (!result.value) {
    return ''
  }
  if (result.value.status === 1) {
    return '恭喜，抢票成功！'
  }
  if (result.value.status === 0) {
    return '排队中，系统正在为您锁定座位…'
  }
  return result.value.message || '抢票失败，请重试'
})

const resultType = computed(() => {
  if (!result.value) {
    return 'info'
  }
  return result.value.status === 1 ? 'success' : (result.value.status === 0 ? 'info' : 'error')
})

onMounted(() => loadDetail(false))

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await fetchTrainDetail(trainId)
    const first = (detail.value.stocks || []).find(s => s.availableCount > 0)
    if (!seatType.value && first) {
      seatType.value = first.seatType
    }
  } finally {
    loading.value = false
  }
}

function onSeatTypeChange() {
  selectedSeatId.value = null
  result.value = null
}

function seatClass(seat) {
  if (seat.seatId === selectedSeatId.value) {
    return 'selected'
  }
  return seat.status === 0 ? 'available' : 'sold'
}

function toggleSeat(seat) {
  if (seat.status !== 0) {
    ElMessage.warning('该座位已售出')
    return
  }
  selectedSeatId.value = seat.seatId === selectedSeatId.value ? null : seat.seatId
}

function clearSeat() {
  selectedSeatId.value = null
}

async function submitSeckill() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return
  }
  if (!passenger.value.passengerName || !passenger.value.idCard) {
    ElMessage.warning('请填写乘客姓名与身份证号')
    return
  }
  seckilling.value = true
  result.value = null
  try {
    const vo = await doSeckill({
      trainId,
      seatType: seatType.value,
      seatId: selectedSeatId.value,
      passengerName: passenger.value.passengerName,
      idCard: passenger.value.idCard
    })
    ElMessage.success('抢票请求已提交，正在为您锁定座位…')
    result.value = { status: 0, orderNo: vo.orderNo, message: '排队中' }
    pollResult()
  } catch (e) {
    result.value = { status: -1, message: e.message }
  } finally {
    seckilling.value = false
  }
}

function pollResult() {
  if (polling.value) {
    return
  }
  polling.value = true
  let times = 0
  const timer = setInterval(async () => {
    times++
    try {
      const res = await fetchSeckillResult(trainId, seatType.value)
      result.value = res
      if (res.status !== 0 || times >= 20) {
        clearInterval(timer)
        polling.value = false
        if (res.status === 1) {
          ElMessage.success('抢票成功')
          loadDetail(false)
        }
      }
    } catch (e) {
      // 查询异常也要给出明确失败态，避免页面一直卡在“排队中”
      clearInterval(timer)
      polling.value = false
      result.value = { status: -1, message: e.message || '查询抢票结果失败，请稍后重试' }
    }
  }, 1000)
}
</script>

<style scoped>
.train-header {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.route {
  font-size: 16px;
}

.carriage {
  margin-bottom: 14px;
}
</style>
