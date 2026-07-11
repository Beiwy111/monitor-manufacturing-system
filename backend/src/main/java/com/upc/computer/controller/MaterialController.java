package com.upc.computer.controller;

import com.upc.computer.service.MaterialService;
import com.upc.computer.common.Result;
import com.upc.computer.entity.Material;
import com.upc.computer.entity.Bom;
import com.upc.computer.entity.Inventory;
import com.upc.computer.entity.InventoryTransaction;
import com.upc.computer.mapper.InventoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/material")
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    @Autowired
    private InventoryMapper inventoryMapper;

    @RequestMapping("/material/list")
    public ArrayList<Material> materialList() {
        return materialService.materialList();
    }

    @RequestMapping("/material/get")
    public Material getMaterialById(Long materialId) {
        return materialService.getMaterialById(materialId);
    }

    @RequestMapping("/material/insert")
    public void insertMaterial(Material material) {
        materialService.insertMaterial(material);
    }

    @RequestMapping("/material/update")
    public void updateMaterial(Material material) {
        materialService.updateMaterial(material);
    }

    @RequestMapping("/material/delete")
    public void deleteMaterial(Long materialId) {
        materialService.deleteMaterial(materialId);
    }

    @PostMapping("/material/bindSupplier")
    public Result<Void> bindSupplier(@RequestParam Long materialId,
                                     @RequestParam(required = false) Long supplierId) {
        materialService.bindSupplier(materialId, supplierId);
        return Result.success();
    }

    @RequestMapping("/bom/list")
    public ArrayList<Bom> bomList() {
        return materialService.bomList();
    }

    @RequestMapping("/bom/get")
    public Bom getBomById(Long bomId) {
        return materialService.getBomById(bomId);
    }

    @RequestMapping("/bom/insert")
    public void insertBom(Bom bom) {
        materialService.insertBom(bom);
    }

    @RequestMapping("/bom/update")
    public void updateBom(Bom bom) {
        materialService.updateBom(bom);
    }

    @RequestMapping("/bom/delete")
    public void deleteBom(Long bomId) {
        materialService.deleteBom(bomId);
    }

    /** 库存列表（含物料名称、单位、安全库存，JOIN material 表） */
    @GetMapping("/inventory/full")
    public Result<List<Inventory>> inventoryFull() {
        return Result.success(inventoryMapper.inventoryListWithMaterial());
    }

    @RequestMapping("/inventory/list")
    public ArrayList<Inventory> inventoryList() {
        return materialService.inventoryList();
    }

    @RequestMapping("/inventory/get")
    public Inventory getInventoryById(Long inventoryId) {
        return materialService.getInventoryById(inventoryId);
    }

    @RequestMapping("/inventory/insert")
    public void insertInventory(Inventory inventory) {
        materialService.insertInventory(inventory);
    }

    @RequestMapping("/inventory/update")
    public void updateInventory(Inventory inventory) {
        materialService.updateInventory(inventory);
    }

    @RequestMapping("/inventory/delete")
    public void deleteInventory(Long inventoryId) {
        materialService.deleteInventory(inventoryId);
    }

    @RequestMapping("/transaction/list")
    public ArrayList<InventoryTransaction> transactionList() {
        return materialService.transactionList();
    }

    @RequestMapping("/transaction/get")
    public InventoryTransaction getTransactionById(Long transactionId) {
        return materialService.getTransactionById(transactionId);
    }

    @RequestMapping("/transaction/insert")
    public void insertTransaction(InventoryTransaction transaction) {
        materialService.insertTransaction(transaction);
    }

    @RequestMapping("/transaction/update")
    public void updateTransaction(InventoryTransaction transaction) {
        materialService.updateTransaction(transaction);
    }

    @RequestMapping("/transaction/delete")
    public void deleteTransaction(Long transactionId) {
        materialService.deleteTransaction(transactionId);
    }
}
