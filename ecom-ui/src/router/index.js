import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { ElMessage } from 'element-plus'
import useInfo from '@/store/userInfo'
import loginHttp from '@/api/loginHttp'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: () => import('@/views/Login') },
  {
    path: '/Menu',
    name: 'Menu',
    component: () => import('@/views/Menu'),
    redirect: '/home',
    children: [
      { path: '/home', name: 'home', component: () => import('@/components/home') },
      { path: '/product/edit', name: 'productEdit', component: () => import('@/components/productEdit') },
      { path: '/order/confirm', name: 'orderConfirm', component: () => import('@/components/orderConfirm') },
      { path: '/order/pay', name: 'orderPay', component: () => import('@/components/orderPay') },
      { path: '/order/detail', name: 'orderDetail', component: () => import('@/components/orderDetail') }
    ]
  },
  { path: '/404', name: 'NotFound', component: () => import('@/views/404') }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

let routesLoaded = false

router.beforeEach(async (to, from, next) => {
  NProgress.start()
  const userStore = useInfo()
  const token = window.sessionStorage.getItem('authorization')

  // 登录页
  if (to.path === '/login') {
    return token ? next('/home') : next()
  }
  // 未登录
  if (!token) {
    ElMessage.error('请先登录！')
    return next('/login')
  }
  // 已登录：首次进入拉取菜单与权限，注册动态路由
  if (!routesLoaded) {
    try {
      const menuRes = await loginHttp.getRoutes()
      const infoRes = await loginHttp.getInfo()
      const menus = menuRes.data.menus || []
      userStore.setMenu(menus)
      userStore.setAuth(infoRes.data.permissions, infoRes.data.roles)

      const flatRoutes = []
      collectRoutes(menus, flatRoutes)
      userStore.setRoutes(flatRoutes)
      flatRoutes.forEach(route => {
        router.addRoute('Menu', {
          path: route.urlPath,
          name: route.name,
          component: () => import(`@/components/${route.component}`)
        })
      })
      routesLoaded = true
      return next({ ...to, replace: true })
    } catch (e) {
      window.sessionStorage.clear()
      userStore.clear()
      return next('/login')
    }
  }
  next()
})

router.afterEach(() => {
  NProgress.done()
})

/** 递归收集可注册为路由的菜单（有 component 的叶子；带 children 的目录只递归） */
function collectRoutes(menus, result) {
  menus.forEach(menu => {
    if (menu.children && menu.children.length > 0) {
      collectRoutes(menu.children, result)
    } else if (menu.component) {
      result.push({ urlPath: menu.urlPath, name: menu.component, component: menu.component })
    }
  })
}

/** 退出后重置动态路由标记（Menu.vue 退出登录时调用） */
export function resetRoutesLoaded() {
  routesLoaded = false
}

export default router
