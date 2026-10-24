import useInfo from '@/store/userInfo'

/**
 * 按钮级权限指令：v-hasPermi="'sys:user:add'"
 * 无权限时移除元素（super_admin 直通）
 */
export default {
  mounted(el, binding) {
    const permission = binding.value
    if (!permission) {
      return
    }
    const userStore = useInfo()
    const roles = userStore.userInfo.roles || []
    const permissions = userStore.userInfo.permissions || []
    const has = roles.includes('super_admin') || permissions.includes(permission)
    if (!has && el.parentNode) {
      el.parentNode.removeChild(el)
    }
  }
}
