package com.upc.computer.mapper;

import com.upc.computer.entity.PurchaseRequirementSource;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PurchaseRequirementSourceMapper {

    @Select("SELECT source_id, requirement_id, customer_order_id, customer_order_no, work_order_id, work_order_no, source_type, material_id, required_quantity, shortage_quantity, created_at FROM purchase_requirement_source WHERE requirement_id = #{requirementId}")
    ArrayList<PurchaseRequirementSource> listByRequirementId(Long requirementId);

    @Select("SELECT source_id, requirement_id, customer_order_id, customer_order_no, work_order_id, work_order_no, source_type, material_id, required_quantity, shortage_quantity, created_at FROM purchase_requirement_source")
    ArrayList<PurchaseRequirementSource> sourceList();

    @Insert("INSERT INTO purchase_requirement_source (requirement_id, customer_order_id, customer_order_no, work_order_id, work_order_no, source_type, material_id, required_quantity, shortage_quantity, created_at) VALUES (#{requirementId}, #{customerOrderId}, #{customerOrderNo}, #{workOrderId}, #{workOrderNo}, #{sourceType}, #{materialId}, #{requiredQuantity}, #{shortageQuantity}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "sourceId")
    void insertSource(PurchaseRequirementSource source);

    @Delete("DELETE FROM purchase_requirement_source WHERE requirement_id = #{requirementId}")
    void deleteByRequirementId(Long requirementId);

    @Delete("DELETE FROM purchase_requirement_source WHERE requirement_id IN (SELECT requirement_id FROM purchase_requirement WHERE status IN ('PENDING','SELECTED'))")
    void deleteByStatuses();

    @Delete("DELETE FROM purchase_requirement_source WHERE requirement_id IN (SELECT requirement_id FROM purchase_requirement WHERE purchase_order_id IS NULL AND status NOT IN ('PURCHASED','ARRIVED'))")
    void deleteOpenWorkbenchRows();

    @Delete("DELETE FROM purchase_requirement_source WHERE requirement_id IN (SELECT requirement_id FROM purchase_requirement WHERE status = 'PENDING')")
    void deleteAllPending();

}
