package com.upc.computer.mapper;

import com.upc.computer.entity.InventoryScanLog;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface InventoryScanLogMapper {
    @Select("""
            SELECT scan_id AS scanId, scan_no AS scanNo, barcode_no AS barcodeNo, scan_type AS scanType,
                   quantity, warehouse_code AS warehouseCode, location_code AS locationCode,
                   business_no AS businessNo, result_status AS resultStatus, message,
                   handled_by AS handledBy, handled_at AS handledAt, created_at AS createdAt
            FROM inventory_scan_log
            ORDER BY handled_at DESC
            """)
    List<InventoryScanLog> scanList();

    @Select("""
            SELECT l.scan_id AS scanId, l.scan_no AS scanNo, l.barcode_no AS barcodeNo, l.scan_type AS scanType,
                   l.quantity, l.warehouse_code AS warehouseCode, l.location_code AS locationCode,
                   l.business_no AS businessNo, l.result_status AS resultStatus, l.message,
                   u.username AS operatorName, l.handled_at AS handledAt
            FROM inventory_scan_log l
            LEFT JOIN user u ON u.user_id = l.handled_by
            ORDER BY l.handled_at DESC
            """)
    List<Map<String, Object>> scanDetailList();

    @Select("""
            SELECT scan_id AS scanId, scan_no AS scanNo, barcode_no AS barcodeNo, scan_type AS scanType,
                   quantity, warehouse_code AS warehouseCode, location_code AS locationCode,
                   business_no AS businessNo, result_status AS resultStatus, message,
                   handled_by AS handledBy, handled_at AS handledAt, created_at AS createdAt
            FROM inventory_scan_log
            WHERE barcode_no = #{barcodeNo}
            ORDER BY handled_at DESC
            """)
    List<InventoryScanLog> listByBarcodeNo(String barcodeNo);

    @Insert("""
            INSERT INTO inventory_scan_log (scan_no, barcode_no, scan_type, quantity, warehouse_code, location_code,
                                            business_no, result_status, message, handled_by, handled_at, created_at)
            VALUES (#{scanNo}, #{barcodeNo}, #{scanType}, #{quantity}, #{warehouseCode}, #{locationCode},
                    #{businessNo}, #{resultStatus}, #{message}, #{handledBy}, #{handledAt}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "scanId")
    void insertScanLog(InventoryScanLog log);
}
