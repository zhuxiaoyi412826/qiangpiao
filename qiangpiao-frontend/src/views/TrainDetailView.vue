<template>
  <div class="page-container">
    <SkeletonCard v-if="loading" :rows="6"/>
    <ErrorRetry v-else-if="errorMsg" :message="errorMsg" @retry="loadDetail"/>
    <template v-else>
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
      <!-- 车厢默认全部折叠（打开页面 / 切换席别都会重置），展开后座位网格布局与之前一致 -->
      <el-collapse v-model="activeCarriages">
        <el-collapse-item v-for="g in carriageGroups" :key="g.carriageNo" :name="String(g.carriageNo)">
          <template #title>
            <strong>{{ g.carriageNo }} 号车厢</strong>
            <el-tag size="small" :type="g.available > 0 ? 'success' : 'info'" style="margin-left: 8px">
              可选 {{ g.available }} / {{ g.total }}
            </el-tag>
            <el-tag v-if="selectedSeat && selectedSeat.carriageNo === g.carriageNo" size="small" type="warning"
                    style="margin-left: 8px">
              已选 {{ selectedSeat.seatNo }}
            </el-tag>
          </template>
          <div class="seat-grid">
            <div v-for="seat in g.seats" :key="seat.seatId"
                 class="seat-item"
                 :class="seatClass(seat)"
                 @click="toggleSeat(seat)">
              {{ seat.seatNo }}
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
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
      <el-alert v-if="blockReason" :title="blockReason" type="warning" :closable="false" show-icon
                style="max-width: 620px; margin-bottom: 12px"/>
      <el-button type="danger" size="large" :loading="seckilling" :disabled="!canSeckill" @click="submitSeckill">
        {{ seckillBtnText }}
      </el-button>
      <span class="muted" style="margin-left: 12px">
        未登录请先登录；每人每天每车次限购 1 张，已购车次到达（下车）后才能再次购票
      </span>
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
  </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchTrainDetail, fetchBuyBlock } from '@/api/train'
import SkeletonCard from '@/components/SkeletonCard.vue'
import ErrorRetry from '@/components/ErrorRetry.vue'
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

const blockReason = ref('')
const errorMsg = ref('')
const canSeckill = computed(() => userStore.isLogin && !!seatType.value && !seckilling.value && !blockReason.value)
const seckillBtnText = computed(() => (userStore.isLogin ? '立即抢票' : '请先登录'))

const currentSeatMap = computed(() => {
  if (!detail.value.seatMaps || !seatType.value) {
    return null
  }
  return detail.value.seatMaps.find(m => m.seatType === seatType.value) || null
})

/** 展开的车厢（空数组 = 全部折叠，默认折叠） */
const activeCarriages = ref([])

/** 按车厢号升序分组，供座位图渲染 */
const carriageGroups = computed(() => {
  if (!currentSeatMap.value) {
    return []
  }
  const map = new Map()
  currentSeatMap.value.seats.forEach(seat => {
    let group = map.get(seat.carriageNo)
    if (!group) {
      group = { carriageNo: seat.carriageNo, seats: [], total: 0, available: 0 }
      map.set(seat.carriageNo, group)
    }
    group.seats.push(seat)
    group.total++
    if (seat.status === 0) {
      group.available++
    }
  })
  return [...map.values()].sort((a, b) => a.carriageNo - b.carriageNo)
})

const selectedSeat = computed(() => {
  if (!selectedSeatId.value || !currentSeatMap.value) {
    return null
  }
  return currentSeatMap.value.seats.find(s => s.seatId === selectedSeatId.value) || null
})

const selectedSeatNo = computed(() => {
  const seat = selectedSeat.value
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
    // 每次打开页面 / 刷新余票都重置为全部折叠
    activeCarriages.value = []
    const first = (detail.value.stocks || []).find(s => s.availableCount > 0)
    if (!seatType.value && first) {
      seatType.value = first.seatType
    }
    // 购票资格预检：每人每天每车次 1 张 + 已购车次运行时间内不可重复购票（下车后方可再买）
    if (userStore.isLogin) {
      try {
        const block = await fetchBuyBlock(trainId)
        blockReason.value = block && block.canBuy === false ? (block.reason || '当前不可购买该车次') : ''
      } catch (e) {
        // 预检失败不阻断购票页展示，只是少了提前提示
        blockReason.value = ''
      }
    }
  } catch (e) {
    errorMsg.value = e.message || '加载车次详情失败'
  } finally {
    loading.value = false
  }
}

function onSeatTypeChange() {
  selectedSeatId.value = null
  result.value = null
  // 换席别后重新折叠所有车厢
  activeCarriages.value = []
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
    // 选了座位却失败（多半是座位刚被他人抢走）：清空选择并刷新座位图，方便立即重选
    if (selectedSeatId.value) {
      selectedSeatId.value = null
      loadDetail()
    }
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
      loadDetail()
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

/* 座位网格：在折叠面板内也显式声明，避免展开后布局退化成单列 */
.seat-grid {
  display: grid;
  grid-template-columns: repeat(6, 46px);
  gap: 8px;
}

.seat-item {
  height: 40px;
  line-height: 40px;
  text-align: center;
  border-radius: 6px;
  border: 1px solid #dcdfe6;
  cursor: pointer;
  font-size: 12px;
  user-select: none;
}

@media (max-width: 768px) {
  .seat-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 6px;
  }

  .seat-item {
    height: 44px;
    line-height: 44px;
  }
}
</style>
