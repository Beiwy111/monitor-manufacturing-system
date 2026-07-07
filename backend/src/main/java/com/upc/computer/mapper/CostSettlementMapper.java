package com.upc.computer.mapper;

import com.upc.computer.entity.CostSettlement;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface CostSettlementMapper {

    // 查询所有成本结算
    @Select("SELECT settlement_id, settlement_no, work_order_id, order_id, material_cost, labor_cost, equipment_cost, quality_cost, other_cost, total_cost, settlement_period, settlement_status, confirmed_by, confirmed_at, exported_at, remark, created_at, updated_at FROM cost_settlement")
    public ArrayList<CostSettlement> settlementList();

    // 根据主键查询成本结算
    @Select("SELECT settlement_id, settlement_no, work_order_id, order_id, material_cost, labor_cost, equipment_cost, quality_cost, other_cost, total_cost, settlement_period, settlement_status, confirmed_by, confirmed_at, exported_at, remark, created_at, updated_at FROM cost_settlement WHERE settlement_id = #{settlementId}")
    public CostSettlement getSettlementById(Long settlementId);

    // 新增成本结算
    @Insert("INSERT INTO cost_settlement (settlement_id, settlement_no, work_order_id, order_id, material_cost, labor_cost, equipment_cost, quality_cost, other_cost, total_cost, settlement_period, settlement_status, confirmed_by, confirmed_at, exported_at, remark, created_at, updated_at) VALUES (#{settlementId}, #{settlementNo}, #{workOrderId}, #{orderId}, #{materialCost}, #{laborCost}, #{equipmentCost}, #{qualityCost}, #{otherCost}, #{totalCost}, #{settlementPeriod}, #{settlementStatus}, #{confirmedBy}, #{confirmedAt}, #{exportedAt}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "settlementId")
    public void insertSettlement(CostSettlement settlement);

    // 修改成本结算
    @Update("UPDATE cost_settlement SET settlement_no=#{settlementNo}, work_order_id=#{workOrderId}, order_id=#{orderId}, material_cost=#{materialCost}, labor_cost=#{laborCost}, equipment_cost=#{equipmentCost}, quality_cost=#{qualityCost}, other_cost=#{otherCost}, total_cost=#{totalCost}, settlement_period=#{settlementPeriod}, settlement_status=#{settlementStatus}, confirmed_by=#{confirmedBy}, confirmed_at=#{confirmedAt}, exported_at=#{exportedAt}, remark=#{remark}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE settlement_id = #{settlementId}")
    public void updateSettlement(CostSettlement settlement);

    // 删除成本结算
    @Delete("DELETE FROM cost_settlement WHERE settlement_id = #{settlementId}")
    public void deleteSettlement(Long settlementId);

}
