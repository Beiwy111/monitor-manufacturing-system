import mysql.connector

conn = mysql.connector.connect(
    host='localhost', port=3306,
    user='root', password='gxr051201',
    database='display_manufacturing',
    charset='utf8mb4'
)
cur = conn.cursor()

# case_level 枚举: GENERAL / IMPORTANT / URGENT
# case_status 枚举: OPEN / TRACING / PROCESSING / CLOSED / CANCELLED
# problem_type: 自由文本(中文)，无约束

cases = [
    # (case_no, order_id, material_id, batch_no, qi_id,
    #  customer_name, contact_name, contact_phone,
    #  problem_description, problem_type, case_level, case_status,
    #  handle_result, trace_result,
    #  offset_open, offset_processing, offset_resolved, offset_closed)
    # offset 单位: 天(负数=前N天), None=不填

    # 案例1 待受理 色彩问题 IMPORTANT级
    ('AS202607001', 36, 8, 'BATCH-FP-202607-23G', 17,
     '北京星辰科技有限公司', '王总监', '13800001111',
     '收到的23.8寸竞技显示器色彩偏暖，颜色明显偏红，与规格标注sRGB 99%不符',
     '色彩问题', 'IMPORTANT', 'OPEN',
     None, None,
     -1, None, None, None),

    # 案例2 处理中 外观划痕 GENERAL级
    ('AS202607002', 37, 8, 'BATCH-FP-202607-23G', None,
     '上海联创电子科技', '李工', '13900002222',
     '23.8寸显示器边框左下角有约3cm划痕，包装箱完好，怀疑出厂前外观检测漏检',
     '外观损伤', 'GENERAL', 'PROCESSING',
     None, None,
     -2, -1, None, None),

    # 案例3 已解决 追溯结论:质检漏判 IMPORTANT级
    ('AS202607003', 38, 9, 'BATCH-FP-202607-27K', 18,
     '深圳明图显示器采购部', '陈主管', '13700003333',
     '27寸4K显示器局部区域出现亮线，怀疑LCD面板存在缺陷',
     '显示缺陷', 'IMPORTANT', 'PROCESSING',
     '已对该批次面板重新质检，发现存在2条亮线缺陷未完全检出，已安排退换货并向供应商申请质量赔偿',
     '追溯至质检单QI202607002，面板检测项（亮线/暗线）判定待检，实测值未录入，系质检漏判。已联动质量改进措施。',
     -5, -4, None, None),

    # 案例4 已关闭 接口故障 GENERAL级 确认为线缆问题
    ('AS202607004', 36, 8, 'BATCH-FP-202607-23G', None,
     '广州绿洲办公采购中心', '赵助理', '13600004444',
     'HDMI接口偶发无法识别，重新插拔后恢复正常，怀疑接口接触不良',
     '接口故障', 'GENERAL', 'CLOSED',
     '经测试为线缆问题，非显示器本体故障，已指导客户更换HDMI线解决',
     None,
     -10, -9, -8, -7),

    # 案例5 【分诊→技术支持】DP兼容性 GENERAL级 不触发RCA
    ('AS202607005', 37, 9, 'BATCH-FP-202607-27K', None,
     '杭州创智办公设备有限公司', '周工', '13500005555',
     '新采购的27寸4K显示器接DP线后分辨率只能到1080P，换HDMI可以到4K，怀疑DP接口或驱动有问题，显卡是RTX 3060',
     '接口故障', 'GENERAL', 'OPEN',
     None, None,
     -1, None, None, None),

    # 案例6 【分诊→物流损坏】包装破损 GENERAL级 走换货
    ('AS202607006', 36, 8, 'BATCH-FP-202607-23G', None,
     '成都西部电子采购中心', '刘经理', '13400006666',
     '收到货物后外包装箱有明显挤压变形，开箱后显示器屏幕左上角有横向裂痕，疑为运输途中碰撞所致',
     '外观损伤', 'IMPORTANT', 'OPEN',
     None, None,
     -3, None, None, None),

    # 案例7 【分诊→批次质量风险】20台3台亮点+质检18 评分破60触发RCA
    ('AS202607007', 38, 9, 'BATCH-FP-202607-27K', 18,
     '武汉光谷科技园采购部', '孙总', '13300007777',
     '本次采购的20台4K显示器已发现3台存在屏幕亮点，其中2台右下角固定亮点，1台中央闪烁点，影响正常使用',
     '坏点/亮点', 'URGENT', 'OPEN',
     None, None,
     -2, None, None, None),

    # 案例8 【分诊→业务咨询】询问报价交期 直接分流客服
    ('AS202607008', None, 9, None, None,
     '南京云峰教育集团', '采购专员赵', '13200008888',
     '我们想采购100台27寸显示器用于高校机房，请问现在有现货吗？最快什么时候能到货？能否给一下批量采购报价',
     '业务咨询', 'GENERAL', 'OPEN',
     None, None,
     0, None, None, None),

    # 案例9 【追溯根因→设备异常】已解决，与案例3质检漏判形成对比
    ('AS202607009', 37, 8, 'BATCH-FP-202607-23G', 17,
     '西安智慧显示技术公司', '技术主管林', '13100009999',
     '整批23.8寸竞技显示器使用2周后陆续出现屏幕闪烁，重启可短暂恢复，目前已影响5台，怀疑背光驱动板存在问题',
     '显示缺陷', 'URGENT', 'PROCESSING',
     '根因确认为背光贴附工序设备在生产该批次期间发生多次紧急报警（背光压力异常），导致部分背光模组贴附不均匀。已对同批次产品全检，退换受影响12台，设备完成维护校准。',
     '追溯路径：工单→设备多次URGENT报警（背光压力异常）→质检单QI202607001→批次BATCH-FP-202607-23G。根因分类：设备异常，责任部门：生产/设备。',
     -8, -7, None, None),

    # 案例10 【追溯根因→供应商批次】5台全部色偏绿 指向LCD供应商
    ('AS202607010', 39, 9, 'BATCH-FP-202607-27K', 18,
     '北京视界科技园', '采购部张总', '13900010101',
     '本批次4K显示器已收到5台，全部出现色彩偏绿问题，白色背景下绿色偏移明显，怀疑LCD面板存在色彩一致性问题',
     '色彩问题', 'IMPORTANT', 'PROCESSING',
     None, None,
     -6, -5, None, None),
]

sql = """
INSERT INTO after_sales_case
  (case_no, order_id, material_id, batch_no, quality_inspection_id,
   customer_name, contact_name, contact_phone,
   problem_description, problem_type, case_level, case_status,
   handle_result, trace_result,
   opened_at, processing_at, resolved_at, closed_at,
   created_at, updated_at)
VALUES (%s, %s, %s, %s, %s,
        %s, %s, %s,
        %s, %s, %s, %s,
        %s, %s,
        DATE_ADD(NOW(), INTERVAL %s DAY),
        CASE WHEN %s IS NULL THEN NULL ELSE DATE_ADD(NOW(), INTERVAL %s DAY) END,
        CASE WHEN %s IS NULL THEN NULL ELSE DATE_ADD(NOW(), INTERVAL %s DAY) END,
        CASE WHEN %s IS NULL THEN NULL ELSE DATE_ADD(NOW(), INTERVAL %s DAY) END,
        DATE_ADD(NOW(), INTERVAL %s DAY), NOW())
ON DUPLICATE KEY UPDATE updated_at=updated_at
"""

ok = 0
for c in cases:
    (case_no, order_id, material_id, batch_no, qi_id,
     customer_name, contact_name, contact_phone,
     problem_desc, problem_type, case_level, case_status,
     handle_result, trace_result,
     off_open, off_proc, off_res, off_closed) = c

    params = (
        case_no, order_id, material_id, batch_no, qi_id,
        customer_name, contact_name, contact_phone,
        problem_desc, problem_type, case_level, case_status,
        handle_result, trace_result,
        off_open,
        off_proc, off_proc,
        off_res,  off_res,
        off_closed, off_closed,
        off_open,
    )
    try:
        cur.execute(sql, params)
        ok += 1
        print(f"  OK  {case_no}  {customer_name}")
    except Exception as e:
        print(f"  ERR {case_no}: {e}")

conn.commit()
cur.close()
conn.close()
print(f"\n完成：成功插入/跳过 {ok}/{len(cases)} 条")
