import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user.js'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/products',
    name: 'Products',
    component: () => import('../views/Products.vue')
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('../views/ProductDetail.vue')
  },
  {
    path: '/seckill',
    name: 'Seckill',
    component: () => import('../views/Seckill.vue')
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('../views/Orders.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('../views/Cart.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/coupons',
    name: 'Coupons',
    component: () => import('../views/Coupons.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/my-coupons',
    name: 'MyCoupons',
    component: () => import('../views/MyCoupons.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/checkout',
    name: 'Checkout',
    component: () => import('../views/Checkout.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/pay-success',
    name: 'PaySuccess',
    component: () => import('../views/PaySuccess.vue')
  },
  {
    path: '/pay',
    name: 'Pay',
    component: () => import('../views/Pay.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      { path: '', name: 'AdminDashboard', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'products', name: 'AdminProducts', component: () => import('../views/admin/Products.vue') },
      { path: 'orders', name: 'AdminOrders', component: () => import('../views/admin/Orders.vue') },
      { path: 'coupons', name: 'AdminCoupons', component: () => import('../views/admin/Coupons.vue') },
      { path: 'seckills', name: 'AdminSeckills', component: () => import('../views/admin/Seckill.vue') },
      { path: 'merchants', name: 'AdminMerchants', component: () => import('../views/admin/Merchants.vue') },
      { path: 'users', name: 'AdminUsers', component: () => import('../views/admin/Users.vue') }
    ]
  },
  {
    path: '/merchant',
    name: 'Merchant',
    component: () => import('../views/merchant/MerchantLayout.vue'),
    meta: { requiresAuth: true, requiresMerchant: true },
    children: [
      { path: '', name: 'MerchantOverview', component: () => import('../views/merchant/Overview.vue') },
      { path: 'products', name: 'MerchantProducts', component: () => import('../views/merchant/Products.vue') },
      { path: 'orders', name: 'MerchantOrders', component: () => import('../views/merchant/Orders.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  // P0-4 修复：首次访问受保护页且本地无登录态时，先尝试恢复会话再判断，
  // 避免已登录用户刷新/直达 /orders、/admin、/merchant 等被误踢回登录页
  //（checkLogin 内部有 Promise 缓存，与 App.vue onMounted 并发时只发一次请求）
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    await userStore.checkLogin()
  }
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next({ name: 'Home' })
    return
  }
  if (to.meta.requiresMerchant && !userStore.isMerchant) {
    next({ name: 'Home' })
    return
  }
  next()
})

export default router
