package com.upc.computer.ai.tool;

import com.upc.computer.service.MaterialService;
import com.upc.computer.service.WarehouseBarcodeService;
import com.upc.computer.service.WarehouseLocationService;
import com.upc.computer.service.WarehouseSlotService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** 物料、BOM、库存、条码和库位只读工具。 */
@Component
public class MaterialWarehouseAgentTools {

    private final MaterialService materialService;
    private final WarehouseBarcodeService barcodeService;
    private final WarehouseLocationService locationService;
    private final WarehouseSlotService slotService;

    public MaterialWarehouseAgentTools(MaterialService materialService,
                                       WarehouseBarcodeService barcodeService,
                                       WarehouseLocationService locationService,
                                       WarehouseSlotService slotService) {
        this.materialService = materialService;
        this.barcodeService = barcodeService;
        this.locationService = locationService;
        this.slotService = slotService;
    }

    @Tool(name = "material_list", description = "查询显示器原材料、半成品和成品物料档案")
    public Object listMaterials() {
        return AgentToolSupport.limit(materialService.materialList(), 200);
    }

    @Tool(name = "material_list_bom", description = "查询显示器产品 BOM 组成")
    public Object listBom() {
        return AgentToolSupport.limit(materialService.bomList(), 200);
    }

    @Tool(name = "warehouse_inventory", description = "查询实时库存、可用数量、库位、批次和预警状态")
    public Object inventory() {
        return AgentToolSupport.limit(barcodeService.inventoryList(), 250);
    }

    @Tool(name = "warehouse_inventory_catalog", description = "按物料汇总查询库存目录和总可用数量")
    public Object inventoryCatalog() {
        return AgentToolSupport.limit(barcodeService.inventoryCatalog(), 250);
    }

    @Tool(name = "warehouse_transactions", description = "查询最近的库存收发事务")
    public Object transactions() {
        return AgentToolSupport.limit(barcodeService.transactionList(), 200);
    }

    @Tool(name = "warehouse_barcode_trace", description = "根据库存条码查询物料批次和扫码追溯信息")
    public Object barcodeTrace(@ToolParam(description = "库存条码编号") String barcodeNo) {
        return barcodeService.queryBarcodeTrace(
                AgentToolSupport.requiredText(barcodeNo, "条码编号"));
    }

    @Tool(name = "warehouse_location_map", description = "查询仓库区域、库位、储位占用率和物料分布")
    public Object locationMap() {
        return locationService.locationMap();
    }

    @Tool(name = "warehouse_available_slots", description = "查询可用储位，类别可传 RAW 或 FINISHED，区域编码可不传")
    public Object availableSlots(
            @ToolParam(description = "库存类别：RAW 或 FINISHED", required = false) String category,
            @ToolParam(description = "仓库区域编码，例如 RM-WH", required = false) String zoneCode) {
        return AgentToolSupport.limit(slotService.listAvailableSlots(category, zoneCode), 200);
    }

    @Tool(name = "warehouse_pending_purchase_arrivals", description = "查询等待采购到货入库的采购单和物料")
    public Object pendingPurchaseArrivals() {
        return AgentToolSupport.limit(slotService.listPendingPurchaseArrivals(), 100);
    }
}
