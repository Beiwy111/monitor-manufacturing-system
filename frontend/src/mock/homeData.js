/** 首页展示数据 */

export const processSteps = [
  { key: 'order', name: '订单审核', desc: '客户订单接收、评审与交期确认' },
  { key: 'plan', name: '生产计划', desc: '按订单编制计划并下达车间' },
  { key: 'dispatch', name: '工单派工', desc: '工序派工、设备与人员分配' },
  { key: 'report', name: '生产报工', desc: '现场开工、报工与进度反馈' },
  { key: 'qc', name: '质量检验', desc: '首件、过程与终检录入' },
  { key: 'inbound', name: '成品入库', desc: '合格品入库与批次绑定' },
  { key: 'delivery', name: '发货交付', desc: '出库发运与客户签收' },
  { key: 'aftersale', name: '售后追溯', desc: '问题登记与批次反向追溯' }
]

export const projectHighlights = [
  {
    no: '01',
    title: '订单驱动生产计划',
    desc: '客户订单审核通过后自动触发计划编制，支持多型号显示器混线排产与交期预警。'
  },
  {
    no: '02',
    title: '工单派工与现场报工闭环',
    desc: '主管派工到工序与操作员，操作员接单、开工、报工，进度实时回写工单。'
  },
  {
    no: '03',
    title: '质检结果与不合格品追溯',
    desc: '检验数据与工单、批次绑定，不合格品登记后可追溯至原料批次与工序。'
  },
  {
    no: '04',
    title: '仓储出入库与库存预警',
    desc: '采购入库、生产领料、成品入库、发货出库全流程记录，低库存自动预警。'
  },
  {
    no: '05',
    title: '设备维护与安灯报警',
    desc: '设备台账、保养计划、故障维修与现场安灯联动，异常快速响应。'
  },
  {
    no: '06',
    title: '售后问题反向追溯',
    desc: '售后登记关联发货批次与质检记录，支持从客户端反向定位生产环节。'
  }
]

export const mesPreviewFeatures = [
  {
    title: '实时生产监控',
    desc: '计划达成率、工单完成数、工序进度一目了然，主管可快速定位瓶颈工序。'
  },
  {
    title: '质量数据联动',
    desc: '质检结果与工单状态同步更新，不合格品自动触发复检与隔离流程。'
  },
  {
    title: '多角色协同视图',
    desc: '操作员、质检员、仓库员、主管在同一平台按权限查看各自待办与指标。'
  },
  {
    title: '可扩展数据接口',
    desc: '预留与 ERP、WMS、设备采集系统对接能力，支持后续深度集成。'
  }
]

export const mesPreviewKpis = [
  { label: '今日计划达成率', value: '94.2%', status: 'success' },
  { label: '工单完成数', value: '18 / 24', status: 'processing' },
  { label: '质检合格率', value: '98.6%', status: 'success' },
  { label: '设备在线率', value: '96.7%', status: 'processing' }
]

export const mesWorkOrders = [
  { no: 'WO202607001', model: 'DM-27UHD', step: 'LCD 组装', plan: 500, done: 420, status: '生产中', owner: '王操作' },
  { no: 'WO202607002', model: 'DM-24FHD', step: '老化测试', plan: 300, done: 300, status: '待质检', owner: '赵操作' },
  { no: 'WO202607003', model: 'DM-32QHD', step: '终检包装', plan: 200, done: 200, status: '已入库', owner: '陈质检' },
  { no: 'WO202607004', model: 'DM-27UHD', step: '背光贴合', plan: 400, done: 180, status: '异常', owner: '李主管' }
]

export const mesQcRecords = [
  { no: 'QI202607018', model: 'DM-24FHD', type: '终检', pass: 298, fail: 2, result: '合格' },
  { no: 'QI202607019', model: 'DM-27UHD', type: '过程检', pass: 415, fail: 5, result: '合格' },
  { no: 'QI202607020', model: 'DM-32QHD', type: '首件检', pass: 0, fail: 1, result: '不合格' }
]

export const statusDistribution = [
  { label: '生产中', value: 42, color: '#1677ff' },
  { label: '待质检', value: 18, color: '#faad14' },
  { label: '已入库', value: 28, color: '#52c41a' },
  { label: '异常', value: 4, color: '#ff4d4f' }
]

export const coreModules = [
  { name: '订单管理', desc: '客户订单录入、审核、变更与交期跟踪，驱动下游生产计划。' },
  { name: '生产管理', desc: '生产计划、工单、派工、报工与进度监控，支撑车间执行。' },
  { name: '采购管理', desc: '采购申请、订单、到货验收与供应商协同管理。' },
  { name: '仓储管理', desc: '物料入库、领料出库、成品入库、库存查询与流水追溯。' },
  { name: '质量管理', desc: '检验任务、质检录入、不合格品处理与质量追溯。' },
  { name: '设备管理', desc: '设备台账、运行状态、保养维修与安灯报警处理。' },
  { name: '发货管理', desc: '发货计划、出库确认、物流信息与签收反馈。' },
  { name: '售后追溯', desc: '售后登记、客户反馈、批次追溯与处理闭环。' },
  { name: '成本结算', desc: '工单材料、人工、设备成本归集与结算报表。' },
  { name: '系统管理', desc: '用户、角色、权限、菜单配置与操作日志审计。' }
]

export const roleWorkbenches = [
  { key: 'admin', name: '系统管理员', desc: '用户、角色、权限、菜单、日志与全局看板', tasks: '权限配置、日志审计' },
  { key: 'order', name: '订单管理员', desc: '客户订单、审核、提交计划员与交付跟踪', tasks: '订单审核、提交计划' },
  { key: 'planner', name: '计划员', desc: '接收订单、编制生产计划并提交生产主管', tasks: '编制计划、提交主管' },
  { key: 'manager', name: '生产主管', desc: '接收计划、生成工单、派工与进度监控', tasks: '工单、派工、安灯' },
  { key: 'operator', name: '生产操作员', desc: '接收派工、现场报工、工序执行与安灯触发', tasks: '接单、报工' },
  { key: 'quality', name: '质检员', desc: '待检任务、检验录入、不合格品与复检处理', tasks: '检验、追溯' },
  { key: 'warehouse', name: '仓库员', desc: '采购入库、领料出库、成品入库与库存管理', tasks: '出入库' },
  { key: 'purchase', name: '采购员', desc: '采购订单、到货跟进、供应商与采购异常', tasks: '采购跟进' },
  { key: 'device', name: '设备维护人员', desc: '设备台账、报警接收、维修记录与保养计划', tasks: '维修、保养' },
  { key: 'aftersale', name: '售后人员', desc: '售后登记、客户反馈、发货查询与质量追溯', tasks: '售后处理' },
  { key: 'cost', name: '财务/成本人员', desc: '工单成本、材料人工设备成本与结算报表', tasks: '成本结算' }
]

export const opsKpis = [
  { label: '计划达成率', value: '92.6%', status: 'success', sub: '较上周 +1.2%' },
  { label: '工单进度', value: '68 在制', status: 'processing', sub: '12 单待派工' },
  { label: '质检合格率', value: '98.4%', status: 'success', sub: '本月累计' },
  { label: '设备状态', value: '18 运行', status: 'processing', sub: '1 台维护' },
  { label: '库存预警', value: '3 项', status: 'warning', sub: 'LCD 面板等' },
  { label: '安灯报警', value: '2 条', status: 'danger', sub: '1 条待处理' }
]

export const opsTodos = [
  { id: 1, title: 'WO202607003 背光工序派工', role: '生产主管', priority: '高', status: '待处理' },
  { id: 2, title: 'DM-27UHD 批次终检', role: '质检员', priority: '高', status: '进行中' },
  { id: 3, title: 'LCD 面板库存低于安全线', role: '仓库员', priority: '中', status: '预警' },
  { id: 4, title: 'SMT-02 设备故障报警', role: '设备维护', priority: '高', status: '异常' },
  { id: 5, title: 'PO202607004 到货验收', role: '采购员', priority: '中', status: '待处理' }
]

export const heroTags = [
  '显示器制造全流程',
  '质量追溯',
  '现场协同',
  '工单可视化',
  '库存预警'
]
