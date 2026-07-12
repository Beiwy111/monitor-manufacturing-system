-- 采购模块：补充多家供应商，并按物料类型分配默认供应商
USE display_manufacturing;

INSERT INTO supplier (supplier_no, supplier_name, contact_person, contact_phone, supply_materials, status, created_at, updated_at)
SELECT 'SUP002', '深圳光电材料有限公司', '孙经理', '13700001001', 'LCD/OLED 显示面板', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM supplier WHERE supplier_no = 'SUP002');

INSERT INTO supplier (supplier_no, supplier_name, contact_person, contact_phone, supply_materials, status, created_at, updated_at)
SELECT 'SUP003', '东莞背光科技股份', '钱工', '13700001002', 'LED 背光模组', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM supplier WHERE supplier_no = 'SUP003');

INSERT INTO supplier (supplier_no, supplier_name, contact_person, contact_phone, supply_materials, status, created_at, updated_at)
SELECT 'SUP004', '苏州芯片代理商', '郑销售', '13700001003', '驱动 IC / TCON 芯片', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM supplier WHERE supplier_no = 'SUP004');

INSERT INTO supplier (supplier_no, supplier_name, contact_person, contact_phone, supply_materials, status, created_at, updated_at)
SELECT 'SUP005', '惠州电路板厂', '冯厂长', '13700001004', 'PCB 主板 / 主控板', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM supplier WHERE supplier_no = 'SUP005');

INSERT INTO supplier (supplier_no, supplier_name, contact_person, contact_phone, supply_materials, status, created_at, updated_at)
SELECT 'SUP006', '惠州精密五金', '周采购', '13700001005', '铝合金边框 / 结构件', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM supplier WHERE supplier_no = 'SUP006');

INSERT INTO supplier (supplier_no, supplier_name, contact_person, contact_phone, supply_materials, status, created_at, updated_at)
SELECT 'SUP007', '东莞电源科技', '吴经理', '13700001006', '电源适配器', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM supplier WHERE supplier_no = 'SUP007');

-- 显示面板 → 深圳光电
UPDATE material SET supplier_id = (SELECT supplier_id FROM supplier WHERE supplier_no = 'SUP002' LIMIT 1)
WHERE material_type = 'RAW' AND (material_code LIKE 'MAT-P%' OR material_name LIKE '%面板%' OR material_name LIKE '%LCD%' OR material_name LIKE '%OLED%');

-- 背光模组 → 东莞背光
UPDATE material SET supplier_id = (SELECT supplier_id FROM supplier WHERE supplier_no = 'SUP003' LIMIT 1)
WHERE material_type = 'RAW' AND (material_code LIKE 'MAT-B%' OR material_name LIKE '%背光%');

-- 驱动芯片 → 苏州芯片
UPDATE material SET supplier_id = (SELECT supplier_id FROM supplier WHERE supplier_no = 'SUP004' LIMIT 1)
WHERE material_type = 'RAW' AND (material_code IN ('MAT-M03', 'MAT-003') OR material_name LIKE '%芯片%' OR material_name LIKE '%驱动%');

-- 主控板 / PCB → 惠州电路板
UPDATE material SET supplier_id = (SELECT supplier_id FROM supplier WHERE supplier_no = 'SUP005' LIMIT 1)
WHERE material_type = 'RAW' AND (material_code LIKE 'MAT-M0%' OR material_code = 'MAT-005' OR material_name LIKE '%主控%' OR material_name LIKE '%PCB%' OR material_name LIKE '%主板%')
  AND material_code NOT IN ('MAT-M03', 'MAT-003');

-- 边框结构件 → 惠州精密五金
UPDATE material SET supplier_id = (SELECT supplier_id FROM supplier WHERE supplier_no = 'SUP006' LIMIT 1)
WHERE material_type = 'RAW' AND (material_code LIKE 'MAT-S0%' AND material_code != 'MAT-S02' OR material_code = 'MAT-004' OR material_name LIKE '%边框%');

-- 电源适配器 → 东莞电源
UPDATE material SET supplier_id = (SELECT supplier_id FROM supplier WHERE supplier_no = 'SUP007' LIMIT 1)
WHERE material_type = 'RAW' AND (material_code = 'MAT-S02' OR material_code = 'MAT-006' OR material_name LIKE '%电源%' OR material_name LIKE '%适配器%');

-- 兜底：仍未分配供应商的原材料仍挂华南显示科技
UPDATE material SET supplier_id = (SELECT supplier_id FROM supplier WHERE supplier_no = 'SUP001' LIMIT 1)
WHERE material_type = 'RAW' AND supplier_id IS NULL;
