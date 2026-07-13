package com.upc.computer.mapper;

import com.upc.computer.entity.PurchaseRequirement;
import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PurchaseRequirementMapper {

    @Select("SELECT requirement_id, material_id, material_code, material_name, required_quantity, stock_quantity, on_purchase_quantity, shortage_quantity, suggested_purchase_quantity, status, priority, expected_arrival_date, purchase_order_id, supplier_id, supplier_name, remark, created_at, updated_at FROM purchase_requirement ORDER BY priority ASC, shortage_quantity DESC")
    ArrayList<PurchaseRequirement> requirementList();

    @Select("SELECT requirement_id, material_id, material_code, material_name, required_quantity, stock_quantity, on_purchase_quantity, shortage_quantity, suggested_purchase_quantity, status, priority, expected_arrival_date, purchase_order_id, supplier_id, supplier_name, remark, created_at, updated_at FROM purchase_requirement WHERE requirement_id = #{requirementId}")
    PurchaseRequirement getRequirementById(Long requirementId);

    @Select("SELECT requirement_id, material_id, material_code, material_name, required_quantity, stock_quantity, on_purchase_quantity, shortage_quantity, suggested_purchase_quantity, status, priority, expected_arrival_date, purchase_order_id, supplier_id, supplier_name, remark, created_at, updated_at FROM purchase_requirement WHERE material_id = #{materialId} AND status IN ('PENDING','SELECTED') ORDER BY priority ASC LIMIT 1")
    PurchaseRequirement getActiveByMaterialId(Long materialId);

    @Select("SELECT requirement_id, material_id, material_code, material_name, required_quantity, stock_quantity, on_purchase_quantity, shortage_quantity, suggested_purchase_quantity, status, priority, expected_arrival_date, purchase_order_id, supplier_id, supplier_name, remark, created_at, updated_at FROM purchase_requirement WHERE purchase_order_id = #{purchaseOrderId}")
    ArrayList<PurchaseRequirement> listByPurchaseOrderId(Long purchaseOrderId);

    @Insert("INSERT INTO purchase_requirement (material_id, material_code, material_name, required_quantity, stock_quantity, on_purchase_quantity, shortage_quantity, suggested_purchase_quantity, status, priority, expected_arrival_date, purchase_order_id, supplier_id, supplier_name, remark, created_at, updated_at) VALUES (#{materialId}, #{materialCode}, #{materialName}, #{requiredQuantity}, #{stockQuantity}, #{onPurchaseQuantity}, #{shortageQuantity}, #{suggestedPurchaseQuantity}, #{status}, #{priority}, #{expectedArrivalDate}, #{purchaseOrderId}, #{supplierId}, #{supplierName}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "requirementId")
    void insertRequirement(PurchaseRequirement requirement);

    @Update("UPDATE purchase_requirement SET material_id=#{materialId}, material_code=#{materialCode}, material_name=#{materialName}, required_quantity=#{requiredQuantity}, stock_quantity=#{stockQuantity}, on_purchase_quantity=#{onPurchaseQuantity}, shortage_quantity=#{shortageQuantity}, suggested_purchase_quantity=#{suggestedPurchaseQuantity}, status=#{status}, priority=#{priority}, expected_arrival_date=#{expectedArrivalDate}, purchase_order_id=#{purchaseOrderId}, supplier_id=#{supplierId}, supplier_name=#{supplierName}, remark=#{remark}, updated_at=#{updatedAt} WHERE requirement_id = #{requirementId}")
    void updateRequirement(PurchaseRequirement requirement);

    @Update("UPDATE purchase_requirement SET status=#{status}, updated_at=NOW() WHERE requirement_id = #{requirementId}")
    void updateStatus(@Param("requirementId") Long requirementId, @Param("status") String status);

    @Update("UPDATE purchase_requirement SET status=#{status}, purchase_order_id=#{purchaseOrderId}, updated_at=NOW() WHERE requirement_id = #{requirementId}")
    void bindPurchaseOrder(@Param("requirementId") Long requirementId, @Param("purchaseOrderId") Long purchaseOrderId, @Param("status") String status);

    @Update("UPDATE purchase_requirement SET purchase_order_id=NULL, updated_at=NOW() WHERE requirement_id = #{requirementId}")
    void unbindPurchaseOrder(@Param("requirementId") Long requirementId);

    @Delete("DELETE FROM purchase_requirement WHERE requirement_id = #{requirementId}")
    void deleteRequirement(Long requirementId);

    @Delete("DELETE FROM purchase_requirement WHERE status IN ('PENDING','SELECTED')")
    void deleteByStatuses();

    @Delete("DELETE FROM purchase_requirement WHERE purchase_order_id IS NULL AND status NOT IN ('PURCHASED','ARRIVED')")
    void deleteOpenWorkbenchRows();

    @Delete("DELETE FROM purchase_requirement WHERE status = 'PENDING'")
    void deleteAllPending();
}
