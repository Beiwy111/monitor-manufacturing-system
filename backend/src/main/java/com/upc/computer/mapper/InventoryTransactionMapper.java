package com.upc.computer.mapper;

import com.upc.computer.entity.InventoryTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface InventoryTransactionMapper {

    // 查询所有库存流水
    @Select("SELECT transaction_id, transaction_no, inventory_id, material_id, transaction_type, quantity, warehouse_code, location_code, batch_no, related_purchase_order_id, related_work_order_id, handled_by, handled_at, remark, created_at FROM inventory_transaction")
    public ArrayList<InventoryTransaction> transactionList();

    // 根据主键查询库存流水
    @Select("SELECT transaction_id, transaction_no, inventory_id, material_id, transaction_type, quantity, warehouse_code, location_code, batch_no, related_purchase_order_id, related_work_order_id, handled_by, handled_at, remark, created_at FROM inventory_transaction WHERE transaction_id = #{transactionId}")
    public InventoryTransaction getTransactionById(Long transactionId);

    // 新增库存流水
    @Insert("INSERT INTO inventory_transaction (transaction_id, transaction_no, inventory_id, material_id, transaction_type, quantity, warehouse_code, location_code, batch_no, related_purchase_order_id, related_work_order_id, handled_by, handled_at, remark, created_at) VALUES (#{transactionId}, #{transactionNo}, #{inventoryId}, #{materialId}, #{transactionType}, #{quantity}, #{warehouseCode}, #{locationCode}, #{batchNo}, #{relatedPurchaseOrderId}, #{relatedWorkOrderId}, #{handledBy}, #{handledAt}, #{remark}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "transactionId")
    public void insertTransaction(InventoryTransaction transaction);

    // 修改库存流水
    @Update("UPDATE inventory_transaction SET transaction_no=#{transactionNo}, inventory_id=#{inventoryId}, material_id=#{materialId}, transaction_type=#{transactionType}, quantity=#{quantity}, warehouse_code=#{warehouseCode}, location_code=#{locationCode}, batch_no=#{batchNo}, related_purchase_order_id=#{relatedPurchaseOrderId}, related_work_order_id=#{relatedWorkOrderId}, handled_by=#{handledBy}, handled_at=#{handledAt}, remark=#{remark}, created_at=#{createdAt} WHERE transaction_id = #{transactionId}")
    public void updateTransaction(InventoryTransaction transaction);

    // 删除库存流水
    @Delete("DELETE FROM inventory_transaction WHERE transaction_id = #{transactionId}")
    public void deleteTransaction(Long transactionId);

    @Select("""
            SELECT t.transaction_no AS transactionNo,
                   t.transaction_type AS transactionType,
                   m.material_code AS materialCode,
                   m.material_name AS materialName,
                   m.material_type AS materialType,
                   t.quantity AS quantity,
                   t.warehouse_code AS warehouseCode,
                   t.location_code AS locationCode,
                   t.batch_no AS batchNo,
                   t.remark AS remark,
                   u.real_name AS operatorName,
                   t.handled_at AS handledAt
            FROM inventory_transaction t
            LEFT JOIN material m ON m.material_id = t.material_id
            LEFT JOIN user u ON u.user_id = t.handled_by
            ORDER BY t.handled_at DESC, t.transaction_id DESC
            """)
    List<Map<String, Object>> transactionDetailList();

}
