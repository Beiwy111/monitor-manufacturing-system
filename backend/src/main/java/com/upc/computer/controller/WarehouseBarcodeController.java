package com.upc.computer.controller;

import com.upc.computer.common.Result;
import com.upc.computer.entity.BarcodeRule;
import com.upc.computer.service.WarehouseBarcodeService;
import com.upc.computer.service.WarehouseLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 仓储条码、批次、扫码接口。 */
@RestController
@RequestMapping("/warehouse")
public class WarehouseBarcodeController {
    @Autowired
    private WarehouseBarcodeService warehouseBarcodeService;
    @Autowired
    private WarehouseLocationService warehouseLocationService;

    @GetMapping("/inventory/list")
    public Result<Object> inventoryList() {
        return Result.success(warehouseBarcodeService.inventoryList());
    }

    @GetMapping("/inventory/catalog")
    public Result<Object> inventoryCatalog() {
        return Result.success(warehouseBarcodeService.inventoryCatalog());
    }

    @GetMapping("/transactions/list")
    public Result<Object> transactionList() {
        return Result.success(warehouseBarcodeService.transactionList());
    }

    @GetMapping("/barcode/rules")
    public Result<Object> rules() {
        return Result.success(warehouseBarcodeService.rules());
    }

    @PostMapping("/barcode/rules")
    public Result<Object> saveRule(@RequestBody BarcodeRule rule) {
        return Result.success(warehouseBarcodeService.saveRule(rule));
    }

    @PostMapping("/barcode/generate")
    public Result<Object> generate(@RequestBody Map<String, Object> body) {
        String businessType = body != null ? String.valueOf(body.getOrDefault("businessType", "PRODUCT_IN")) : "PRODUCT_IN";
        return Result.success(Map.of("barcodeNo", warehouseBarcodeService.generateBarcode(businessType)));
    }

    @PostMapping("/barcode/scan")
    public Result<Object> scan(@RequestBody Map<String, Object> body,
                               @RequestParam(required = false, defaultValue = "system") String operator) {
        return Result.success(warehouseBarcodeService.scanBarcode(body != null ? body : Map.of(), operator));
    }

    @GetMapping("/barcode/list")
    public Result<Object> barcodeList() {
        return Result.success(warehouseBarcodeService.barcodeList());
    }

    @GetMapping("/barcode/trace/{barcodeNo}")
    public Result<Object> trace(@PathVariable String barcodeNo) {
        return Result.success(warehouseBarcodeService.queryBarcodeTrace(barcodeNo));
    }

    @GetMapping("/scan/logs")
    public Result<Object> scanLogs() {
        return Result.success(warehouseBarcodeService.scanLogs());
    }

    @GetMapping("/location-map")
    public Result<Object> locationMap() {
        return Result.success(warehouseLocationService.locationMap());
    }
}
