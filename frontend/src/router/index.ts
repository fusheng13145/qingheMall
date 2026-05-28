import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { setupRouterGuard } from './guard'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'goods/list',
        name: 'GoodsList',
        component: () => import('@/views/goods/list.vue'),
        meta: { title: '商品列表' }
      },
      {
        path: 'goods/detail/:id',
        name: 'GoodsDetail',
        component: () => import('@/views/goods/detail.vue'),
        meta: { title: '商品详情' }
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('@/views/cart/index.vue'),
        meta: { title: '购物车' }
      }
    ]
  },
  {
    path: '/order',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      {
        path: 'confirm',
        name: 'OrderConfirm',
        component: () => import('@/views/order/confirm.vue'),
        meta: { title: '确认订单', requiresAuth: true }
      },
      {
        path: 'list',
        name: 'OrderList',
        component: () => import('@/views/order/list.vue'),
        meta: { title: '我的订单', requiresAuth: true }
      },
      {
        path: 'detail/:id',
        name: 'OrderDetail',
        component: () => import('@/views/order/detail.vue'),
        meta: { title: '订单详情', requiresAuth: true }
      }
    ]
  },
  {
    path: '/payment',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      {
        path: '',
        name: 'Payment',
        component: () => import('@/views/payment/index.vue'),
        meta: { title: '支付', requiresAuth: true }
      }
    ]
  },
  {
    path: '/user',
    component: () => import('@/layouts/UserLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/user/profile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'address',
        name: 'UserAddress',
        component: () => import('@/views/user/address.vue'),
        meta: { title: '收货地址', requiresAuth: true }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hideNavbar: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { title: '注册', hideNavbar: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  }
})

// 设置路由守卫
setupRouterGuard(router)

export default router
