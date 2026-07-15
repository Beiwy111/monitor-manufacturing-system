-- 修正 BOM remark 中被误当作「组件类别」的产品说明文字
-- mysql --default-character-set=utf8mb4 -uroot -p display_manufacturing < sql/fixes/fix_bom_assembly_remark.sql

USE display_manufacturing;
SET NAMES utf8mb4;

UPDATE bom b
JOIN material child ON child.material_id = b.child_material_id
SET b.remark = CASE
  WHEN child.material_code LIKE 'MAT-P%' OR child.material_name LIKE '%面板%' THEN '显示面板'
  WHEN child.material_code LIKE 'MAT-B%' OR child.material_name LIKE '%背光%' THEN '背光模组'
  WHEN child.material_code LIKE 'MAT-M%' OR child.material_name LIKE '%主控%' OR child.material_name LIKE '%驱动%' OR child.material_name LIKE '%芯片%' THEN '主控电路'
  WHEN child.material_code LIKE 'MAT-S%' OR child.material_name LIKE '%边框%' OR child.material_name LIKE '%电源%' THEN '结构附件'
  ELSE b.remark
END
WHERE b.remark IS NULL
   OR b.remark = ''
   OR b.remark LIKE '%BOM%'
   OR b.remark LIKE '%?%'
   OR b.remark LIKE '%需要%'
   OR b.remark LIKE '%每台%';
