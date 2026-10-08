import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      name: 'layout',
      component: () => import('@/views/LayoutView.vue'),
      redirect: '/home',
      children: [
        {
          path: '/home',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
          meta: { requiresAuth: false }
        },
        {
          path: '/items',
          name: 'items',
          component: () => import('@/views/ItemListView.vue'),
          meta: { requiresAuth: false }
        },
        {
          path: '/items/publish',
          name: 'item-publish',
          component: () => import('@/views/ItemPublishView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/items/:id',
          name: 'item-detail',
          component: () => import('@/views/ItemDetailView.vue'),
          meta: { requiresAuth: false }
        },
        {
          path: '/claims',
          name: 'claims',
          component: () => import('@/views/ClaimListView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/disputes',
          name: 'disputes',
          component: () => import('@/views/DisputeView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: '/admin',
          name: 'admin',
          component: () => import('@/views/AdminView.vue'),
          meta: { requiresAuth: true, requiresAdmin: true }
        },
        {
          path: '/point-admin',
          name: 'point-admin',
          component: () => import('@/views/PointAdminView.vue'),
          meta: { requiresAuth: true, requiresPointAdmin: true }
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
          meta: { requiresAuth: true, forbidsAdmin: true }
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

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next('/home')
  } else if (to.meta.requiresPointAdmin && !(userStore.isPointAdmin || userStore.isAdmin)) {
    next('/home')
  } else if (to.meta.forbidsAdmin && (userStore.isAdmin || userStore.isPointAdmin)) {
    next('/home')
  } else if ((to.path === '/login' || to.path === '/register') && userStore.isLoggedIn) {
    next('/home')
  } else {
    next()
  }
})

export default router
