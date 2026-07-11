-- 补全 23.8寸电竞显示器(material_id=8) 和 27寸4K显示器(material_id=9) 的 BOM
-- 与15.6寸结构相同：1片LCD面板 + 1套背光模组 + 驱动IC + 铝合金框架 + PCB主板 + 电源适配器

-- 23.8寸（parent=8）
INSERT IGNORE INTO bom (parent_material_id, child_material_id, quantity, loss_rate, version_no, effective_date, expire_date, status, remark) VALUES
(8, 1, 1.0000, 0.0200, 'V1.0', '2026-01-01', NULL, 1, '23.8寸电竞显示器BOM'),
(8, 2, 1.0000, 0.0100, 'V1.0', '2026-01-01', NULL, 1, NULL),
(8, 3, 3.0000, 0.0050, 'V1.0', '2026-01-01', NULL, 1, '每台需要3颗驱动IC'),
(8, 4, 1.0000, 0.0100, 'V1.0', '2026-01-01', NULL, 1, NULL),
(8, 5, 1.0000, 0.0200, 'V1.0', '2026-01-01', NULL, 1, NULL),
(8, 6, 1.0000, 0.0000, 'V1.0', '2026-01-01', NULL, 1, NULL);

-- 27寸4K（parent=9）
INSERT IGNORE INTO bom (parent_material_id, child_material_id, quantity, loss_rate, version_no, effective_date, expire_date, status, remark) VALUES
(9, 1, 1.0000, 0.0200, 'V1.0', '2026-01-01', NULL, 1, '27寸4K显示器BOM'),
(9, 2, 1.0000, 0.0100, 'V1.0', '2026-01-01', NULL, 1, NULL),
(9, 3, 4.0000, 0.0050, 'V1.0', '2026-01-01', NULL, 1, '27寸4K每台需要4颗驱动IC'),
(9, 4, 1.0000, 0.0100, 'V1.0', '2026-01-01', NULL, 1, NULL),
(9, 5, 1.0000, 0.0200, 'V1.0', '2026-01-01', NULL, 1, NULL),
(9, 6, 1.0000, 0.0000, 'V1.0', '2026-01-01', NULL, 1, NULL);
