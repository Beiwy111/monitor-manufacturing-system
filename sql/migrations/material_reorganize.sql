-- 物料归类重组（在已有数据库上执行，无需重建）
-- 将物料按 4 类组装部件重新命名，并补充 23.8/27 寸规格与 BOM

USE display_manufacturing;

-- 1. 更新现有物料编码与名称
UPDATE material SET material_code='MAT-P01', material_name='15.6寸 IPS面板', specification='显示面板 · 1920x1080' WHERE material_id=1;
UPDATE material SET material_code='MAT-B01', material_name='15.6寸 LED背光', specification='背光模组 · 标准款' WHERE material_id=2;
UPDATE material SET material_code='MAT-M03', material_name='TCON驱动芯片', specification='主控电路 · 通用驱动' WHERE material_id=3;
UPDATE material SET material_code='MAT-S01', material_name='商用铝合金边框', specification='结构附件 · 商用款' WHERE material_id=4;
UPDATE material SET material_code='MAT-M01', material_name='商用主控板', specification='主控电路 · 商用款' WHERE material_id=5;
UPDATE material SET material_code='MAT-S02', material_name='19V电源适配器', specification='结构附件 · 通用电源' WHERE material_id=6;

-- 2. 补充新型号物料（若已存在则跳过）
INSERT IGNORE INTO material (material_id, material_code, material_name, material_type, specification, unit, safety_stock, standard_cost, status, created_at, updated_at) VALUES
(10,'MAT-P02','23.8寸 IPS面板','RAW','显示面板 · 2560x1440','片',400,420,1,NOW(),NOW()),
(11,'MAT-B02','23.8寸 LED背光','RAW','背光模组 · 电竞款','套',250,120,1,NOW(),NOW()),
(12,'MAT-M02','电竞主控板','RAW','主控电路 · 144Hz','块',300,95,1,NOW(),NOW()),
(13,'MAT-P03','27寸 OLED面板','RAW','显示面板 · 3840x2160','片',200,680,1,NOW(),NOW()),
(14,'MAT-B03','27寸 Mini-LED背光','RAW','背光模组 · HDR款','套',180,180,1,NOW(),NOW()),
(15,'MAT-M04','4K主控板','RAW','主控电路 · 4K HDR','块',200,120,1,NOW(),NOW()),
(16,'MAT-S03','电竞轻量化边框','RAW','结构附件 · 电竞款','套',150,55,1,NOW(),NOW());

-- 3. 重建 BOM（先清空再插入）
DELETE FROM bom;

INSERT INTO bom (parent_material_id, child_material_id, quantity, loss_rate, version_no, effective_date, status, remark, created_at, updated_at) VALUES
(7,1,1,0.02,'V1.0','2026-01-01',1,'显示面板',NOW(),NOW()),
(7,2,1,0.01,'V1.0','2026-01-01',1,'背光模组',NOW(),NOW()),
(7,5,1,0.02,'V1.0','2026-01-01',1,'主控电路',NOW(),NOW()),
(7,3,2,0.005,'V1.0','2026-01-01',1,'主控电路',NOW(),NOW()),
(7,4,1,0.01,'V1.0','2026-01-01',1,'结构附件',NOW(),NOW()),
(7,6,1,0,'V1.0','2026-01-01',1,'结构附件',NOW(),NOW()),
(8,10,1,0.02,'V1.0','2026-01-01',1,'显示面板',NOW(),NOW()),
(8,11,1,0.01,'V1.0','2026-01-01',1,'背光模组',NOW(),NOW()),
(8,12,1,0.02,'V1.0','2026-01-01',1,'主控电路',NOW(),NOW()),
(8,3,3,0.005,'V1.0','2026-01-01',1,'主控电路',NOW(),NOW()),
(8,16,1,0.01,'V1.0','2026-01-01',1,'结构附件',NOW(),NOW()),
(8,6,1,0,'V1.0','2026-01-01',1,'结构附件',NOW(),NOW()),
(9,13,1,0.02,'V1.0','2026-01-01',1,'显示面板',NOW(),NOW()),
(9,14,1,0.01,'V1.0','2026-01-01',1,'背光模组',NOW(),NOW()),
(9,15,1,0.02,'V1.0','2026-01-01',1,'主控电路',NOW(),NOW()),
(9,3,4,0.005,'V1.0','2026-01-01',1,'主控电路',NOW(),NOW()),
(9,16,1,0.01,'V1.0','2026-01-01',1,'结构附件',NOW(),NOW()),
(9,6,1,0,'V1.0','2026-01-01',1,'结构附件',NOW(),NOW());

-- 4. 补充新型号库存（若不存在）
INSERT IGNORE INTO inventory (material_id, warehouse_code, warehouse_name, location_code, batch_no, quantity_on_hand, quantity_available, quantity_locked, inventory_status, last_transaction_at, created_at, updated_at) VALUES
(10,'WH-01','原材料仓','A-01-02','BATCH-LCD-238',600,500,100,'NORMAL',NOW(),NOW(),NOW()),
(11,'WH-01','原材料仓','A-02-02','BATCH-BL-238',450,370,80,'NORMAL',NOW(),NOW(),NOW()),
(12,'WH-01','原材料仓','A-05-02','BATCH-PCB-GAME',350,290,60,'NORMAL',NOW(),NOW(),NOW()),
(13,'WH-01','原材料仓','A-01-03','BATCH-OLED-27',280,230,50,'NORMAL',NOW(),NOW(),NOW()),
(14,'WH-01','原材料仓','A-02-03','BATCH-MLED-27',220,180,40,'NORMAL',NOW(),NOW(),NOW()),
(15,'WH-01','原材料仓','A-05-03','BATCH-PCB-4K',180,150,30,'NORMAL',NOW(),NOW(),NOW()),
(16,'WH-01','原材料仓','A-04-02','BATCH-FR-GAME',320,280,40,'NORMAL',NOW(),NOW(),NOW());
