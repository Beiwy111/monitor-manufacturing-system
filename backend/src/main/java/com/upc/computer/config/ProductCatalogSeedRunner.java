package com.upc.computer.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时补齐 material 展示字段并确保成品 catalog ≥ 8 款
 */
@Component
public class ProductCatalogSeedRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            ensureColumns();
            ensureTemplateRouteSteps();
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM material WHERE material_type = 'FINISHED' AND status = 1",
                    Integer.class);
            if (count != null && count >= 8) {
                ensureProductChains();
                return;
            }
            jdbcTemplate.execute("""
                UPDATE material SET image_url='/materials/products/prd-001.jpg', product_summary='15.6 英寸 IPS 商用显示器，1080P 全高清，适合办公与教育场景。', ports='VGA ×1 · HDMI ×1', sort_order=1 WHERE material_code='PRD-001'
                """);
            jdbcTemplate.execute("""
                UPDATE material SET image_url='/materials/products/prd-002.jpg', product_summary='23.8 英寸 2K 电竞显示器，144Hz 高刷，低延迟游戏体验。', ports='HDMI ×2 · DP ×1 · USB-C ×1', sort_order=2 WHERE material_code='PRD-002'
                """);
            jdbcTemplate.execute("""
                UPDATE material SET image_url='/materials/products/prd-003.jpg', product_summary='27 英寸 4K HDR 显示器，高色域专业视觉体验。', ports='HDMI ×2 · DP ×1 · USB-C ×1', sort_order=3 WHERE material_code='PRD-003'
                """);
            seedProduct("PRD-004", "21.5寸办公显示器", "1920x1080 IPS 办公款", 4, 720, "21.5 英寸 IPS 办公显示器，窄边框设计，日常办公优选。", "HDMI ×1 · DP ×1", "PRD-001");
            seedProduct("PRD-005", "24寸曲面显示器", "1920x1080 165Hz 曲面", 5, 980, "24 英寸曲面显示器，165Hz 刷新率，沉浸式娱乐与办公。", "HDMI ×2 · DP ×1", "PRD-002");
            seedProduct("PRD-006", "32寸电竞显示器", "2560x1440 165Hz 电竞", 6, 1680, "32 英寸 2K 大屏电竞显示器，165Hz 高刷，竞技大屏首选。", "HDMI ×2 · DP ×1 · USB-C ×1", "PRD-002");
            seedProduct("PRD-007", "27寸OLED显示器", "3840x2160 OLED HDR", 7, 2580, "27 英寸 OLED 4K 显示器，纯黑对比度，专业创作与影音。", "HDMI ×2 · DP ×1 · USB-C ×1", "PRD-003");
            seedProduct("PRD-008", "34寸超宽显示器", "3440x1440 100Hz 超宽", 8, 2280, "34 英寸 21:9 超宽显示器，100Hz 刷新，多任务与沉浸办公。", "HDMI ×2 · DP ×1 · USB-C ×1", "PRD-003");
            ensureProductChains();
        } catch (Exception ignored) {
            // 表未就绪时不阻断启动
        }
        repairStaleDispatchStatuses();
    }

    /** 已完结工单的派工若仍停留在在途状态，会误占操作员导致无法一键派工。 */
    private void repairStaleDispatchStatuses() {
        try {
            jdbcTemplate.update("""
                UPDATE dispatch_task dt
                JOIN work_order wo ON wo.work_order_id = dt.work_order_id
                SET dt.status = 'COMPLETED', dt.updated_at = NOW()
                WHERE wo.status IN ('COMPLETED', 'CANCELLED', 'CLOSED')
                  AND dt.status IN ('ASSIGNED', 'ACCEPTED', 'PRODUCING', 'RUNNING')
                """);
        } catch (Exception ignored) {
        }
    }

    private void ensureProductChains() {
        seedProductChain("PRD-004", "PRD-001");
        seedProductChain("PRD-005", "PRD-002");
        seedProductChain("PRD-006", "PRD-002");
        seedProductChain("PRD-007", "PRD-003");
        seedProductChain("PRD-008", "PRD-003");
    }

    /** PRD-003 作为高端模板，若工序未初始化则从 PRD-001 补齐 */
    private void ensureTemplateRouteSteps() {
        backfillProcessSteps("PRD-003", "PRD-001");
    }

    private void backfillProcessSteps(String targetCode, String templateCode) {
        jdbcTemplate.update("""
            INSERT INTO process_step (route_id, step_no, step_code, step_name, standard_work_hours, standard_equipment_type, quality_required, status, created_at, updated_at)
            SELECT tr.route_id, ps.step_no, CONCAT('S-', tm.material_code, '-', LPAD(CAST(ps.step_no AS CHAR), 3, '0')), ps.step_name,
                   ps.standard_work_hours, ps.standard_equipment_type, ps.quality_required, ps.status, NOW(), NOW()
            FROM process_step ps
            JOIN process_route sr ON sr.route_id = ps.route_id
            JOIN material sm ON sm.material_id = sr.material_id AND sm.material_code = ?
            JOIN material tm ON tm.material_code = ?
            JOIN process_route tr ON tr.material_id = tm.material_id
            WHERE NOT EXISTS (SELECT 1 FROM process_step xs WHERE xs.route_id = tr.route_id AND xs.step_no = ps.step_no)
            """, templateCode, targetCode);
    }

    private void seedProductChain(String code, String templateCode) {
        jdbcTemplate.update("""
            INSERT INTO bom (parent_material_id, child_material_id, quantity, loss_rate, version_no, effective_date, status, remark, created_at, updated_at)
            SELECT tgt.material_id, b.child_material_id, b.quantity, b.loss_rate, b.version_no, b.effective_date, b.status, b.remark, NOW(), NOW()
            FROM bom b JOIN material src ON src.material_code = ? JOIN material tgt ON tgt.material_code = ?
            WHERE b.parent_material_id = src.material_id
              AND NOT EXISTS (SELECT 1 FROM bom x WHERE x.parent_material_id = tgt.material_id AND x.child_material_id = b.child_material_id)
            """, templateCode, code);
        jdbcTemplate.update("""
            INSERT INTO process_route (material_id, route_code, route_name, version_no, status, created_by, created_at, updated_at)
            SELECT m.material_id, CONCAT('RT-', m.material_code, '-V1'), CONCAT(m.material_name, '工艺路线'), 'V1.0', 1, 2, NOW(), NOW()
            FROM material m WHERE m.material_code = ?
              AND NOT EXISTS (SELECT 1 FROM process_route pr WHERE pr.material_id = m.material_id)
            """, code);
        backfillProcessSteps(code, templateCode);
        jdbcTemplate.update("""
            INSERT INTO inventory (material_id, warehouse_code, warehouse_name, location_code, batch_no, quantity_on_hand, quantity_available, quantity_locked, inventory_status, last_transaction_at, created_at, updated_at)
            SELECT m.material_id, 'WH-02', '成品仓', CONCAT('B-', LPAD(m.sort_order, 2, '0'), '-01'), CONCAT('BATCH-', m.material_code), 0, 0, 0, 'NORMAL', NOW(), NOW(), NOW()
            FROM material m WHERE m.material_code = ?
              AND NOT EXISTS (SELECT 1 FROM inventory i WHERE i.material_id = m.material_id AND i.warehouse_code = 'WH-02')
            """, code);
    }

    private void ensureColumns() {
        tryAddColumn("image_url", "varchar(500) DEFAULT NULL COMMENT '展示图路径'");
        tryAddColumn("product_summary", "varchar(500) DEFAULT NULL COMMENT '产品简介'");
        tryAddColumn("ports", "varchar(200) DEFAULT NULL COMMENT '接口配置'");
        tryAddColumn("sort_order", "int NOT NULL DEFAULT 0 COMMENT '展示排序'");
    }

    private void tryAddColumn(String name, String definition) {
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material' AND COLUMN_NAME = ?",
                Integer.class, name);
        if (exists != null && exists > 0) return;
        jdbcTemplate.execute("ALTER TABLE material ADD COLUMN " + name + " " + definition);
    }

    private void seedProduct(String code, String name, String spec, int sort, double cost, String summary, String ports, String templateCode) {
        jdbcTemplate.update("""
            INSERT INTO material (material_code, material_name, material_type, specification, image_url, product_summary, ports, unit, safety_stock, standard_cost, sort_order, status, created_at, updated_at)
            SELECT ?, ?, 'FINISHED', ?, ?, ?, ?, '台', 20, ?, ?, 1, NOW(), NOW()
            FROM (SELECT 1) AS seed_tmp
            WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = ?)
            """, code, name, spec, "/materials/products/" + code.toLowerCase() + ".jpg", summary, ports, cost, sort, code);

        jdbcTemplate.update("""
            INSERT INTO bom (parent_material_id, child_material_id, quantity, loss_rate, version_no, effective_date, status, remark, created_at, updated_at)
            SELECT tgt.material_id, b.child_material_id, b.quantity, b.loss_rate, b.version_no, b.effective_date, b.status, b.remark, NOW(), NOW()
            FROM bom b JOIN material src ON src.material_code = ? JOIN material tgt ON tgt.material_code = ?
            WHERE b.parent_material_id = src.material_id
              AND NOT EXISTS (SELECT 1 FROM bom x WHERE x.parent_material_id = tgt.material_id AND x.child_material_id = b.child_material_id)
            """, templateCode, code);

        jdbcTemplate.update("""
            INSERT INTO process_route (material_id, route_code, route_name, version_no, status, created_by, created_at, updated_at)
            SELECT m.material_id, CONCAT('RT-', m.material_code, '-V1'), CONCAT(m.material_name, '工艺路线'), 'V1.0', 1, 2, NOW(), NOW()
            FROM material m WHERE m.material_code = ?
              AND NOT EXISTS (SELECT 1 FROM process_route pr WHERE pr.material_id = m.material_id)
            """, code);

        backfillProcessSteps(code, templateCode);

        jdbcTemplate.update("""
            INSERT INTO inventory (material_id, warehouse_code, warehouse_name, location_code, batch_no, quantity_on_hand, quantity_available, quantity_locked, inventory_status, last_transaction_at, created_at, updated_at)
            SELECT m.material_id, 'WH-02', '成品仓', CONCAT('B-', LPAD(m.sort_order, 2, '0'), '-01'), CONCAT('BATCH-', m.material_code), 0, 0, 0, 'NORMAL', NOW(), NOW(), NOW()
            FROM material m WHERE m.material_code = ?
              AND NOT EXISTS (SELECT 1 FROM inventory i WHERE i.material_id = m.material_id AND i.warehouse_code = 'WH-02')
            """, code);
    }
}
