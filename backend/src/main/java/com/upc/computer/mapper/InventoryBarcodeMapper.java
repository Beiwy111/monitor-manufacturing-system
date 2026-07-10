package com.upc.computer.mapper;

import com.upc.computer.entity.InventoryBarcode;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface InventoryBarcodeMapper {
    @Select("""
            SELECT barcode_id AS barcodeId, barcode_no AS barcodeNo, material_id AS materialId, batch_no AS batchNo,
                   inventory_id AS inventoryId, quantity, remaining_quantity AS remainingQuantity,
                   barcode_status AS barcodeStatus, source_type AS sourceType, source_no AS sourceNo,
                   related_work_order_id AS relatedWorkOrderId, related_purchase_order_id AS relatedPurchaseOrderId,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM inventory_barcode
            ORDER BY created_at DESC
            """)
    List<InventoryBarcode> barcodeList();

    @Select("""
            SELECT barcode_id AS barcodeId, barcode_no AS barcodeNo, material_id AS materialId, batch_no AS batchNo,
                   inventory_id AS inventoryId, quantity, remaining_quantity AS remainingQuantity,
                   barcode_status AS barcodeStatus, source_type AS sourceType, source_no AS sourceNo,
                   related_work_order_id AS relatedWorkOrderId, related_purchase_order_id AS relatedPurchaseOrderId,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM inventory_barcode
            WHERE barcode_no = #{barcodeNo}
            LIMIT 1
            """)
    InventoryBarcode getByBarcodeNo(String barcodeNo);

    @Select("""
            SELECT b.barcode_id AS barcodeId, b.barcode_no AS barcodeNo, b.material_id AS materialId,
                   m.material_code AS materialCode, m.material_name AS materialName, m.unit,
                   b.batch_no AS batchNo, b.inventory_id AS inventoryId, i.warehouse_code AS warehouseCode,
                   i.warehouse_name AS warehouseName, i.location_code AS locationCode,
                   b.quantity, b.remaining_quantity AS remainingQuantity, b.barcode_status AS barcodeStatus,
                   b.source_type AS sourceType, b.source_no AS sourceNo,
                   b.created_at AS createdAt, b.updated_at AS updatedAt
            FROM inventory_barcode b
            LEFT JOIN material m ON m.material_id = b.material_id
            LEFT JOIN inventory i ON i.inventory_id = b.inventory_id
            ORDER BY b.created_at DESC
            """)
    List<Map<String, Object>> barcodeDetailList();

    @Insert("""
            INSERT INTO inventory_barcode (barcode_no, material_id, batch_no, inventory_id, quantity, remaining_quantity,
                                           barcode_status, source_type, source_no, related_work_order_id,
                                           related_purchase_order_id, created_at, updated_at)
            VALUES (#{barcodeNo}, #{materialId}, #{batchNo}, #{inventoryId}, #{quantity}, #{remainingQuantity},
                    #{barcodeStatus}, #{sourceType}, #{sourceNo}, #{relatedWorkOrderId},
                    #{relatedPurchaseOrderId}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "barcodeId")
    void insertBarcode(InventoryBarcode barcode);

    @Update("""
            UPDATE inventory_barcode
            SET inventory_id=#{inventoryId}, quantity=#{quantity}, remaining_quantity=#{remainingQuantity},
                barcode_status=#{barcodeStatus}, updated_at=#{updatedAt}
            WHERE barcode_id=#{barcodeId}
            """)
    void updateBarcode(InventoryBarcode barcode);

    @Select("""
            SELECT COALESCE(SUM(remaining_quantity), 0)
            FROM inventory_barcode
            WHERE inventory_id = #{inventoryId} AND barcode_status IN ('IN_STOCK', 'PARTIAL')
            """)
    java.math.BigDecimal sumRemainingByInventory(Long inventoryId);
}
