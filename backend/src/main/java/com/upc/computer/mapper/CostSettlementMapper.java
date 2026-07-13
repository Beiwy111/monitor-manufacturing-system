package com.upc.computer.mapper;

import com.upc.computer.entity.CostSettlement;
import org.apache.ibatis.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mapper
public interface CostSettlementMapper {

    @Select("SELECT settlement_id,settlement_no,work_order_id,order_id,source_type,source_id,cost_reason,material_cost,labor_cost,equipment_cost,quality_cost,other_cost,total_cost,settlement_period,settlement_status,confirmed_by,confirmed_at,exported_at,remark,created_at,updated_at FROM cost_settlement ORDER BY created_at DESC")
    ArrayList<CostSettlement> settlementList();

    @Select("SELECT settlement_id,settlement_no,work_order_id,order_id,source_type,source_id,cost_reason,material_cost,labor_cost,equipment_cost,quality_cost,other_cost,total_cost,settlement_period,settlement_status,confirmed_by,confirmed_at,exported_at,remark,created_at,updated_at FROM cost_settlement WHERE settlement_id = #{settlementId}")
    CostSettlement getSettlementById(Long settlementId);

    @Insert("INSERT INTO cost_settlement (settlement_no,work_order_id,order_id,source_type,source_id,cost_reason,material_cost,labor_cost,equipment_cost,quality_cost,other_cost,total_cost,settlement_period,settlement_status,confirmed_by,confirmed_at,exported_at,remark,created_at,updated_at) VALUES (#{settlementNo},#{workOrderId},#{orderId},#{sourceType},#{sourceId},#{costReason},#{materialCost},#{laborCost},#{equipmentCost},#{qualityCost},#{otherCost},#{totalCost},#{settlementPeriod},#{settlementStatus},#{confirmedBy},#{confirmedAt},#{exportedAt},#{remark},#{createdAt},#{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "settlementId")
    void insertSettlement(CostSettlement settlement);

    @Update("UPDATE cost_settlement SET settlement_no=#{settlementNo},work_order_id=#{workOrderId},order_id=#{orderId},source_type=#{sourceType},source_id=#{sourceId},cost_reason=#{costReason},material_cost=#{materialCost},labor_cost=#{laborCost},equipment_cost=#{equipmentCost},quality_cost=#{qualityCost},other_cost=#{otherCost},total_cost=#{totalCost},settlement_period=#{settlementPeriod},settlement_status=#{settlementStatus},confirmed_by=#{confirmedBy},confirmed_at=#{confirmedAt},exported_at=#{exportedAt},remark=#{remark},updated_at=#{updatedAt} WHERE settlement_id=#{settlementId}")
    void updateSettlement(CostSettlement settlement);

    @Delete("DELETE FROM cost_settlement WHERE settlement_id = #{settlementId}")
    void deleteSettlement(Long settlementId);

    /** 列表视图（含来源类型中文、工单号） */
    @Select("""
        SELECT
          cs.settlement_id     AS settlementId,
          cs.settlement_no     AS settlementNo,
          cs.work_order_id     AS workOrderId,
          wo.work_order_no     AS workOrderNo,
          cs.order_id          AS orderId,
          cs.source_type       AS sourceType,
          cs.source_id         AS sourceId,
          cs.cost_reason       AS costReason,
          cs.material_cost     AS materialCost,
          cs.labor_cost        AS laborCost,
          cs.equipment_cost    AS equipmentCost,
          cs.quality_cost      AS qualityCost,
          cs.other_cost        AS otherCost,
          cs.total_cost        AS totalCost,
          cs.settlement_period AS settlementPeriod,
          cs.settlement_status AS settlementStatus,
          cs.confirmed_at      AS confirmedAt,
          cs.remark            AS remark,
          cs.created_at        AS createdAt
        FROM cost_settlement cs
        LEFT JOIN work_order wo ON wo.work_order_id = cs.work_order_id
        ORDER BY cs.created_at DESC
        """)
    List<Map<String, Object>> listSettlementViews();

    /** KPI 汇总：各成本类型合计 + 状态计数 */
    @Select("""
        SELECT
          COUNT(*)                                        AS total,
          SUM(CASE WHEN settlement_status='DRAFT'     THEN 1 ELSE 0 END) AS draft,
          SUM(CASE WHEN settlement_status='CONFIRMED' THEN 1 ELSE 0 END) AS confirmed,
          SUM(CASE WHEN settlement_status='EXPORTED'  THEN 1 ELSE 0 END) AS exported,
          COALESCE(SUM(total_cost),0)                    AS totalAmount,
          COALESCE(SUM(quality_cost),0)                  AS totalQualityCost,
          COALESCE(SUM(equipment_cost),0)                AS totalEquipmentCost,
          COALESCE(SUM(material_cost),0)                 AS totalMaterialCost,
          COALESCE(SUM(labor_cost),0)                    AS totalLaborCost,
          COALESCE(SUM(CASE WHEN source_type='NONCONFORMING_PRODUCT' THEN total_cost ELSE 0 END),0) AS ncCost,
          COALESCE(SUM(CASE WHEN source_type='AFTER_SALES'           THEN total_cost ELSE 0 END),0) AS afterSalesCost,
          COALESCE(SUM(CASE WHEN source_type='EQUIPMENT_MAINTENANCE'  THEN total_cost ELSE 0 END),0) AS equipmentMaintCost,
          COALESCE(SUM(CASE WHEN source_type='PURCHASE_RETURN'        THEN total_cost ELSE 0 END),0) AS purchaseReturnCost
        FROM cost_settlement
        """)
    Map<String, Object> costKpi();

    /** 按成本来源类型分组统计（用于饼图/柱图） */
    @Select("""
        SELECT
          source_type                  AS sourceType,
          COUNT(*)                     AS count,
          COALESCE(SUM(total_cost), 0) AS amount
        FROM cost_settlement
        GROUP BY source_type
        ORDER BY amount DESC
        """)
    List<Map<String, Object>> groupBySourceType();
}
