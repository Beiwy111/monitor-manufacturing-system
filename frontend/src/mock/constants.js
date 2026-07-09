/** MES 业务常量 — 显示器制造场景 */

export const ORDER_STATUS = ['待审核', '已审核', '待计划', '已计划', '生产中', '已发货', '已完成', '已作废']
export const PLAN_STATUS = ['草稿', '已发布', '已提交', '执行中', '已完成', '已取消']
export const WORK_ORDER_STATUS = ['草稿', '已下达', '已派工', '生产中', '待质检', '已完成', '已取消']
export const DISPATCH_STATUS = ['已分配', '已接收', '生产中', '待质检', '已完成']
export const DEFECT_STATUS = ['待处理', '返修中', '已返修', '已报废']
export const DEFECT_SEVERITY = ['轻微', '严重']
export const DEFECT_LOCATIONS = [
  'LCD 面板', '背光模组', '主控板', '电源板', '外壳', '点亮/显示', '色彩/亮度', '包装'
]
export const REPORT_STATUS = ['已提交', '已确认', '已驳回']
export const INSPECTION_STATUS = ['待检', '合格', '不合格', '让步接收', '复检']
export const STOCK_STATUS = ['正常', '冻结', '待检', '隔离']
export const ALARM_STATUS = ['已上报', '已接收', '处理中', '已关闭']
export const AFTERSALE_STATUS = ['已创建', '追溯中', '处理中', '已关闭']
export const COST_STATUS = ['草稿', '已确认', '已导出']
export const PURCHASE_STATUS = ['草稿', '已下达', '部分到货', '已到货', '已取消']
export const DELIVERY_STATUS = ['待出库', '已出库', '运输中', '已签收']

export const PRODUCT_MODELS = [
  'DM-27-LCD-FHD', 'DM-32-OLED-4K', 'DM-24-LCD-FHD', 'DM-34-OLED-UWQHD'
]

export const DISPLAY_MATERIALS = [
  { code: 'LCD-PANEL', name: 'LCD 面板', unit: '片' },
  { code: 'OLED-PANEL', name: 'OLED 面板', unit: '片' },
  { code: 'BL-MODULE', name: '背光模组', unit: '套' },
  { code: 'MAIN-BOARD', name: '主控板', unit: '块' },
  { code: 'PWR-BOARD', name: '电源板', unit: '块' },
  { code: 'HOUSING', name: '外壳', unit: '套' },
  { code: 'STAND', name: '支架', unit: '套' },
  { code: 'PACK-BOX', name: '包装箱', unit: '个' }
]

export const QC_ITEMS = [
  '外观检查', '点亮测试', '亮度检测', '色彩检测', '坏点检测', '老化测试', '包装检查'
]

export const QC_TYPES = ['首件检验', '过程检验', '终检', '复检']

export const PROCESS_STEPS = [
  '面板贴合', '背光组装', '主控安装', '电源安装', '外壳装配',
  '点亮测试', '亮度检测', '色彩检测', '坏点检测', '老化测试', '外观检查', '成品包装'
]

export const EQUIPMENT_TYPES = [
  '装配线', '点亮测试设备', '亮度检测设备', '老化测试架', '包装设备', '贴合设备'
]

export const STATUS_COLOR = {
  待审核: 'warning', 已审核: 'primary', 待计划: 'warning', 已计划: 'primary', 生产中: 'primary',
  已发货: 'success', 已完成: 'success', 已作废: 'info',
  草稿: 'info', 已发布: 'primary', 执行中: 'primary', 已取消: 'info',
  已下达: 'primary', 已派工: 'primary', 待质检: 'warning',
  已分配: 'warning', 已接收: 'primary', 待质检: 'warning',
  待处理: 'warning', 返修中: 'primary', 已返修: 'success', 已报废: 'info',
  已提交: 'warning', 已确认: 'success', 已驳回: 'danger',
  待检: 'warning', 合格: 'success', 不合格: 'danger', 让步接收: 'warning', 复检: 'warning',
  正常: 'success', 冻结: 'info', 待检: 'warning', 隔离: 'danger',
  已上报: 'danger', 处理中: 'primary', 已关闭: 'info',
  已创建: 'warning', 追溯中: 'primary',
  部分到货: 'warning', 已到货: 'success',
  待出库: 'warning', 已出库: 'primary', 运输中: 'primary', 已签收: 'success',
  已导出: 'success'
}
