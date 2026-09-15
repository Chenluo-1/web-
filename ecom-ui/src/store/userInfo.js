import { defineStore } from 'pinia'

/**
 * 登录用户信息（持久化到 sessionStorage）
 * token 另外单独存 sessionStorage 的 authorization 字段（请求拦截器读取）
 */
const useInfo = defineStore('userInfo', {
  state: () => ({
    userInfo: {
      token: '',
      user: {},
      permissions: [],
      roles: [],
      menu: [],
      routes: []
    }
  }),

  getters: {
    isSuperAdmin: state => (state.userInfo.roles || []).includes('super_admin')
  },

  actions: {
    setLogin(token, user) {
      this.userInfo.token = token
      this.userInfo.user = user
    },
    setAuth(permissions, roles) {
      this.userInfo.permissions = permissions || []
      this.userInfo.roles = roles || []
    },
    setMenu(menu) {
      this.userInfo.menu = menu || []
    },
    setRoutes(routes) {
      this.userInfo.routes = routes || []
    },
    clear() {
      this.userInfo = { token: '', user: {}, permissions: [], roles: [], menu: [], routes: [] }
    }
  },

  persist: {
    key: 'ecom-userInfo',
    storage: window.sessionStorage
  }
})

export default useInfo
