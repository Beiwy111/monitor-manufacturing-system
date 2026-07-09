/**
 * 首页图片（来自 frontend/picture）
 *
 * 替换方式：覆盖 frontend/picture 下对应文件，或修改下方 import 路径。
 * 也可改用 public/images/capability-1.jpg 等形式：
 *   import imgPlan from '/images/capability-1.jpg'  // 需放到 public/images/
 */
import heroBg from '@picture/1.jpg'
import heroVideo from '@picture/dynamic.mp4'
import imgWarehouse from '@picture/1.jpg'
import imgProduction from '@picture/2.jpg'
import imgQuality from '@picture/3.jpg'
import imgDelivery from '@picture/4.jpg'
import imgPlanning from '@picture/5.jpg'
import imgDevice from '@picture/6.jpg'
import imgAftersale from '@picture/7.jpg'

export const homeImages = {
  heroBg,
  heroVideo,
  capabilities: {
    planning: imgPlanning,    // 能力1：订单与计划 → picture/5.jpg
    production: imgProduction, // 能力2：派工报工 → picture/2.jpg
    quality: imgQuality,       // 能力3：质量检验 → picture/3.jpg
    warehouse: imgWarehouse,   // 能力4：仓储物流 → picture/1.jpg
    device: imgDevice,         // 能力5：设备安灯 → picture/6.jpg
    delivery: imgDelivery,       // 能力6：发货追溯 → picture/4.jpg（可换 7.jpg）
    aftersale: imgAftersale
  }
}

export const productShowcase = [
  {
    id: 'planning',
    image: imgPlanning,
    label: '计划协同',
    title: '订单审核与生产计划联动',
    desc: '客户订单审核通过后，系统自动关联产品型号、交付日期与库存状态，生成生产计划，支持多型号显示器混线排产、交期预警与产能负荷分析。',
    anchor: 'process'
  },
  {
    id: 'production',
    image: imgProduction,
    label: '生产执行',
    title: '工单派工与现场报工闭环',
    desc: '面向显示器组装产线，支持工序派工、操作员接单、开工报工、完工反馈与进度实时回写，打通计划到执行的生产闭环。',
    anchor: 'process'
  },
  {
    id: 'quality',
    image: imgQuality,
    label: '质量检验',
    title: '显示器检测与质量追溯',
    desc: '覆盖首件检验、过程检验、终检、老化测试、亮度检测、外观检测等质量环节，检验结果绑定工单、批次与产品型号，不合格品可反向追溯至原料与工序。',
    anchor: 'modules'
  },
  {
    id: 'warehouse',
    image: imgWarehouse,
    label: '仓储物流',
    title: '物料出入库与库存预警',
    desc: '管理液晶面板、电路板、背光模组、外壳、包装材料等关键物料，支持采购入库、生产领料、成品入库、发货出库和安全库存预警。',
    anchor: 'modules'
  },
  {
    id: 'device',
    image: imgDevice,
    label: '设备协同',
    title: '设备维护与安灯异常闭环',
    desc: '对贴合设备、老化测试设备、亮度检测设备、包装设备等进行状态维护，现场异常可触发安灯报警，并推送给设备、质量、仓储或生产管理人员处理。',
    anchor: 'modules'
  },
  {
    id: 'delivery',
    image: imgDelivery,
    label: '交付追溯',
    title: '发货交付与售后质量追溯',
    desc: '成品质检合格后进入发货流程，系统记录客户订单、批次、物流与交付信息。售后反馈发生时，可反向追溯订单、工单、质检记录、设备与物料批次。',
    anchor: 'roles'
  }
]
