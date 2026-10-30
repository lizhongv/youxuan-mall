import { ElMessage, ElMessageBox } from 'element-plus'

/** 危险操作二次确认；用户取消返回 false，不抛异常 */
export async function confirm(message: string, title = '操作确认', type: 'warning' | 'info' = 'warning'): Promise<boolean> {
  try {
    await ElMessageBox.confirm(message, title, {
      type,
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      draggable: true,
    })
    return true
  } catch {
    return false
  }
}

/** 删除确认 + 执行 + 提示 */
export async function confirmDelete(name: string, action: () => Promise<unknown>): Promise<boolean> {
  const ok = await confirm(`确定删除「${name}」吗？删除后不可恢复。`, '删除确认')
  if (!ok) return false
  await action()
  ElMessage.success('删除成功')
  return true
}

/** 弹出输入框（备注、拒绝原因等） */
export async function promptText(
  title: string,
  options: { placeholder?: string; required?: boolean; initial?: string; textarea?: boolean } = {},
): Promise<string | null> {
  try {
    const { value } = await ElMessageBox.prompt('', title, {
      inputPlaceholder: options.placeholder,
      inputValue: options.initial,
      inputType: options.textarea ? 'textarea' : 'text',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValidator: (v: string) => (options.required && !v?.trim() ? '请填写内容' : true),
    })
    return value ?? ''
  } catch {
    return null
  }
}

export const toast = {
  success: (msg = '操作成功') => ElMessage.success(msg),
  warning: (msg: string) => ElMessage.warning(msg),
  error: (msg: string) => ElMessage.error(msg),
}
