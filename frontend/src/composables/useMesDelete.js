import { ElMessageBox, ElMessage } from 'element-plus'

/**
 * 统一 MES 业务记录删除确认
 * @param {object} mes - useMesStore()
 * @param {object} userStore - useUserStore()
 */
export function useMesDelete(mes, userStore) {
  async function runDelete({
    title = '确认删除',
    message = '删除后不可恢复，是否继续？',
    action,
    payload,
    onSuccess
  }) {
    if (!action || !payload) return
    await ElMessageBox.confirm(message, title, {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await mes.deleteRecord(action, payload, userStore.username, userStore.roleKey)
    ElMessage.success('已删除')
    onSuccess?.()
  }

  return { runDelete }
}
