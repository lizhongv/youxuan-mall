import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { clearChunkReloadFlag, handleChunkError, isChunkError } from '@/utils/chunkError'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    /** 需要登录 */
    auth?: boolean
    /** 显示底部标签栏 */
    tabbar?: boolean
    /** 缓存页面（返回时保留滚动与数据） */
    keepAlive?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  { path: '/', name: 'home', component: () => import('@/views/home/HomeView.vue'), meta: { title: '首页', tabbar: true, keepAlive: true } },
  { path: '/category', name: 'category', component: () => import('@/views/category/CategoryView.vue'), meta: { title: '分类', tabbar: true, keepAlive: true } },
  { path: '/cart', name: 'cart', component: () => import('@/views/cart/CartView.vue'), meta: { title: '购物车', tabbar: true } },
  { path: '/mine', name: 'mine', component: () => import('@/views/mine/MineView.vue'), meta: { title: '我的', tabbar: true } },

  { path: '/search', name: 'search', component: () => import('@/views/search/SearchView.vue'), meta: { title: '搜索' } },
  { path: '/goods/list', name: 'goods-list', component: () => import('@/views/search/GoodsListView.vue'), meta: { title: '商品列表', keepAlive: true } },
  { path: '/goods/:id', name: 'goods', component: () => import('@/views/goods/GoodsDetailView.vue'), meta: { title: '商品详情' } },
  { path: '/goods/:id/reviews', name: 'goods-reviews', component: () => import('@/views/goods/GoodsReviewsView.vue'), meta: { title: '商品评价' } },
  { path: '/seckill', name: 'seckill', component: () => import('@/views/home/SeckillView.vue'), meta: { title: '限时秒杀' } },
  { path: '/notice/:id', name: 'notice', component: () => import('@/views/home/NoticeView.vue'), meta: { title: '公告详情' } },

  { path: '/order/confirm', name: 'order-confirm', component: () => import('@/views/order/OrderConfirmView.vue'), meta: { title: '确认订单', auth: true } },
  { path: '/pay/:orderId', name: 'cashier', component: () => import('@/views/order/CashierView.vue'), meta: { title: '收银台', auth: true } },
  { path: '/pay-result', name: 'pay-result', component: () => import('@/views/order/PayResultView.vue'), meta: { title: '支付结果', auth: true } },
  { path: '/order/list', name: 'order-list', component: () => import('@/views/order/OrderListView.vue'), meta: { title: '我的订单', auth: true } },
  { path: '/order/:id/logistics', name: 'logistics', component: () => import('@/views/order/LogisticsView.vue'), meta: { title: '物流详情', auth: true } },
  { path: '/order/:id', name: 'order-detail', component: () => import('@/views/order/OrderDetailView.vue'), meta: { title: '订单详情', auth: true } },

  { path: '/aftersale/apply', name: 'aftersale-apply', component: () => import('@/views/aftersale/AftersaleApplyView.vue'), meta: { title: '申请售后', auth: true } },
  { path: '/aftersale/list', name: 'aftersale-list', component: () => import('@/views/aftersale/AftersaleListView.vue'), meta: { title: '售后服务', auth: true } },
  { path: '/aftersale/:id', name: 'aftersale-detail', component: () => import('@/views/aftersale/AftersaleDetailView.vue'), meta: { title: '售后详情', auth: true } },

  { path: '/review/create', name: 'review-create', component: () => import('@/views/review/ReviewCreateView.vue'), meta: { title: '发表评价', auth: true } },
  { path: '/review/mine', name: 'review-mine', component: () => import('@/views/review/MyReviewsView.vue'), meta: { title: '我的评价', auth: true } },

  { path: '/profile', name: 'profile', component: () => import('@/views/mine/ProfileView.vue'), meta: { title: '个人资料', auth: true } },
  { path: '/settings', name: 'settings', component: () => import('@/views/mine/SettingsView.vue'), meta: { title: '设置' } },
  { path: '/password', name: 'password', component: () => import('@/views/mine/PasswordView.vue'), meta: { title: '登录密码', auth: true } },
  { path: '/pay-password', name: 'pay-password', component: () => import('@/views/mine/PayPasswordView.vue'), meta: { title: '支付密码', auth: true } },
  { path: '/agreement/:type(user|privacy)', name: 'agreement', component: () => import('@/views/mine/AgreementView.vue'), meta: { title: '协议' } },
  { path: '/about', name: 'about', component: () => import('@/views/mine/AboutView.vue'), meta: { title: '关于我们' } },
  { path: '/address', name: 'address', component: () => import('@/views/address/AddressListView.vue'), meta: { title: '收货地址', auth: true } },
  { path: '/address/edit', name: 'address-edit', component: () => import('@/views/address/AddressEditView.vue'), meta: { title: '编辑地址', auth: true } },
  { path: '/coupon/mine', name: 'my-coupons', component: () => import('@/views/coupon/MyCouponsView.vue'), meta: { title: '我的优惠券', auth: true } },
  { path: '/coupon/center', name: 'coupon-center', component: () => import('@/views/coupon/CouponCenterView.vue'), meta: { title: '领券中心' } },
  { path: '/favorite', name: 'favorite', component: () => import('@/views/mine/FavoriteView.vue'), meta: { title: '我的收藏', auth: true } },
  { path: '/history', name: 'history', component: () => import('@/views/mine/HistoryView.vue'), meta: { title: '浏览足迹', auth: true } },
  { path: '/account-log', name: 'account-log', component: () => import('@/views/mine/AccountLogView.vue'), meta: { title: '账户明细', auth: true } },

  { path: '/login', name: 'login', component: () => import('@/views/auth/LoginView.vue'), meta: { title: '登录' } },
  { path: '/register', name: 'register', component: () => import('@/views/auth/RegisterView.vue'), meta: { title: '注册' } },
  { path: '/forgot', name: 'forgot', component: () => import('@/views/auth/ForgotView.vue'), meta: { title: '找回密码' } },
  { path: '/auth/wechat', name: 'wechat-callback', component: () => import('@/views/auth/WechatCallbackView.vue'), meta: { title: '微信登录' } },
  { path: '/auth/bind', name: 'wechat-bind', component: () => import('@/views/auth/BindMobileView.vue'), meta: { title: '绑定手机号' } },

  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue'), meta: { title: '页面不存在' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, saved) {
    return saved || { top: 0 }
  },
})

router.beforeEach((to) => {
  const user = useUserStore()
  if (to.meta.auth && !user.isLogin) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if ((to.name === 'login' || to.name === 'register') && user.isLogin) {
    return typeof to.query.redirect === 'string' ? to.query.redirect : { name: 'mine' }
  }
  return true
})

// 懒加载页面分片失败（发版后旧分片被删除 / 弱网）：自动刷新一次，再失败显示重试
router.onError((err, to) => {
  if (isChunkError(err)) handleChunkError(to?.fullPath ? router.resolve(to.fullPath).href : undefined)
})

router.afterEach((to, _from, failure) => {
  if (!failure) setTimeout(clearChunkReloadFlag, 3000)
  const base = document.title.split(' - ').pop() || '商城'
  document.title = to.meta.title ? `${to.meta.title} - ${base}` : base
})

export default router
