import type { RouteLocationNormalized, RouteRecordRaw } from 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    icon?: string
    /** 查看权限码，见 docs/mall/api.md §4 */
    permission?: string
    /** 不在菜单中显示 */
    hidden?: boolean
    /** 隐藏路由高亮的菜单 */
    activeMenu?: string
    /** 不出现在标签页 */
    hideTab?: boolean
    /** 动态标签标题 */
    tabTitle?: (route: RouteLocationNormalized) => string
    /** 公开页面（无需登录） */
    public?: boolean
  }
}

export interface MenuNode {
  path: string
  title: string
  icon?: string
  permission?: string
  children?: MenuNode[]
}

interface PageDef {
  path: string
  name: string
  title: string
  icon?: string
  permission?: string
  hidden?: boolean
  activeMenu?: string
  tabTitle?: (route: RouteLocationNormalized) => string
  component: RouteRecordRaw['component']
}

interface GroupDef {
  title: string
  icon: string
  /** 单页分组直接作为一级菜单 */
  flat?: boolean
  pages: PageDef[]
}

/** 静态路由 + 菜单定义（单一来源） */
const groups: GroupDef[] = [
  {
    title: '首页',
    icon: 'HomeFilled',
    flat: true,
    pages: [
      { path: '/dashboard', name: 'Dashboard', title: '数据概览', icon: 'Odometer', permission: 'stats:view', component: () => import('@/views/dashboard/index.vue') },
    ],
  },
  {
    title: '商品',
    icon: 'Goods',
    pages: [
      { path: '/goods/list', name: 'GoodsList', title: '商品列表', icon: 'Goods', permission: 'goods:goods:view', component: () => import('@/views/goods/list.vue') },
      {
        path: '/goods/edit/:id?',
        name: 'GoodsEdit',
        title: '发布商品',
        icon: 'EditPen',
        permission: 'goods:goods:view',
        hidden: true,
        activeMenu: '/goods/list',
        tabTitle: (r) => (r.params.id ? '编辑商品' : '发布商品'),
        component: () => import('@/views/goods/edit.vue'),
      },
      { path: '/goods/category', name: 'GoodsCategory', title: '商品类目', icon: 'Menu', permission: 'goods:category:view', component: () => import('@/views/goods/category.vue') },
      { path: '/goods/brand', name: 'GoodsBrand', title: '品牌管理', icon: 'CollectionTag', permission: 'goods:brand:view', component: () => import('@/views/goods/brand.vue') },
    ],
  },
  {
    title: '订单',
    icon: 'Tickets',
    pages: [
      { path: '/trade/order', name: 'TradeOrder', title: '订单列表', icon: 'List', permission: 'trade:order:view', component: () => import('@/views/trade/order.vue') },
      {
        path: '/trade/order/:id',
        name: 'TradeOrderDetail',
        title: '订单详情',
        icon: 'Document',
        permission: 'trade:order:view',
        hidden: true,
        activeMenu: '/trade/order',
        component: () => import('@/views/trade/order-detail.vue'),
      },
      { path: '/trade/aftersale', name: 'TradeAftersale', title: '售后处理', icon: 'RefreshLeft', permission: 'trade:aftersale:view', component: () => import('@/views/trade/aftersale.vue') },
      { path: '/trade/payment', name: 'TradePayment', title: '支付流水', icon: 'Wallet', permission: 'trade:payment:view', component: () => import('@/views/trade/payment/index.vue') },
      { path: '/trade/review', name: 'TradeReview', title: '评价管理', icon: 'ChatLineSquare', permission: 'trade:review:view', component: () => import('@/views/trade/review.vue') },
      { path: '/trade/freight', name: 'TradeFreight', title: '运费模板', icon: 'Van', permission: 'trade:freight:view', component: () => import('@/views/trade/freight/index.vue') },
    ],
  },
  {
    title: '营销',
    icon: 'Present',
    pages: [
      { path: '/marketing/coupon', name: 'MarketingCoupon', title: '优惠券', icon: 'Ticket', permission: 'marketing:coupon:view', component: () => import('@/views/marketing/coupon.vue') },
      { path: '/marketing/seckill', name: 'MarketingSeckill', title: '秒杀活动', icon: 'Lightning', permission: 'marketing:seckill:view', component: () => import('@/views/marketing/seckill.vue') },
      { path: '/marketing/banner', name: 'MarketingBanner', title: '轮播广告', icon: 'PictureFilled', permission: 'marketing:banner:view', component: () => import('@/views/marketing/banner.vue') },
      { path: '/marketing/notice', name: 'MarketingNotice', title: '商城公告', icon: 'Bell', permission: 'marketing:notice:view', component: () => import('@/views/marketing/notice.vue') },
    ],
  },
  {
    title: '会员',
    icon: 'User',
    pages: [
      { path: '/member/list', name: 'MemberList', title: '会员列表', icon: 'Avatar', permission: 'member:member:view', component: () => import('@/views/member/list.vue') },
      { path: '/member/level', name: 'MemberLevel', title: '会员等级', icon: 'Medal', permission: 'member:level:view', component: () => import('@/views/member/level.vue') },
    ],
  },
  {
    title: '系统',
    icon: 'Setting',
    pages: [
      { path: '/system/admin', name: 'SystemAdmin', title: '管理员', icon: 'UserFilled', permission: 'system:admin:view', component: () => import('@/views/system/admin.vue') },
      { path: '/system/role', name: 'SystemRole', title: '角色权限', icon: 'Key', permission: 'system:role:view', component: () => import('@/views/system/role.vue') },
      { path: '/system/menu', name: 'SystemMenu', title: '菜单管理', icon: 'Grid', permission: 'system:menu:view', component: () => import('@/views/system/menu.vue') },
      { path: '/system/dict', name: 'SystemDict', title: '数据字典', icon: 'Notebook', permission: 'system:dict:view', component: () => import('@/views/system/dict.vue') },
      { path: '/system/config', name: 'SystemConfig', title: '系统参数', icon: 'Tools', permission: 'system:config:view', component: () => import('@/views/system/config.vue') },
      { path: '/system/oper-log', name: 'SystemOperLog', title: '操作日志', icon: 'Memo', permission: 'system:log:view', component: () => import('@/views/system/oper-log.vue') },
      { path: '/system/login-log', name: 'SystemLoginLog', title: '登录日志', icon: 'Finished', permission: 'system:log:view', component: () => import('@/views/system/login-log.vue') },
    ],
  },
]

const pageRoutes: RouteRecordRaw[] = groups.flatMap((g) =>
  g.pages.map((p) => ({
    path: p.path,
    name: p.name,
    component: p.component!,
    meta: {
      title: p.title,
      icon: p.icon,
      permission: p.permission,
      hidden: p.hidden,
      activeMenu: p.activeMenu,
      tabTitle: p.tabTitle,
    },
  })) as RouteRecordRaw[],
)

/** 侧边菜单树（未过滤权限） */
export const menuTree: MenuNode[] = groups.map((g) => {
  const visible = g.pages.filter((p) => !p.hidden)
  if (g.flat && visible.length === 1) {
    const p = visible[0]
    return { path: p.path, title: p.title, icon: p.icon, permission: p.permission }
  }
  return {
    path: `group:${g.title}`,
    title: g.title,
    icon: g.icon,
    children: visible.map((p) => ({ path: p.path, title: p.title, icon: p.icon, permission: p.permission })),
  }
})

/** 面包屑：路径 → [分组, 页面] */
export const breadcrumbMap: Record<string, string[]> = Object.fromEntries(
  groups.flatMap((g) => g.pages.map((p) => [p.name, g.flat ? [p.title] : [g.title, p.title]])),
)

export const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'Login', component: () => import('@/views/login/index.vue'), meta: { title: '登录', public: true } },
  // 强制修改初始密码：需登录但不进入布局，不能绕过（守卫拦截其他页面）
  { path: '/change-password', name: 'ChangePassword', component: () => import('@/views/login/change-password.vue'), meta: { title: '修改初始密码', hideTab: true } },
  {
    path: '/',
    name: 'Root',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      ...pageRoutes,
      { path: '/profile', name: 'Profile', component: () => import('@/views/profile/index.vue'), meta: { title: '个人中心', icon: 'User', hidden: true } },
      { path: '/403', name: 'Forbidden', component: () => import('@/views/error/403.vue'), meta: { title: '无权限', hidden: true, hideTab: true } },
    ],
  },
  { path: '/network-error', name: 'NetworkError', component: () => import('@/views/error/network.vue'), meta: { title: '网络异常', public: true } },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/error/404.vue'), meta: { title: '页面不存在', public: true } },
]
