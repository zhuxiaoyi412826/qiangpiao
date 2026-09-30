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
      <h3>2. 选择座位<span class="muted">（可多选，最多与乘客数一致；不选则由系统自动分配；灰色为已售）</span></h3>
      <!-- 车厢默认全部折叠（打开页面 / 切换席别都会重置），展开后座位网格布局与之前一致 -->
      <el-collapse v-model="activeCarriages">
        <el-collapse-item v-for="g in carriageGroups" :key="g.carriageNo" :name="String(g.carriageNo)">
          <template #title>
            <strong>{{ g.carriageNo }} 号车厢</strong>
            <el-tag size="small" :type="g.available > 0 ? 'success' : 'info'" style="margin-left: 8px">
              可选 {{ g.available }} / {{ g.total }}
            </el-tag>
            <el-tag v-if="seatsInCarriage(g.carriageNo).length" size="small" type="warning"
                    style="margin-left: 8px">
              已选 {{ seatsInCarriage(g.carriageNo).map(s => s.seatNo).join('、') }}
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
        已选座位：{{ selectedSeatText || '未选择（系统自动分配）' }}
        <el-button link type="primary" v-if="selectedSeatIds.length" @click="clearSeat">清空选择</el-button>
      </div>
    </div>

    <div class="card-panel">
      <h3>3. 乘客信息<span class="muted">（可多选，一次最多 {{ TICKET_MAX }} 位，每人一张票）</span></h3>

      <!-- 本次购票的乘客（常用乘车人 + 临时填写） -->
      <div class="passenger-box">
        <div class="passenger-head">
          <span>本次乘客（{{ ticketCount }}/{{ TICKET_MAX }}）</span>
          <el-button link type="primary" :disabled="ticketCount >= TICKET_MAX" @click="openTempPassenger">
            {{ ticketCount >= TICKET_MAX ? '已达上限' : '+ 添加其他乘客' }}
          </el-button>
        </div>
        <div v-if="!ticketCount" class="muted">
          从下方常用乘车人中勾选，或点击「添加其他乘客」手动填写
        </div>
        <div v-else class="passenger-list">
          <div v-for="t in ticketList" :key="t.key" class="passenger-card active">
            <div class="p-name">
              {{ t.name }}
              <el-tag size="small" type="info">{{ t.source }}</el-tag>
            </div>
            <div class="p-no">{{ t.idCardMasked }}</div>
            <el-button link type="danger" size="small" class="p-del" @click.stop="removeTicket(t)">
              移除
            </el-button>
          </div>
        </div>
      </div>

      <!-- 常用乘车人：可多选，选中后下单只回传 id，证件号不出前端 -->
      <div class="passenger-box">
        <div class="passenger-head">
          <span>常用乘车人（{{ passengers.length }}/{{ PASSENGER_MAX }}）</span>
          <el-button v-if="userStore.isLogin" link type="primary"
                     :disabled="passengers.length >= PASSENGER_MAX" @click="openAddPassenger">
            {{ passengers.length >= PASSENGER_MAX ? '已达上限' : '+ 添加常用乘车人' }}
          </el-button>
          <span v-else class="muted">登录后可保存常用乘车人</span>
        </div>
        <div v-if="userStore.isLogin && !passengers.length" class="muted">
          暂无常用乘车人，添加后购票可一键勾选
        </div>
        <div v-else class="passenger-list">
          <div v-for="p in passengers" :key="p.id"
               class="passenger-card" :class="{ active: selectedPassengerIds.includes(p.id) }"
               @click="togglePassenger(p)">
            <div class="p-name">
              {{ p.passengerName }}
              <el-tag size="small" type="info">{{ p.passengerTypeText }}</el-tag>
            </div>
            <div class="p-no">{{ p.idCardMasked }}</div>
            <el-button link type="danger" size="small" class="p-del" @click.stop="removePassenger(p)">
              删除
            </el-button>
          </div>
        </div>
      </div>

      <el-alert v-if="blockReason" :title="blockReason" type="warning" :closable="false" show-icon
                style="max-width: 620px; margin-bottom: 12px"/>
      <el-button type="danger" size="large" :loading="seckilling" :disabled="!canSeckill" @click="submitSeckill">
        {{ seckillBtnText }}
      </el-button>
      <CaptchaDialog ref="captchaRef" @ok="onCaptchaOk"/>
      <span class="muted" style="margin-left: 12px">
        未登录请先登录；同一车次最多购买 {{ TICKET_MAX }} 张（需为不同乘车人），已购车次到达（下车）后才能再次购票
      </span>
    </div>

    <div class="card-panel" v-if="batchResult">
      <h3>抢票结果（{{ batchResult.count }} 张）</h3>
      <el-alert :title="batchResult.message" :type="batchResultType" :closable="false" show-icon/>
      <el-table v-if="batchResult.items.length" :data="batchResult.items" size="small" border
                style="max-width: 620px; margin-top: 12px">
        <el-table-column prop="passengerName" label="乘客" width="110"/>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : (row.status === 0 ? 'info' : 'danger')">
              {{ row.status === 1 ? '成功' : (row.status === 0 ? '排队中' : '失败') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="座位 / 原因">
          <template #default="{ row }">
            {{ row.carriageNo ? `${row.carriageNo} 车 ${row.seatNo}` : (row.message || '—') }}
          </template>
        </el-table-column>
      </el-table>
      <el-button v-if="batchResult.status === 1 || batchResult.status === 2" type="primary"
                 style="margin-top: 12px" @click="$router.push('/orders')">去支付</el-button>
    </div>

    <el-dialog v-model="passengerDialog" title="添加常用乘车人" width="440px">
      <el-form :model="passengerForm" label-width="90px">
        <el-form-item label="姓名">
          <el-input v-model="passengerForm.passengerName" placeholder="请输入乘客姓名"/>
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="passengerForm.idCard" maxlength="18" placeholder="请输入身份证号"/>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="passengerForm.phone" maxlength="11" placeholder="选填"/>
        </el-form-item>
        <el-form-item label="乘客类型">
          <el-select v-model="passengerForm.passengerType" style="width: 100%">
            <el-option label="成人" :value="1"/>
            <el-option label="儿童" :value="2"/>
            <el-option label="学生" :value="3"/>
            <el-option label="残军" :value="4"/>
          </el-select>
        </el-form-item>
      </el-form>
      <div class="muted">身份证号与手机号加密存储，列表仅展示脱敏值</div>
      <div class="muted warn-tip">
        开发测试白名单：身份证 111111 开头、手机号 111 开头可跳过格式校验（上线前关闭）
      </div>
      <template #footer>
        <el-button @click="passengerDialog = false">取消</el-button>
        <el-button type="primary" :loading="passengerSaving" @click="submitPassenger">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="tempDialog" title="添加其他乘客" width="440px">
      <el-form :model="tempForm" label-width="90px">
        <el-form-item label="姓名">
          <el-input v-model="tempForm.passengerName" placeholder="请输入乘客姓名"/>
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="tempForm.idCard" maxlength="18" placeholder="请输入身份证号"/>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="tempForm.phone" maxlength="11" placeholder="请输入手机号"/>
        </el-form-item>
        <el-form-item label="乘客类型">
          <el-select v-model="tempForm.passengerType" style="width: 100%">
            <el-option label="成人" :value="1"/>
            <el-option label="儿童" :value="2"/>
            <el-option label="学生" :value="3"/>
            <el-option label="残军" :value="4"/>
          </el-select>
        </el-form-item>
      </el-form>
      <div class="muted">仅用于本次购票，不保存为常用乘车人</div>
      <div class="muted warn-tip">
        开发测试白名单：身份证 111111 开头、手机号 111 开头可跳过格式校验（上线前关闭）
      </div>
      <template #footer>
        <el-button @click="tempDialog = false">取消</el-button>
        <el-button type="primary" @click="submitTempPassenger">确定</el-button>
      </template>
    </el-dialog>
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
import { doSeckill, fetchBatchSeckillResult } from '@/api/seckill'
import CaptchaDialog from '@/components/CaptchaDialog.vue'
import { fetchPassengers, addPassenger, deletePassenger } from '@/api/passenger'
import { validPhone, validIdCard } from '@/utils/validators'
import { useUserStore } from '@/store/user'
import { localCache } from '@/utils/cache'

/** 常用乘车人上限（与后端 PassengerService.MAX_COUNT 保持一致） */
const PASSENGER_MAX = 9
/** 一次最多购票张数（与后端 PurchaseLimitService.MAX_TICKETS_PER_TRAIN 保持一致） */
const TICKET_MAX = 9

const route = useRoute()
const userStore = useUserStore()

const trainId = Number(route.params.trainId)
const loading = ref(false)
const seckilling = ref(false)
const detail = ref({})
const seatType = ref(null)
/** 已选座位（可多选，最多与乘客数一致） */
const selectedSeatIds = ref([])
const batchResult = ref(null)
const polling = ref(false)

/** 常用乘车人列表（身份证脱敏） + 本次勾选的乘车人 id */
const passengers = ref([])
const selectedPassengerIds = ref([])
/** 临时乘客（未存为常用乘车人）：{ passengerName, idCard } */
const tempPassengers = ref([])
const passengerDialog = ref(false)
const passengerSaving = ref(false)
const passengerForm = ref({ passengerName: '', idCard: '', phone: '', passengerType: 1 })
/** 临时乘客弹窗 */
const tempDialog = ref(false)
const tempForm = ref({ passengerName: '', idCard: '', phone: '', passengerType: 1 })

/** 本次购票乘客（常用 + 临时），一人一张票 */
const ticketList = computed(() => {
  const list = []
  passengers.value.forEach(p => {
    if (selectedPassengerIds.value.includes(p.id)) {
      list.push({
        key: 'P' + p.id,
        passengerId: p.id,
        name: p.passengerName,
        idCardMasked: p.idCardMasked,
        source: '常用'
      })
    }
  })
  tempPassengers.value.forEach((p, i) => {
    list.push({
      key: 'T' + i,
      passengerName: p.passengerName,
      idCard: p.idCard,
      phone: p.phone,
      passengerType: p.passengerType,
      name: p.passengerName,
      idCardMasked: maskIdCard(p.idCard),
      source: '临时'
    })
  })
  return list
})
const ticketCount = computed(() => ticketList.value.length)

const blockReason = ref('')
const errorMsg = ref('')
/** 抢票前置人机验证弹窗 */
const captchaRef = ref(null)
const canSeckill = computed(() => userStore.isLogin && !!seatType.value && !seckilling.value
  && !blockReason.value && ticketCount.value > 0)
const seckillBtnText = computed(() => (userStore.isLogin ? '立即抢票' : '请先登录'))

/** 前端脱敏展示（后端列表只给脱敏值，临时填写的在这里本地打码） */
function maskIdCard(idCard) {
  if (!idCard || idCard.length < 8) {
    return idCard
  }
  return idCard.substring(0, 4) + '********' + idCard.substring(idCard.length - 4)
}

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

/** 某车厢内已选座位（供折叠面板标题展示） */
function seatsInCarriage(carriageNo) {
  if (!currentSeatMap.value) {
    return []
  }
  return currentSeatMap.value.seats.filter(
    s => s.carriageNo === carriageNo && selectedSeatIds.value.includes(s.seatId))
}

const selectedSeatText = computed(() => {
  if (!selectedSeatIds.value.length || !currentSeatMap.value) {
    return ''
  }
  return currentSeatMap.value.seats
    .filter(s => selectedSeatIds.value.includes(s.seatId))
    .map(s => `${s.carriageNo}车${s.seatNo}`)
    .join('、')
})

const batchResultType = computed(() => {
  if (!batchResult.value) {
    return 'info'
  }
  const s = batchResult.value.status
  return s === 1 ? 'success' : (s === 0 ? 'info' : (s === 2 ? 'warning' : 'error'))
})

onMounted(() => {
  loadDetail(false)
  if (userStore.isLogin) {
    loadPassengers()
  }
})

/** 乘车区间：列表页带过来的上下车站（区间票），没有则按全程处理 */
const rangeQuery = computed(() => {
  const from = route.query.from
  const to = route.query.to
  return (from || to) ? { from, to } : undefined
})

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await fetchTrainDetail(trainId, rangeQuery.value)
    // 每次打开页面 / 刷新余票都重置为全部折叠
    activeCarriages.value = []
    const first = (detail.value.stocks || []).find(s => s.availableCount > 0)
    if (!seatType.value && first) {
      seatType.value = first.seatType
    }
    // 购票资格预检：同一车次最多 9 张 + 已购车次运行时间内不可重复购票（下车后方可再买）
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

// ==================== 常用乘车人 ====================

async function loadPassengers() {
  try {
    passengers.value = await fetchPassengers() || []
  } catch (e) {
    // 加载失败不阻断购票，用户仍可手动填写乘客信息
    passengers.value = []
  }
}

/** 勾选 / 取消常用乘车人：只回传 id 给后端，证件号不出前端 */
function togglePassenger(p) {
  const idx = selectedPassengerIds.value.indexOf(p.id)
  if (idx >= 0) {
    selectedPassengerIds.value.splice(idx, 1)
    return
  }
  if (ticketCount.value >= TICKET_MAX) {
    ElMessage.warning(`一次最多购买 ${TICKET_MAX} 张票`)
    return
  }
  selectedPassengerIds.value.push(p.id)
}

/** 从本次购票中移除某个乘客 */
function removeTicket(t) {
  if (t.passengerId) {
    const idx = selectedPassengerIds.value.indexOf(t.passengerId)
    if (idx >= 0) {
      selectedPassengerIds.value.splice(idx, 1)
    }
    return
  }
  const idx = tempPassengers.value.findIndex(
    p => p.passengerName === t.passengerName && p.idCard === t.idCard)
  if (idx >= 0) {
    tempPassengers.value.splice(idx, 1)
  }
}

function openTempPassenger() {
  if (ticketCount.value >= TICKET_MAX) {
    ElMessage.warning(`一次最多购买 ${TICKET_MAX} 张票`)
    return
  }
  tempForm.value = { passengerName: '', idCard: '' }
  tempDialog.value = true
}

function submitTempPassenger() {
  const form = tempForm.value
  if (!form.passengerName || !validIdCard(form.idCard)) {
    ElMessage.warning('请填写正确的姓名与身份证号（校验位或出生日期不合法）')
    return
  }
  if (!form.phone || !validPhone(form.phone)) {
    ElMessage.warning('请填写正确的手机号（11 位且号段有效）')
    return
  }
  if (tempPassengers.value.some(p => p.idCard === form.idCard)) {
    ElMessage.warning('该乘客已在列表中')
    return
  }
  tempPassengers.value.push({
    passengerName: form.passengerName.trim(),
    idCard: form.idCard.trim(),
    phone: form.phone.trim(),
    passengerType: form.passengerType || 1
  })
  tempDialog.value = false
}

function openAddPassenger() {
  if (passengers.value.length >= PASSENGER_MAX) {
    ElMessage.warning(`常用乘车人最多 ${PASSENGER_MAX} 位`)
    return
  }
  passengerForm.value = { passengerName: '', idCard: '', phone: '', passengerType: 1 }
  passengerDialog.value = true
}

async function submitPassenger() {
  const form = passengerForm.value
  if (!form.passengerName || !form.idCard) {
    ElMessage.warning('请填写姓名与身份证号')
    return
  }
  if (!validIdCard(form.idCard)) {
    ElMessage.warning('身份证号不正确（校验位或出生日期不合法）')
    return
  }
  if (form.phone && !validPhone(form.phone)) {
    ElMessage.warning('手机号格式不正确（11 位且号段有效）')
    return
  }
  passengerSaving.value = true
  try {
    await addPassenger(form)
    ElMessage.success('已添加常用乘车人')
    passengerDialog.value = false
    await loadPassengers()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    passengerSaving.value = false
  }
}

async function removePassenger(p) {
  try {
    await deletePassenger(p.id)
    const idx = selectedPassengerIds.value.indexOf(p.id)
    if (idx >= 0) {
      selectedPassengerIds.value.splice(idx, 1)
    }
    ElMessage.success('已删除')
    await loadPassengers()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function onSeatTypeChange() {
  selectedSeatIds.value = []
  batchResult.value = null
  // 换席别后重新折叠所有车厢
  activeCarriages.value = []
}

function seatClass(seat) {
  if (selectedSeatIds.value.includes(seat.seatId)) {
    return 'selected'
  }
  return seat.status === 0 ? 'available' : 'sold'
}

/** 多选座位：最多只能选到乘客数那么多 */
function toggleSeat(seat) {
  if (seat.status !== 0) {
    ElMessage.warning('该座位已售出')
    return
  }
  const idx = selectedSeatIds.value.indexOf(seat.seatId)
  if (idx >= 0) {
    selectedSeatIds.value.splice(idx, 1)
    return
  }
  if (!ticketCount.value) {
    ElMessage.warning('请先选择乘客，再挑选座位')
    return
  }
  if (selectedSeatIds.value.length >= ticketCount.value) {
    ElMessage.warning(`本次共 ${ticketCount.value} 位乘客，最多选 ${ticketCount.value} 个座位`)
    return
  }
  selectedSeatIds.value.push(seat.seatId)
}

function clearSeat() {
  selectedSeatIds.value = []
}

function submitSeckill() {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return
  }
  if (!ticketCount.value) {
    ElMessage.warning('请选择乘客（最多 ' + TICKET_MAX + ' 位）')
    return
  }
  // 抢票前置人机验证：通过后回调 onCaptchaOk 再真正提交
  if (captchaRef.value) {
    captchaRef.value.open()
  }
}

/** 人机验证通过：带上验证码参数提交抢票 */
function onCaptchaOk(captcha) {
  runSeckill(captcha)
}

async function runSeckill(captcha) {
  seckilling.value = true
  batchResult.value = null
  try {
    const payload = {
      trainId,
      seatType: seatType.value,
      // 常用乘车人只回传 id，证件号由服务端解密带出；临时乘客传明文
      passengers: ticketList.value.map(t => (t.passengerId
        ? { passengerId: t.passengerId }
        // 临时乘客：手机号 / 乘客类型一并回传，服务端做格式校验
        : {
          passengerName: t.passengerName,
          idCard: t.idCard,
          phone: t.phone,
          passengerType: t.passengerType
        }))
    }
    if (selectedSeatIds.value.length) {
      payload.seatIds = selectedSeatIds.value.slice(0, ticketCount.value)
    }
    // 区间票：带上本次查询的上下车站（不传 = 买全程票）
    if (route.query.from) payload.fromStation = route.query.from
    if (route.query.to) payload.toStation = route.query.to
    // 人机验证参数（图形 captchaId/captchaCode 或滑块 sliderId/sliderX）
    if (captcha) {
      Object.assign(payload, captcha)
    }
    const vo = await doSeckill(payload)
    // 排队进度：12306 那种「你是第几位、前面还有几单」的体验
    const queueTip = vo && vo.queueAhead > 0
      ? `（排队序号 ${vo.queueSeq}，前面还有 ${vo.queueAhead} 张在处理）`
      : (vo && vo.queueSeq ? `（排队序号 ${vo.queueSeq}，正在处理）` : '')
    ElMessage.success(`共 ${ticketCount.value} 张票，抢票请求已提交，正在为您锁定座位…${queueTip}`)
    batchResult.value = { batchNo: vo.batchNo, count: vo.count || ticketCount.value, status: 0, message: '排队中', items: [] }
    pollBatchResult(vo.batchNo)
  } catch (e) {
    batchResult.value = { count: ticketCount.value, status: -1, message: e.message, items: [] }
    // 人机验证失败 / 已失效：重新弹出验证，乘客与座位选择保留
    if (e.message && e.message.indexOf('人机验证') >= 0) {
      if (captchaRef.value) {
        captchaRef.value.open()
      }
      return
    }
    // 选了座位却失败（多半是座位刚被他人抢走）：清空选择并刷新座位图，方便立即重选
    if (selectedSeatIds.value.length) {
      selectedSeatIds.value = []
      loadDetail()
    }
  } finally {
    seckilling.value = false
  }
}

/** 抢票结果通道：SSE 推送优先，轮询降级为兜底 */
let pollTimer = null
let sseSource = null

/** SSE 订阅：服务端每出一张票的结果就推一次，前端不再干等下一轮轮询 */
function subscribeSeckill(batchNo) {
  if (typeof EventSource === 'undefined') {
    return
  }
  try {
    const token = localCache.get('token') || ''
    const es = new EventSource(`/api/seckill/stream?batchNo=${encodeURIComponent(batchNo)}&token=${encodeURIComponent(token)}`)
    sseSource = es
    es.addEventListener('seckill-result', async () => {
      try {
        const res = await fetchBatchSeckillResult(batchNo)
        batchResult.value = res
        if (res.status !== 0) {
          finishPolling(res)
        }
      } catch (e) {
        // 拉取失败就继续等轮询兜底，不惊扰用户
      }
    })
    es.onerror = () => {
      es.close()
      if (sseSource === es) {
        sseSource = null
      }
    }
  } catch (e) {
    // 浏览器不支持 / 连接失败：静默降级为轮询
    sseSource = null
  }
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  if (sseSource) {
    sseSource.close()
    sseSource = null
  }
  polling.value = false
}

function finishPolling(res) {
  batchResult.value = res
  stopPolling()
  if (res.status === 1) {
    ElMessage.success('抢票成功')
  } else if (res.status === 2) {
    ElMessage.warning('部分票抢票失败，成功的票可先去支付')
  }
  loadDetail(false)
}

/**
 * 批量结果：SSE 连上时轮询只作兜底（3 秒一次、最多 40 次），
 * 连接不上则退回原来的 1 秒轮询，保证任何环境都能拿到结果。
 */
function pollBatchResult(batchNo) {
  if (polling.value || !batchNo) {
    return
  }
  stopPolling()
  polling.value = true
  subscribeSeckill(batchNo)
  const sseOk = !!sseSource
  const interval = sseOk ? 3000 : 1000
  const maxTimes = sseOk ? 40 : 20
  let times = 0
  pollTimer = setInterval(async () => {
    times++
    try {
      const res = await fetchBatchSeckillResult(batchNo)
      batchResult.value = res
      if (res.status !== 0 || times >= maxTimes) {
        finishPolling(res)
      }
    } catch (e) {
      // 查询异常也要给出明确失败态，避免页面一直卡在“排队中”
      finishPolling({ count: 0, status: -1, message: e.message || '查询抢票结果失败，请稍后重试', items: [] })
      loadDetail()
    }
  }, interval)
}
</script>

<style scoped>
.passenger-box {
  max-width: 620px;
  margin: 4px 0 14px;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}

.passenger-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 14px;
}

.passenger-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.passenger-card {
  position: relative;
  min-width: 180px;
  padding: 8px 10px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}

.passenger-card:hover {
  border-color: #409eff;
}

.passenger-card.active {
  border-color: #409eff;
  background: #ecf5ff;
}

.p-name {
  font-size: 14px;
  font-weight: 600;
}

.p-no {
  margin-top: 2px;
  color: #909399;
  font-size: 12px;
}

.p-del {
  position: absolute;
  right: 6px;
  bottom: 2px;
  font-size: 12px;
}

.muted.warn-tip {
  margin-top: 4px;
  color: #e6a23c;
}

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
