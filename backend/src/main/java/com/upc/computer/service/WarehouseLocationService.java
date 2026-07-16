package com.upc.computer.service;

import com.upc.computer.entity.WarehouseLocation;
import com.upc.computer.entity.WarehouseLocationSlot;
import com.upc.computer.entity.WarehouseZone;
import com.upc.computer.mapper.WarehouseLocationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WarehouseLocationService {

    @Autowired
    private WarehouseLocationMapper warehouseLocationMapper;
    @Autowired
    private WarehouseSlotService warehouseSlotService;

    public Map<String, Object> locationMap() {
        List<WarehouseZone> zones = safeListZones();
        List<WarehouseLocation> locations = safeListLocations();
        List<WarehouseLocationSlot> slots = safeListSlots();

        Map<Long, List<WarehouseLocation>> locByZone = locations.stream()
                .collect(Collectors.groupingBy(WarehouseLocation::getZoneId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<WarehouseLocationSlot>> slotByLoc = slots.stream()
                .collect(Collectors.groupingBy(WarehouseLocationSlot::getLocationId, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> zoneViews = new ArrayList<>();
        int totalBins = 0;
        int occupiedBins = 0;
        int occupiedLocations = 0;
        int emptyLocations = 0;

        for (WarehouseZone zone : zones) {
            List<Map<String, Object>> locationViews = new ArrayList<>();
            int zoneCap = 0;
            int zoneOcc = 0;

            for (WarehouseLocation loc : locByZone.getOrDefault(zone.getZoneId(), List.of())) {
                List<WarehouseLocationSlot> locSlots = slotByLoc.getOrDefault(loc.getLocationId(), List.of());
                int cap = locSlots.stream().mapToInt(s -> s.getCapacity() != null ? s.getCapacity() : 1).sum();
                int occ = locSlots.stream().mapToInt(s -> s.getOccupied() != null && s.getOccupied() > 0 ? 1 : 0).sum();
                zoneCap += cap;
                zoneOcc += occ;
                totalBins += cap;
                occupiedBins += occ;
                if (occ > 0) occupiedLocations++;
                else emptyLocations++;

                String primaryMaterial = locSlots.stream()
                        .filter(s -> s.getOccupied() != null && s.getOccupied() > 0 && s.getMaterialName() != null)
                        .map(WarehouseLocationSlot::getMaterialName)
                        .findFirst()
                        .orElse("—");

                List<Map<String, Object>> binViews = locSlots.stream().map(s -> {
                    int sCap = s.getCapacity() != null ? s.getCapacity() : 1;
                    int sOcc = s.getOccupied() != null && s.getOccupied() > 0 ? 1 : 0;
                    Map<String, Object> bin = new LinkedHashMap<>();
                    bin.put("id", s.getSlotCode());
                    bin.put("occupied", sOcc);
                    bin.put("capacity", sCap);
                    bin.put("materialName", sOcc > 0 ? s.getMaterialName() : "");
                    bin.put("materialId", s.getMaterialId());
                    bin.put("inventoryId", s.getInventoryId());
                    bin.put("rowNo", s.getRowNo());
                    bin.put("colNo", s.getColNo());
                    bin.put("zoneName", zone.getZoneName());
                    bin.put("locationName", loc.getLocationName());
                    bin.put("slotLabel", warehouseSlotService.formatSlotLabel(s.getSlotCode()));
                    return bin;
                }).toList();

                Map<String, Object> locView = new LinkedHashMap<>();
                locView.put("id", loc.getLocationCode());
                locView.put("name", loc.getLocationName());
                locView.put("materialName", primaryMaterial);
                locView.put("capacity", cap);
                locView.put("occupied", occ);
                locView.put("bins", binViews);
                locationViews.add(locView);
            }

            Map<String, Object> zoneView = new LinkedHashMap<>();
            zoneView.put("id", zoneCodeShort(zone.getZoneCode()));
            zoneView.put("code", zone.getZoneCode());
            zoneView.put("name", zone.getZoneName());
            zoneView.put("description", zone.getDescription());
            zoneView.put("warehouseCode", zone.getWarehouseCode());
            zoneView.put("utilization", rate(zoneOcc, zoneCap));
            zoneView.put("locations", locationViews);
            zoneViews.add(zoneView);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalLocations", locations.size());
        summary.put("totalBins", totalBins);
        summary.put("occupiedBins", occupiedBins);
        summary.put("freeBins", totalBins - occupiedBins);
        summary.put("utilization", rate(occupiedBins, totalBins));
        summary.put("occupiedLocations", occupiedLocations);
        summary.put("emptyLocations", emptyLocations);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("zones", zoneViews);
        return result;
    }

    private List<WarehouseZone> safeListZones() {
        try {
            return warehouseLocationMapper.listZones();
        } catch (Exception e) {
            if (isMissingSchema(e)) return List.of();
            throw e;
        }
    }

    private List<WarehouseLocation> safeListLocations() {
        try {
            return warehouseLocationMapper.listLocations();
        } catch (Exception e) {
            if (isMissingSchema(e)) return List.of();
            throw e;
        }
    }

    private List<WarehouseLocationSlot> safeListSlots() {
        try {
            return warehouseLocationMapper.listSlots();
        } catch (Exception e) {
            if (isMissingSchema(e)) return List.of();
            throw e;
        }
    }

    private static int rate(int occupied, int capacity) {
        if (capacity <= 0) return 0;
        return Math.round(occupied * 100f / capacity);
    }

    private static String zoneCodeShort(String code) {
        if (code == null) return "";
        int dash = code.indexOf('-');
        return dash > 0 ? code.substring(0, dash) : code;
    }

    private static boolean isMissingSchema(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        return msg.contains("warehouse_zone") || msg.contains("warehouse_location")
                || msg.contains("doesn't exist") || msg.contains("不存在");
    }
}
