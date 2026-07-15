package com.upc.computer.mapper;

import com.upc.computer.entity.WarehouseLocation;
import com.upc.computer.entity.WarehouseLocationSlot;
import com.upc.computer.entity.WarehouseZone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
}
