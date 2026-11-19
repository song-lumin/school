// router/index.ts —— 路由配置文件
// 功能：定义整个应用的所有页面路由（URL 与组件的映射关系）以及全局导航守卫。
// 为什么需要：
//   1) Vue Router 让系统表现为“多页面”的单页应用（SPA），切换页面不刷新浏览器；
//   2) 路由元信息 meta + 全局前置守卫实现权限控制：未登录不能进个人页、
//      非管理员不能进后台页，这是系统安全的第一道前端防线。
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  // 使用 HTML5 History 模式：URL 中没有 # 号，更美观（需要服务器端配合回退到 index.html）
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // ---------- 登录 / 注册页：独立于主布局，全屏展示 ----------
    {
      path: '/login',
      name: 'login',
      // 组件懒加载：只有真正访问该页面时才下载对应 JS 代码，减小首屏体积
      component: () => import('@/views/LoginView.vue'),
      meta: { requiresAuth: false }   // 元信息：无需登录即可访问
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { requiresAuth: false }
    },
    // ---------- 主布局：除登录注册外的所有页面都嵌套在 Layout 内 ----------
    {
      path: '/',
      name: 'layout',
      component: () => import('@/views/LayoutView.vue'),
      redirect: '/home',              // 访问根路径时重定向到首页
      children: [
        {
          path: '/home',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
          meta: { requiresAuth: false }   // 首页对游客开放
        },
        {
          path: '/items',
          name: 'items',
          component: () => import('@/views/ItemListView.vue'),
          meta: { requiresAuth: false }   // 物品列表对游客开放浏览
        },
        {
          path: '/items/publish',
          name: 'item-publish',
          component: () => import('@/views/ItemPublishView.vue'),
          meta: { requiresAuth: true }    // 发布物品必须登录
        },
        {
          path: '/items/:id',
          name: 'item-detail',
          component: () => import('@/views/ItemDetailView.vue'),
          meta: { requiresAuth: false }   // 详情页游客可看（:id 为动态路由参数）
        },
        {
          path: '/claims',
          name: 'claims',
          component: () => import('@/views/ClaimListView.vue'),
          meta: { requiresAuth: true }    // 认领管理涉及个人数据，必须登录
        },
        {
          path: '/feedback',
          name: 'feedback',
          component: () => import('@/views/FeedbackView.vue'),
          // 未写 requiresAuth 字段视为不限制
        },
        {
          path: '/admin',
          name: 'admin',
          component: () => import('@/views/AdminView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true }   // 双重校验：登录 + 管理员角色
        },
        {
          path: '/point-admin',
          name: 'point-admin',
          component: () => import('@/views/PointAdminView.vue'),
          meta: { requiresAuth: true, requiresPointAdmin: true }   // 投放点管理员专属
        },
        {
          path: '/archive',
          name: 'archive',
          component: () => import('@/views/ArchiveView.vue'),
          meta: { requiresAuth: true, requiresPointAdmin: true }   // 归档页也是投放点管理员权限
        },
        {
          path: '/profile',
          name: 'profile',
          component: () => import('@/views/ProfileView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/certificates',
          name: 'certificates',
          component: () => import('@/views/CertificateView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/notices',
          name: 'notices',
          component: () => import('@/views/LostNoticeListView.vue'),
          meta: { requiresAuth: false }
        },
        {
          path: '/notices/:id',
          name: 'notice-detail',
          component: () => import('@/views/LostNoticeDetailView.vue'),
          meta: { requiresAuth: false }
        }
      ]
    }
  ]
})

// ==================== 全局前置守卫：每次路由跳转前都会执行 ====================
// 作用：根据目标路由的 meta 要求与当前用户登录态/角色，决定放行还是重定向。
// 这是防止用户手动输入 URL 越权访问的关键机制。
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  // 规则 1：需要登录的页面，未登录则跳转到登录页
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else
  // 规则 2：需要系统管理员角色的页面，普通用户访问则踢回首页
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next('/home')
  } else
  // 规则 3：投放点管理员页面，允许“投放点管理员或系统管理员”进入
  if (to.meta.requiresPointAdmin && !(userStore.isPointAdmin || userStore.isAdmin)) {
    next('/home')
  } else
  // 规则 4：标记 forbidsAdmin 的页面（普通用户功能）禁止管理员进入
  if (to.meta.forbidsAdmin && (userStore.isAdmin || userStore.isPointAdmin)) {
    next('/home')
  } else
  // 规则 5：已登录用户再访问登录/注册页没有意义，重定向回首页
  if ((to.path === '/login' || to.path === '/register') && userStore.isLoggedIn) {
    next('/home')
  } else {
    // 所有规则都通过，正常放行
    next()
  }
})

export default router
