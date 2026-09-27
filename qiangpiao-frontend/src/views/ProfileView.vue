<template>
  <div class="page-container">
    <div class="card-panel">
      <h3>个人信息</h3>
      <el-descriptions :column="1" border v-if="userStore.userInfo">
        <el-descriptions-item label="用户ID">{{ userStore.userInfo.userId }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ userStore.userInfo.username }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ userStore.userInfo.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ userStore.userInfo.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ (userStore.userInfo.roles || []).join(',') }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="未登录"/>
    </div>

    <div class="card-panel">
      <h3>我的车票</h3>
      <div class="wallet-entry">
        <div>
          <span class="muted">查看已购车票</span>
          <div class="entry-balance">未开车 / 历史车票</div>
        </div>
        <el-button type="primary" @click="$router.push('/tickets')">查看车票</el-button>
      </div>
      <p class="muted">未开车：已支付且尚未发车的车票；历史车票：已乘车的票以及已取消 / 退票 / 超时的记录。</p>
    </div>

    <div class="card-panel">
      <h3>我的钱包</h3>
      <div class="wallet-entry">
        <div>
          <span class="muted">账户余额（元）</span>
          <div class="entry-balance">¥ {{ balanceText }}</div>
        </div>
        <el-button type="primary" @click="$router.push('/wallet')">进入钱包</el-button>
      </div>
      <p class="muted">购票支付时从钱包余额扣款并生成零钱流水，余额不足可自定义金额充值。</p>
    </div>

    <div class="card-panel">
      <h3>前端缓存</h3>
      <p class="muted">
        车站列表、车次查询结果等低频变更数据缓存在浏览器 localStorage，
        与后端 L1(Caffeine) / L2(Redis) / L3(DB) 三级缓存协同降低后端压力。
      </p>
      <el-button type="warning" @click="clearCache">清空前端缓存</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useStationStore } from '@/store/station'
import { localCache } from '@/utils/cache'
import { fetchWallet } from '@/api/wallet'

const userStore = useUserStore()
const stationStore = useStationStore()
const wallet = ref(null)
const balanceText = computed(() => wallet.value ? Number(wallet.value.balance).toFixed(2) : '--')

onMounted(() => {
  if (userStore.isLogin) {
    userStore.loadUserInfo()
    loadWallet()
  }
})

async function loadWallet() {
  try {
    wallet.value = await fetchWallet()
  } catch (e) {
    wallet.value = null
  }
}

function clearCache() {
  localCache.clearAll()
  stationStore.clear()
  ElMessage.success('前端缓存已清空')
}
</script>

<style scoped>
.wallet-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.entry-balance {
  font-size: 24px;
  font-weight: 700;
  color: #1a73e8;
  margin-top: 4px;
}
</style>
