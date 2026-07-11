package com.upc.computer.mapper;

import com.upc.computer.entity.Inventory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    void increaseQuantity(@Param("inventoryId") Long inventoryId, @Param("quantity") BigDecimal quantity, @Param("transactionAt") LocalDateTime transactionAt);

    @Delete("DELETE FROM inventory WHERE inventory_id = #{inventoryId}")
    void deleteInventory(Long inventoryId);

    @Select("""
            SELECT inventory_id, material_id, warehouse_code, warehouse_name, location_code, batch_no,
                   quantity_on_hand, quantity_reserved, quantity_available, inventory_status,
                   last_transaction_at, created_at, updated_at
            FROM inventory
            WHERE material_id = #{materialId}
              AND COALESCE(batch_no, '') = COALESCE(#{batchNo}, '')
              AND warehouse_code = #{warehouseCode}
              AND COALESCE(location_code, '') = COALESCE(#{locationCode}, '')
            LIMIT 1
            """)
    Inventory getByMaterialBatchLocation(@Param("materialId") Long materialId,
                                         @Param("batchNo") String batchNo,
                                         @Param("warehouseCode") String warehouseCode,
                                         @Param("locationCode") String locationCode);

    @Select("""
            SELECT i.inventory_id AS inventoryId, i.material_id AS materialId,
                   m.material_code AS materialCode, m.material_name AS materialName, m.unit,
                   i.warehouse_code AS warehouseCode, i.warehouse_name AS warehouseName,
                   i.location_code AS locationCode, i.batch_no AS batchNo,
                   i.quantity_on_hand AS quantityOnHand, i.quantity_reserved AS quantityReserved,
                   i.quantity_available AS quantityAvailable, i.inventory_status AS inventoryStatus,
                   COALESCE(bc.barcodeQty, 0) AS barcodeQuantity,
                   COALESCE(bc.barcodeCount, 0) AS barcodeCount,
                   (
                     SELECT MAX(l.handled_at)
                     FROM inventory_barcode ib
                     JOIN inventory_scan_log l ON l.barcode_no = ib.barcode_no
                     WHERE ib.inventory_id = i.inventory_id
                   ) AS lastScanAt,
                   i.last_transaction_at AS lastTransactionAt, i.updated_at AS updatedAt
            FROM inventory i
            LEFT JOIN material m ON m.material_id = i.material_id
            LEFT JOIN (
                SELECT inventory_id, SUM(remaining_quantity) AS barcodeQty, COUNT(*) AS barcodeCount
                FROM inventory_barcode
                WHERE barcode_status IN ('IN_STOCK', 'PARTIAL')
                GROUP BY inventory_id
            ) bc ON bc.inventory_id = i.inventory_id
            ORDER BY i.updated_at DESC
            """)
    List<Map<String, Object>> inventoryDetailList();

    @Select("""
            SELECT m.material_id AS materialId,
                   m.material_code AS materialCode,
                   m.material_name AS materialName,
                   m.material_type AS materialType,
                   m.specification AS specification,
                   m.unit AS unit,
                   m.safety_stock AS safetyStock,
                   COALESCE(SUM(i.quantity_on_hand), 0) AS quantityOnHand,
                   COALESCE(SUM(i.quantity_available), 0) AS quantityAvailable,
                   COALESCE(SUM(CASE WHEN i.warehouse_code LIKE 'FG%' THEN i.quantity_on_hand ELSE 0 END), 0) AS finishedWarehouseQty,
                   COALESCE(SUM(CASE WHEN i.warehouse_code IS NULL OR i.warehouse_code NOT LIKE 'FG%' THEN i.quantity_on_hand ELSE 0 END), 0) AS rawWarehouseQty,
                   COUNT(DISTINCT NULLIF(i.batch_no, '')) AS batchCount,
                   MAX(i.updated_at) AS updatedAt
            FROM material m
            LEFT JOIN inventory i ON i.material_id = m.material_id
            WHERE m.status IS NULL OR m.status = 1
            GROUP BY m.material_id, m.material_code, m.material_name, m.material_type,
                     m.specification, m.unit, m.safety_stock
            ORDER BY m.material_type, m.material_code
            """)
    List<Map<String, Object>> inventoryCatalogList();
}
