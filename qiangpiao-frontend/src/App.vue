<template>
  <div class="app-wrapper">
    <el-header class="app-header">
      <div class="header-inner">
        <div class="logo" @click="$router.push('/trains')">抢票系统</div>
        <el-menu :default-active="activeMenu" mode="horizontal" :router="true" class="nav-menu">
          <el-menu-item index="/trains">车次查询</el-menu-item>
          <el-menu-item index="/orders">我的订单</el-menu-item>
          <el-menu-item index="/wallet">我的钱包</el-menu-item>
          <el-menu-item index="/profile">个人中心</el-menu-item>
        </el-menu>
        <div class="header-right">
          <template v-if="userStore.isLogin">
            <span class="muted">你好，{{ userStore.username }}</span>
            <el-button link type="primary" @click="handleLogout">退出</el-button>
          </template>
          <el-button v-else type="primary" size="small" @click="$router.push('/login')">登录 / 注册</el-button>
        </div>
      </div>
    </el-header>

    <el-main class="app-main">
      <router-view v-slot="{ Component }">
        <component :is="Component"/>
      </router-view>
    </el-main>

    <el-footer class="app-footer">
      <span class="muted">SSM + Redis 三级缓存 + 前端缓存 · 火车票秒杀示例</span>
    </el-footer>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useStationStore } from '@/store/station'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const stationStore = useStationStore()

const activeMenu = computed(() => '/' + (route.path.split('/')[1] || 'trains'))

onMounted(() => {
  // 预加载车站（前端缓存优先）
  stationStore.loadStations()
  if (userStore.isLogin && !userStore.userInfo) {
    userStore.loadUserInfo()
  }
})

function handleLogout() {
  ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
      .then(() => {
        userStore.logout()
        router.push('/login')
      })
      .catch(() => {
      })
}
</script>

<style scoped>
.app-wrapper {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.app-header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, .06);
  padding: 0;
  height: 60px;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.logo {
  font-size: 20px;
  font-weight: 700;
  color: #1a73e8;
  cursor: pointer;
  white-space: nowrap;
}

.nav-menu {
  flex: 1;
  border: none;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-main {
  flex: 1;
  padding: 0;
}

.app-footer {
  text-align: center;
  padding: 16px 0;
}
</style>
