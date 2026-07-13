-- ================================================================
-- 质量模块升级 v2：半成品质检 + 成品质检 + 检测项明细
-- 目标库：display_manufacturing
-- ================================================================

USE `display_manufacturing`;

-- 1. quality_inspection 新增 inspection_category 字段
SET @col1 := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA='display_manufacturing' AND TABLE_NAME='quality_inspection'
    AND COLUMN_NAME='inspection_category');
SET @sql1 := IF(@col1=0,
  'ALTER TABLE `quality_inspection` ADD COLUMN `inspection_category` varchar(30) NOT NULL DEFAULT ''SEMI_FINISHED'' COMMENT ''质检分类'' AFTER `batch_no`',
  'SELECT 1');
PREPARE s FROM @sql1; EXECUTE s; DEALLOCATE PREPARE s;

UPDATE `quality_inspection` SET `inspection_category` = 'SEMI_FINISHED'
  WHERE `inspection_category` = '' OR `inspection_category` IS NULL;

-- 2. quality_inspection_item 表
CREATE TABLE IF NOT EXISTS `quality_inspection_item` (
  `inspection_item_id` bigint NOT NULL AUTO_INCREMENT,
  `inspection_id`      bigint NOT NULL,
  `item_code`          varchar(30)  NOT NULL DEFAULT '',
  `item_name`          varchar(100) NOT NULL DEFAULT '',
  `standard_value`     varchar(100) NOT NULL DEFAULT '',
  `measured_value`     varchar(100)          DEFAULT NULL,
  `unit`               varchar(20)           DEFAULT '',
  `result`             varchar(20)  NOT NULL DEFAULT 'PENDING',
  `defect_level`       varchar(20)           DEFAULT NULL,
  `sort_order`         int          NOT NULL DEFAULT 0,
  `remark`             varchar(500)          DEFAULT NULL,
  `created_at`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`         datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`inspection_item_id`),
  KEY `idx_inspection_id` (`inspection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质检项目明细';

-- 3. 演示质检单（4条）
INSERT INTO `quality_inspection`
  (inspection_no,work_order_id,material_id,batch_no,inspection_type,inspection_category,
   sample_quantity,qualified_quantity,unqualified_quantity,inspection_result,inspection_status,
   inspector_id,inspected_at,remark,created_at,updated_at)
SELECT 'QI202607201',1,7,'BATCH-PANEL-202607','PANEL_INSPECTION','SEMI_FINISHED',
  20,0,0,'PENDING','PENDING',5,NULL,'LCD面板来料检测待质检',NOW(),NOW()
WHERE NOT EXISTS (SELECT 1 FROM `quality_inspection` WHERE inspection_no='QI202607201');

INSERT INTO `quality_inspection`
  (inspection_no,work_order_id,material_id,batch_no,inspection_type,inspection_category,
   sample_quantity,qualified_quantity,unqualified_quantity,inspection_result,inspection_status,
   inspector_id,inspected_at,remark,created_at,updated_at)
SELECT 'QI202607202',1,7,'BATCH-BL-202607','BACKLIGHT_INSPECTION','SEMI_FINISHED',
  15,15,0,'QUALIFIED','PASSED',5,NOW(),'背光模组全部合格',NOW(),NOW()
WHERE NOT EXISTS (SELECT 1 FROM `quality_inspection` WHERE inspection_no='QI202607202');

INSERT INTO `quality_inspection`
  (inspection_no,work_order_id,material_id,batch_no,inspection_type,inspection_category,
   sample_quantity,qualified_quantity,unqualified_quantity,inspection_result,inspection_status,
   inspector_id,inspected_at,remark,created_at,updated_at)
SELECT 'QI202607203',2,8,'BATCH-FP-202607-27G','FINAL_INSPECTION','FINISHED_PRODUCT',
  10,0,0,'PENDING','PENDING',5,NULL,'DM-27-LCD-FHD 电竞成品终检待判定',NOW(),NOW()
WHERE NOT EXISTS (SELECT 1 FROM `quality_inspection` WHERE inspection_no='QI202607203');

INSERT INTO `quality_inspection`
  (inspection_no,work_order_id,material_id,batch_no,inspection_type,inspection_category,
   sample_quantity,qualified_quantity,unqualified_quantity,inspection_result,inspection_status,
   inspector_id,inspected_at,remark,created_at,updated_at)
SELECT 'QI202607204',2,8,'BATCH-FP-202607-4K','FINAL_INSPECTION','FINISHED_PRODUCT',
  5,4,1,'UNQUALIFIED','FAILED',5,NOW(),'DM-32-OLED-4K 色准ΔE超标',NOW(),NOW()
WHERE NOT EXISTS (SELECT 1 FROM `quality_inspection` WHERE inspection_no='QI202607204');

-- 4. 面板检测项（QI202607201，待检）
INSERT INTO `quality_inspection_item`(inspection_id,item_code,item_name,standard_value,unit,result,sort_order,created_at,updated_at)
SELECT qi.inspection_id,'PANEL-01','外观检查','无划痕/污点','','PENDING',1,NOW(),NOW() FROM `quality_inspection` qi WHERE qi.inspection_no='QI202607201' AND NOT EXISTS(SELECT 1 FROM `quality_inspection_item` WHERE inspection_id=qi.inspection_id AND item_code='PANEL-01');
INSERT INTO `quality_inspection_item`(inspection_id,item_code,item_name,standard_value,unit,result,sort_order,created_at,updated_at)
SELECT qi.inspection_id,'PANEL-02','坏点检测','亮点≤3，暗点≤2','个','PENDING',2,NOW(),NOW() FROM `quality_inspection` qi WHERE qi.inspection_no='QI202607201' AND NOT EXISTS(SELECT 1 FROM `quality_inspection_item` WHERE inspection_id=qi.inspection_id AND item_code='PANEL-02');
INSERT INTO `quality_inspection_item`(inspection_id,item_code,item_name,standard_value,unit,result,sort_order,created_at,updated_at)
SELECT qi.inspection_id,'PANEL-03','亮线/暗线','无连续亮线/暗线','','PENDING',3,NOW(),NOW() FROM `quality_inspection` qi WHERE qi.inspection_no='QI202607201' AND NOT EXISTS(SELECT 1 FROM `quality_inspection_item` WHERE inspection_id=qi.inspection_id AND item_code='PANEL-03');
INSERT INTO `quality_inspection_item`(inspection_id,item_code,item_name,standard_value,unit,result,sort_order,created_at,updated_at)
SELECT qi.inspection_id,'PANEL-04','偏色检测','ΔE≤3','ΔE','PENDING',4,NOW(),NOW() FROM `quality_inspection` qi WHERE qi.inspection_no='QI202607201' AND NOT EXISTS(SELECT 1 FROM `quality_inspection_item` WHERE inspection_id=qi.inspection_id AND item_code='PANEL-04');

-- 5. 背光检测项（QI202607202，已通过）
INSERT INTO `quality_inspection_item`(inspection_id,item_code,item_name,standard_value,measured_value,unit,result,sort_order,created_at,updated_at)
SELECT qi.inspection_id,'BL-01','亮度均匀性','≥80%','88','%','PASSED',1,NOW(),NOW() FROM `quality_inspection` qi WHERE qi.inspection_no='QI202607202' AND NOT EXISTS(SELECT 1 FROM `quality_inspection_item` WHERE inspection_id=qi.inspection_id AND item_code='BL-01');
INSERT INTO `quality_inspection_item`(inspection_id,item_code,item_name,standard_value,measured_value,unit,result,sort_order,created_at,updated_at)
SELECT qi.inspection_id,'BL-02','背光闪烁','≥1000Hz','1200','Hz','PASSED',2,NOW(),NOW() FROM `quality_inspection` qi WHERE qi.inspection_no='QI202607202' AND NOT EXISTS(SELECT 1 FROM `quality_inspection_item` WHERE inspection_id=qi.inspection_id AND item_code='BL-02');
INSERT INTO `quality_inspection_item`(inspection_id,item_code,item_name,standard_value,measured_value,unit,result,sort_order,created_at,updated_at)
SELECT qi.inspection_id,'BL-03','漏光检测','无明显漏光','无漏光','','PASSED',3,NOW(),NOW() FROM `quality_inspection` qi WHERE qi.inspection_no='QI202607202' AND NOT EXISTS(SELECT 1 FROM `quality_inspection_item` WHERE inspection_id=qi.inspection_id AND item_code='BL-03');

-- 6. 成品终检项（QI202607203，27寸待检）
INSERT INTO `quality_inspection_item`(inspection_id,item_code,item_name,standard_value,unit,result,sort_order,created_at,updated_at)
SELECT qi.inspection_id,'FP-01','老化测试','8h无异常','h','PENDING',1,NOW(),NOW() FROM `quality_inspection` qi WHERE qi.inspection_no='QI202607203' AND NOT EXISTS(SELECT 1 FROM `quality_inspection_item` WHERE inspection_id=qi.inspection_id AND item_code='FP-01');
INSERT INTO `quality_inspection_item`(inspection_id,item_code,item_name,standard_value,unit,result,sort_order,created_at,updated_at)
SELECT qi.inspection_id,'FP-02','亮点数','≤3个','个','PENDING',2,NOW(),NOW() FROM `quality_inspection` qi WHERE qi.inspection_no='QI202607203' AND NOT EXISTS(SELECT 1 FROM `quality_inspection_item` WHERE inspection_id=qi.inspection_id AND item_code='FP-02');
INSERT INTO `quality_inspection_item`(inspection_id,item_code,item_name,standard_value,unit,result,sort_order,created_at,updated_at)
SELECT qi.inspection_id,'FP-05','亮度测试','≥400','cd/m²','PENDING',5,NOW(),NOW() FROM `quality_inspection` qi WHERE qi.inspection_no='QI202607203' AND NOT EXISTS(SELECT 1 FROM `quality_inspection_item` WHERE inspection_id=qi.inspection_id AND item_code='FP-05');
INSERT INTO `quality_inspection_item`(inspection_id,item_code,item_name,standard_value,unit,result,sort_order,created_at,updated_at)
SELECT qi.inspection_id,'FP-10','HDMI接口','正常输出','','PENDING',10,NOW(),NOW() FROM `quality_inspection` qi WHERE qi.inspection_no='QI202607203' AND NOT EXISTS(SELECT 1 FROM `quality_inspection_item` WHERE inspection_id=qi.inspection_id AND item_code='FP-10');
INSERT INTO `quality_inspection_item`(inspection_id,item_code,item_name,standard_value,unit,result,sort_order,created_at,updated_at)
SELECT qi.inspection_id,'FP-13','外观检测','无划痕/边框平整','','PENDING',13,NOW(),NOW() FROM `quality_inspection` qi WHERE qi.inspection_no='QI202607203' AND NOT EXISTS(SELECT 1 FROM `quality_inspection_item` WHERE inspection_id=qi.inspection_id AND item_code='FP-13');

-- 7. 4K色准检测项（QI202607204，色准FAILED）
INSERT INTO `quality_inspection_item`(inspection_id,item_code,item_name,standard_value,measured_value,unit,result,sort_order,created_at,updated_at)
SELECT qi.inspection_id,'FP-06','sRGB覆盖率','≥99%','99.2','%','PASSED',6,NOW(),NOW() FROM `quality_inspection` qi WHERE qi.inspection_no='QI202607204' AND NOT EXISTS(SELECT 1 FROM `quality_inspection_item` WHERE inspection_id=qi.inspection_id AND item_code='FP-06');
INSERT INTO `quality_inspection_item`(inspection_id,item_code,item_name,standard_value,measured_value,unit,result,sort_order,created_at,updated_at)
SELECT qi.inspection_id,'FP-07','DCI-P3覆盖率','≥90%','95.1','%','PASSED',7,NOW(),NOW() FROM `quality_inspection` qi WHERE qi.inspection_no='QI202607204' AND NOT EXISTS(SELECT 1 FROM `quality_inspection_item` WHERE inspection_id=qi.inspection_id AND item_code='FP-07');
INSERT INTO `quality_inspection_item`(inspection_id,item_code,item_name,standard_value,measured_value,unit,result,defect_level,sort_order,created_at,updated_at)
SELECT qi.inspection_id,'FP-08','色准ΔE','≤2','3.8','ΔE','FAILED','MAJOR',8,NOW(),NOW() FROM `quality_inspection` qi WHERE qi.inspection_no='QI202607204' AND NOT EXISTS(SELECT 1 FROM `quality_inspection_item` WHERE inspection_id=qi.inspection_id AND item_code='FP-08');
INSERT INTO `quality_inspection_item`(inspection_id,item_code,item_name,standard_value,measured_value,unit,result,sort_order,created_at,updated_at)
SELECT qi.inspection_id,'FP-09','刷新率','60Hz','60','Hz','PASSED',9,NOW(),NOW() FROM `quality_inspection` qi WHERE qi.inspection_no='QI202607204' AND NOT EXISTS(SELECT 1 FROM `quality_inspection_item` WHERE inspection_id=qi.inspection_id AND item_code='FP-09');

-- 8. 4K不良品记录
INSERT INTO `nonconforming_product`
  (nonconforming_no,inspection_id,work_order_id,material_id,batch_no,
   defect_type,defect_description,quantity,severity,handle_method,handle_status,
   registered_at,created_at,updated_at)
SELECT 'NC202607201',qi.inspection_id,qi.work_order_id,qi.material_id,qi.batch_no,
  '色准超标','色准ΔE=3.8超出≤2标准，需色彩校准返工',1,'MAJOR','REWORK','PENDING',NOW(),NOW(),NOW()
FROM `quality_inspection` qi WHERE qi.inspection_no='QI202607204'
AND NOT EXISTS(SELECT 1 FROM `nonconforming_product` WHERE nonconforming_no='NC202607201');
