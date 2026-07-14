import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import {
  resolveOperatorUsername,
  operatorBinding,
  OPERATOR_DISPLAY_NAMES
} from '@/utils/operatorWorkshop'

/** 当前登录用户在 MES 操作员流程中的有效身份（admin 映射为第 8 道工序操作员） */
export function useOperatorIdentity() {
  const userStore = useUserStore()

  const loginUsername = computed(() => userStore.userInfo?.username || '')
  const roleKey = computed(() => userStore.roleKey)
  const operatorUsername = computed(() =>
    resolveOperatorUsername(roleKey.value, loginUsername.value)
  )
  const binding = computed(() => operatorBinding(operatorUsername.value))
  const isAdminDemo = computed(() => roleKey.value === 'admin')
  const operatorDisplayName = computed(() => {
    if (isAdminDemo.value) {
      return OPERATOR_DISPLAY_NAMES[operatorUsername.value] || operatorUsername.value
    }
    return userStore.displayName
  })
  const reportPath = computed(() => '/production/report')

  return {
    loginUsername,
    roleKey,
    operatorUsername,
    binding,
    isAdminDemo,
    operatorDisplayName,
    reportPath
  }
}
