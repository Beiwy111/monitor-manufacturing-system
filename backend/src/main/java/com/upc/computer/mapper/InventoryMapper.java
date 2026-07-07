package com.upc.computer.mapper;

import com.upc.computer.entity.Inventory;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface InventoryMapper {

    // 查询所有库存
    @Select("SELECT inventory_id, material_id, warehouse_code, warehouse_name, location_code, batch_no, quantity_on_hand, quantity_reserved, quantity_available, inventory_status, last_transaction_at, created_at, updated_at FROM inventory")
    public ArrayList<Inventory> inventoryList();

    // 根据主键查询库存
    @Select("SELECT inventory_id, material_id, warehouse_code, warehouse_name, location_code, batch_no, quantity_on_hand, quantity_reserved, quantity_available, inventory_status, last_transaction_at, created_at, updated_at FROM inventory WHERE inventory_id = #{inventoryId}")
    public Inventory getInventoryById(Long inventoryId);

    // 新增库存
    @Insert("INSERT INTO inventory (inventory_id, material_id, warehouse_code, warehouse_name, location_code, batch_no, quantity_on_hand, quantity_reserved, quantity_available, inventory_status, last_transaction_at, created_at, updated_at) VALUES (#{inventoryId}, #{materialId}, #{warehouseCode}, #{warehouseName}, #{locationCode}, #{batchNo}, #{quantityOnHand}, #{quantityReserved}, #{quantityAvailable}, #{inventoryStatus}, #{lastTransactionAt}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "inventoryId")
    public void insertInventory(Inventory inventory);

    // 修改库存
    @Update("UPDATE inventory SET material_id=#{materialId}, warehouse_code=#{warehouseCode}, warehouse_name=#{warehouseName}, location_code=#{locationCode}, batch_no=#{batchNo}, quantity_on_hand=#{quantityOnHand}, quantity_reserved=#{quantityReserved}, quantity_available=#{quantityAvailable}, inventory_status=#{inventoryStatus}, last_transaction_at=#{lastTransactionAt}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE inventory_id = #{inventoryId}")
    public void updateInventory(Inventory inventory);

    // 删除库存
    @Delete("DELETE FROM inventory WHERE inventory_id = #{inventoryId}")
    public void deleteInventory(Long inventoryId);

}
