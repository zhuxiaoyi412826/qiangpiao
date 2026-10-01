<template>
  <div class="app-wrapper">
    <el-header class="app-header">
      <div class="header-inner">
        <div class="logo" @click="$router.push('/home')">抢票系统</div>
        <el-menu :default-active="activeMenu" mode="horizontal" :router="true" class="nav-menu">
          <!-- 管理员：后台各管理模块平铺到顶部导航 -->
          <template v-if="isAdmin">
            <el-menu-item index="/home">首页</el-menu-item>
            <el-menu-item v-for="m in adminMenus" :key="m.tab" :index="'/admin?tab=' + m.tab">
              {{ m.label }}
            </el-menu-item>
          </template>
          <!-- 普通用户 -->
          <template v-else>
            <el-menu-item index="/home">首页</el-menu-item>
            <el-menu-item index="/trains">车次查询</el-menu-item>
            <el-menu-item index="/orders">我的订单</el-menu-item>
            <el-menu-item index="/tickets">我的车票</el-menu-item>
            <el-menu-item index="/refund">退票/改签</el-menu-item>
            <el-menu-item index="/wallet">我的钱包</el-menu-item>
            <el-menu-item index="/profile">个人中心</el-menu-item>
          </template>
        </el-menu>
        <div class="header-right">
          <template v-if="userStore.isLogin">
            <span class="muted greeting">你好，{{ userStore.username }}</span>
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
import { computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useStationStore } from '@/store/station'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const stationStore = useStationStore()

// 只有 ROLE_ADMIN 才显示后台管理菜单（平铺到顶部导航）
const isAdmin = computed(() => {
  const roles = userStore.userInfo && userStore.userInfo.roles
  if (!roles) return false
  const list = Array.isArray(roles) ? roles : [roles]
  return list.some(r => String(r).toUpperCase().includes('ADMIN'))
})

// 后台管理模块：与 AdminView 内的 tab 一一对应
const adminMenus = [
  { tab: 'stats', label: '运营概览' },
  { tab: 'stations', label: '车站管理' },
  { tab: 'lines', label: '线路管理' },
  { tab: 'trains', label: '车次管理' },
  { tab: 'price', label: '票价 / 库存' },
  { tab: 'orders', label: '订单管理' },
  { tab: 'users', label: '用户管理' },
  { tab: 'notice', label: '公告管理' },
  { tab: 'monitor', label: '票务监控' },
  { tab: 'risk', label: '风控管理' }
]

const activeMenu = computed(() => {
  if (route.path === '/home') {
    return '/home'
  }
  if (isAdmin.value) {
    return '/admin?tab=' + (route.query.tab || 'stats')
  }
  return '/' + (route.path.split('/')[1] || 'trains')
})

/** 登录态心跳间隔：被管理端强制下线后，最迟 60s 内页面自动变回未登录 */
const HEARTBEAT_MS = 60000
let heartbeatTimer = null
let lastCheckAt = 0

/** 心跳校验：请求 /auth/info，token 已被拉黑时后端返回 401，request 层会派发 auth:expired */
function checkLoginState() {
  if (!userStore.isLogin) return
  lastCheckAt = Date.now()
  userStore.loadUserInfo()
}

/** 标签页切回来时补一次校验（10s 内的重复切换不查，省一次请求） */
function onVisibilityChange() {
  if (document.visibilityState !== 'visible' || !userStore.isLogin) return
  if (Date.now() - lastCheckAt < 10000) return
  checkLoginState()
}

/** 登录失效（登出 / 被强制下线 / 封号）：清状态 → 头部立刻变「登录 / 注册」 */
function onAuthExpired() {
  userStore.clearAuth()
  ElMessage.warning('登录状态已失效（已退出或被强制下线），请重新登录')
  if (route.meta.requiresAuth) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
  }
}

onMounted(() => {
  // 预加载车站（前端缓存优先）
  stationStore.loadStations()
  if (userStore.isLogin && !userStore.userInfo) {
    userStore.loadUserInfo()
  }
  heartbeatTimer = setInterval(checkLoginState, HEARTBEAT_MS)
  document.addEventListener('visibilitychange', onVisibilityChange)
  window.addEventListener('auth:expired', onAuthExpired)
})

onBeforeUnmount(() => {
  if (heartbeatTimer) clearInterval(heartbeatTimer)
  document.removeEventListener('visibilitychange', onVisibilityChange)
  window.removeEventListener('auth:expired', onAuthExpired)
})

function handleLogout() {
  ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
      .then(async () => {
        await userStore.logout()
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

/* 移动端：菜单横向滚动（管理员 9 个模块也不会挤爆），头部紧凑 */
@media (max-width: 768px) {
  .header-inner {
    padding: 0 10px;
    gap: 10px;
  }

  .logo {
    font-size: 17px;
  }

  .greeting {
    display: none;
  }

  .nav-menu {
    min-width: 0;
    overflow-x: auto;
    overflow-y: hidden;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
  }

  .nav-menu::-webkit-scrollbar {
    display: none;
  }

  .nav-menu :deep(.el-menu-item) {
    flex: 0 0 auto;
    padding: 0 12px;
    font-size: 14px;
  }

  .app-header {
    height: 54px;
  }

  .header-inner {
    height: 54px;
  }
}
</style>
