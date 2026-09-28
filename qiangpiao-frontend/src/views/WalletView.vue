<template>
  <div class="page-container">
    <SkeletonCard v-if="loading" :rows="3"/>
    <ErrorRetry v-else-if="errorMsg" :message="errorMsg" @retry="reload"/>
    <template v-else>
    <div class="card-panel wallet-panel">
      <div class="wallet-info">
        <div class="label">账户余额（元）</div>
        <div class="balance">¥ {{ fmt(wallet.balance) }}</div>
        <div class="sub">
          <span>累计充值 ¥{{ fmt(wallet.totalRecharge) }}</span>
          <el-divider direction="vertical"/>
          <span>累计消费 ¥{{ fmt(wallet.totalConsume) }}</span>
        </div>
      </div>
      <div class="wallet-ops">
        <el-button type="primary" size="large" @click="openRecharge">充值</el-button>
        <el-button size="large" @click="loadWallet">刷新</el-button>
      </div>
    </div>

    <div class="card-panel">
      <h3>零钱明细</h3>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="时间" prop="createTime" width="180"/>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 1 ? 'success' : row.type === 2 ? 'danger' : 'warning'">
              {{ row.typeText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="摘要" prop="title" min-width="180"/>
        <el-table-column label="明细" min-width="240">
          <template #default="{ row }">
            <span class="muted">{{ row.detail || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            <span :class="row.amount >= 0 ? 'amount-in' : 'amount-out'">{{ sign(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="余额" width="120" align="right">
          <template #default="{ row }"><span class="price">¥ {{ fmt(row.balance) }}</span></template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无零钱流水"/>
        </template>
      </el-table>

      <el-pagination class="pager" background layout="total, prev, pager, next"
                     :current-page="pageNum" :page-size="pageSize" :total="total"
                     @current-change="onPageChange"/>
    </div>
    </template>

    <el-dialog v-model="rechargeVisible" title="钱包充值" width="420px">
      <el-form label-width="72px">
        <el-form-item label="金额">
          <el-input-number v-model="amount" :min="0.01" :max="50000" :precision="2" :step="50"
                           style="width: 200px"/>
          <span class="unit">元</span>
        </el-form-item>
        <el-form-item label="快捷">
          <el-button v-for="v in quickAmounts" :key="v" size="small" round @click="amount = v">¥ {{ v }}</el-button>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="remark" placeholder="选填，如：支付宝充值"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="recharging" @click="submitRecharge">确认充值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchWallet, queryWalletFlows, rechargeWallet } from '@/api/wallet'
import SkeletonCard from '@/components/SkeletonCard.vue'
import ErrorRetry from '@/components/ErrorRetry.vue'

const loading = ref(false)
const recharging = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const wallet = ref({})
const quickAmounts = [50, 100, 200, 500, 1000]
const rechargeVisible = ref(false)
const amount = ref(100)
const remark = ref('')
const errorMsg = ref('')

onMounted(() => {
  loadWallet()
  loadFlows()
})

// 失败后整体重试（余额 + 流水）
function reload() {
  loadWallet()
  loadFlows()
}

async function loadWallet() {
  try {
    wallet.value = await fetchWallet() || {}
    errorMsg.value = ''
  } catch (e) {
    errorMsg.value = e.message || '加载钱包失败'
  }
}

async function loadFlows() {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await queryWalletFlows({ pageNum: pageNum.value, pageSize: pageSize.value })
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    errorMsg.value = e.message || '加载零钱明细失败'
  } finally {
    loading.value = false
  }
}

function onPageChange(page) {
  pageNum.value = page
  loadFlows()
}

function openRecharge() {
  amount.value = 100
  remark.value = ''
  rechargeVisible.value = true
}

async function submitRecharge() {
  if (!amount.value || amount.value <= 0) {
    ElMessage.warning('请输入大于 0 的充值金额')
    return
  }
  recharging.value = true
  try {
    const data = await rechargeWallet(amount.value, remark.value)
    wallet.value = data || wallet.value
    rechargeVisible.value = false
    ElMessage.success(`充值成功，当前余额 ¥${fmt(data.balance)}`)
    loadFlows()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    recharging.value = false
  }
}

function fmt(v) {
  return v === undefined || v === null ? '--' : Number(v).toFixed(2)
}

function sign(v) {
  const n = Number(v || 0)
  return (n > 0 ? '+' : '') + n.toFixed(2)
}
</script>

<style scoped>
.wallet-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #1a73e8, #4f9cf5);
  color: #fff;
}

.wallet-panel .label {
  font-size: 14px;
  opacity: .85;
}

.wallet-panel .balance {
  font-size: 34px;
  font-weight: 700;
  margin: 6px 0;
}

.wallet-panel .sub {
  font-size: 13px;
  opacity: .9;
}

.amount-in {
  color: #2f9e44;
  font-weight: 600;
}

.amount-out {
  color: #e03131;
  font-weight: 600;
}

.unit {
  margin-left: 8px;
}
</style>
