import { createRouter, createWebHistory } from 'vue-router'
import { localCache } from '@/utils/cache'

const routes = [
    { path: '/', redirect: '/trains' },
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/LoginView.vue'),
        meta: { title: '登录 / 注册' }
    },
    {
        path: '/trains',
        name: 'Trains',
        component: () => import('@/views/TrainListView.vue'),
        meta: { title: '车次查询' }
    },
    {
        path: '/trains/:trainId',
        name: 'TrainDetail',
        component: () => import('@/views/TrainDetailView.vue'),
        meta: { title: '车次详情' }
    },
    {
        path: '/orders',
        name: 'Orders',
        component: () => import('@/views/OrderListView.vue'),
        meta: { title: '我的订单', requiresAuth: true }
    },
    {
        path: '/tickets',
        name: 'Tickets',
        component: () => import('@/views/TicketView.vue'),
        meta: { title: '我的车票', requiresAuth: true }
    },
    {
        path: '/refund',
        name: 'Refund',
        component: () => import('@/views/RefundView.vue'),
        meta: { title: '退票 / 改签', requiresAuth: true }
    },
    {
        path: '/orders/:orderNo',
        name: 'OrderDetail',
        component: () => import('@/views/OrderDetailView.vue'),
        meta: { title: '订单详情', requiresAuth: true }
    },
    {
        path: '/admin',
        name: 'Admin',
        component: () => import('@/views/AdminView.vue'),
        meta: { title: '管理后台', requiresAuth: true }
    },
    {
        path: '/wallet',
        name: 'Wallet',
        component: () => import('@/views/WalletView.vue'),
        meta: { title: '我的钱包', requiresAuth: true }
    },
    {
        path: '/profile',
        name: 'Profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { title: '个人中心', requiresAuth: true }
    },
    { path: '/:pathMatch(.*)*', redirect: '/trains' }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach((to, from, next) => {
    document.title = to.meta.title ? `${to.meta.title} - 抢票系统` : '抢票系统'
    if (to.meta.requiresAuth && !localCache.get('token')) {
        return next({ path: '/login', query: { redirect: to.fullPath } })
    }
    next()
})

export default router
