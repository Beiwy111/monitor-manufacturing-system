package com.upc.computer.mapper;

import com.upc.computer.entity.Inventory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface InventoryMapper {

    @Select("SELECT i.inventory_id, i.material_id, " +
            "m.material_code, m.material_name, m.unit, m.safety_stock, " +
            "i.warehouse_code, i.warehouse_name, i.location_code, i.batch_no, " +
            "i.quantity_on_hand, i.quantity_reserved, i.quantity_available, " +
            "i.inventory_status, i.last_transaction_at, i.created_at, i.updated_at " +
            "FROM inventory i LEFT JOIN material m ON i.material_id = m.material_id " +
            "ORDER BY i.inventory_id")
    ArrayList<Inventory> inventoryListWithMaterial();

    @Select("SELECT inventory_id, material_id, warehouse_code, warehouse_name, location_code, batch_no, quantity_on_hand, quantity_reserved, quantity_available, inventory_status, last_transaction_at, created_at, updated_at FROM inventory")
    ArrayList<Inventory> inventoryList();

    @Select("SELECT inventory_id, material_id, warehouse_code, warehouse_name, location_code, batch_no, quantity_on_hand, quantity_reserved, quantity_available, inventory_status, last_transaction_at, created_at, updated_at FROM inventory WHERE inventory_id = #{inventoryId}")
    Inventory getInventoryById(Long inventoryId);

    @Select("SELECT i.inventory_id, i.material_id, " +
            "m.material_code, m.material_name, m.unit, m.safety_stock, " +
            "i.warehouse_code, i.warehouse_name, i.location_code, i.batch_no, " +
            "i.quantity_on_hand, i.quantity_reserved, i.quantity_available, " +
            "i.inventory_status, i.last_transaction_at, i.created_at, i.updated_at " +
            "FROM inventory i LEFT JOIN material m ON i.material_id = m.material_id " +
            "WHERE i.material_id = #{materialId} ORDER BY i.inventory_id ASC")
    List<Inventory> listByMaterialId(Long materialId);

    @Insert("INSERT INTO inventory (material_id, warehouse_code, warehouse_name, location_code, batch_no, quantity_on_hand, quantity_reserved, quantity_available, inventory_status, last_transaction_at, created_at, updated_at) VALUES (#{materialId}, #{warehouseCode}, #{warehouseName}, #{locationCode}, #{batchNo}, #{quantityOnHand}, #{quantityReserved}, #{quantityAvailable}, #{inventoryStatus}, #{lastTransactionAt}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "inventoryId")
    void insertInventory(Inventory inventory);

    @Update("UPDATE inventory SET material_id=#{materialId}, warehouse_code=#{warehouseCode}, warehouse_name=#{warehouseName}, location_code=#{locationCode}, batch_no=#{batchNo}, quantity_on_hand=#{quantityOnHand}, quantity_reserved=#{quantityReserved}, quantity_available=#{quantityAvailable}, inventory_status=#{inventoryStatus}, last_transaction_at=#{lastTransactionAt}, updated_at=#{updatedAt} WHERE inventory_id = #{inventoryId}")
    void updateInventory(Inventory inventory);

    @Update("UPDATE inventory SET quantity_on_hand = quantity_on_hand + #{quantity}, quantity_available = quantity_available + #{quantity}, last_transaction_at = #{transactionAt}, updated_at = #{transactionAt} WHERE inventory_id = #{inventoryId}")
    void increaseQuantity(@Param("inventoryId") Long inventoryId, @Param("quantity") BigDecimal quantity, @Param("transactionAt") java.time.LocalDateTime transactionAt);

    @Delete("DELETE FROM inventory WHERE inventory_id = #{inventoryId}")
    void deleteInventory(Long inventoryId);
}
