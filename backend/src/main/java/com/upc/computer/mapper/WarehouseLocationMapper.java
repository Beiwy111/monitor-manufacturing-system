package com.upc.computer.mapper;

import com.upc.computer.entity.WarehouseLocation;
import com.upc.computer.entity.WarehouseLocationSlot;
import com.upc.computer.entity.WarehouseZone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface WarehouseLocationMapper {

    @Select("""
            SELECT zone_id AS zoneId, zone_code AS zoneCode, zone_name AS zoneName,
                   warehouse_code AS warehouseCode, description, sort_no AS sortNo, status,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM warehouse_zone
            WHERE status = 1
            ORDER BY sort_no, zone_id
            """)
    List<WarehouseZone> listZones();

    @Select("""
            SELECT location_id AS locationId, zone_id AS zoneId, location_code AS locationCode,
                   location_name AS locationName, grid_rows AS gridRows, grid_cols AS gridCols,
                   sort_no AS sortNo, status, created_at AS createdAt, updated_at AS updatedAt
            FROM warehouse_location
            WHERE status = 1
            ORDER BY sort_no, location_id
            """)
    List<WarehouseLocation> listLocations();

    @Select("""
            SELECT slot_id AS slotId, location_id AS locationId, slot_code AS slotCode,
                   row_no AS rowNo, col_no AS colNo, capacity, occupied,
                   material_id AS materialId, material_name AS materialName,
                   inventory_id AS inventoryId, created_at AS createdAt, updated_at AS updatedAt
            FROM warehouse_location_slot
            ORDER BY location_id, row_no, col_no
            """)
    List<WarehouseLocationSlot> listSlots();

    @Select("""
            SELECT s.slot_id AS slotId, s.slot_code AS slotCode, s.row_no AS rowNo, s.col_no AS colNo,
                   s.capacity AS capacity, s.occupied AS occupied,
                   s.material_id AS materialId, s.material_name AS materialName, s.inventory_id AS inventoryId,
                   l.location_id AS locationId, l.location_code AS locationCode, l.location_name AS locationName,
                   z.zone_id AS zoneId, z.zone_code AS zoneCode, z.zone_name AS zoneName,
                   z.warehouse_code AS warehouseCode
            FROM warehouse_location_slot s
            JOIN warehouse_location l ON l.location_id = s.location_id
            JOIN warehouse_zone z ON z.zone_id = l.zone_id
            WHERE s.occupied = 0 AND z.status = 1 AND l.status = 1
              AND z.zone_code = #{zoneCode}
            ORDER BY l.sort_no, s.row_no, s.col_no
            """)
    List<Map<String, Object>> listAvailableSlotDetails(@Param("zoneCode") String zoneCode);

    @Select("""
            SELECT s.slot_id AS slotId, s.slot_code AS slotCode, s.row_no AS rowNo, s.col_no AS colNo,
                   s.capacity AS capacity, s.occupied AS occupied,
                   s.material_id AS materialId, s.material_name AS materialName, s.inventory_id AS inventoryId,
                   l.location_id AS locationId, l.location_code AS locationCode, l.location_name AS locationName,
                   z.zone_id AS zoneId, z.zone_code AS zoneCode, z.zone_name AS zoneName,
                   z.warehouse_code AS warehouseCode
            FROM warehouse_location_slot s
            JOIN warehouse_location l ON l.location_id = s.location_id
            JOIN warehouse_zone z ON z.zone_id = l.zone_id
            WHERE s.slot_code = #{slotCode}
            LIMIT 1
            """)
    Map<String, Object> getSlotDetailByCode(@Param("slotCode") String slotCode);

    @Update("""
            UPDATE warehouse_location_slot
            SET occupied = 1, material_id = #{materialId}, material_name = #{materialName},
                inventory_id = #{inventoryId}, updated_at = NOW()
            WHERE slot_code = #{slotCode} AND occupied = 0
            """)
    int occupySlot(@Param("slotCode") String slotCode,
                   @Param("materialId") Long materialId,
                   @Param("materialName") String materialName,
                   @Param("inventoryId") Long inventoryId);

    @Update("""
            UPDATE warehouse_location_slot
            SET occupied = 0, material_id = NULL, material_name = NULL,
                inventory_id = NULL, updated_at = NOW()
            WHERE inventory_id = #{inventoryId}
            """)
    void releaseSlotsByInventoryId(@Param("inventoryId") Long inventoryId);
}
