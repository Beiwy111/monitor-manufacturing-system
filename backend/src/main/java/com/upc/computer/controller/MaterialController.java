package com.upc.computer.controller;

import com.upc.computer.service.MaterialService;
import com.upc.computer.entity.Material;
import com.upc.computer.entity.Bom;
import com.upc.computer.entity.Inventory;
import com.upc.computer.entity.InventoryTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;

@RestController
@RequestMapping("/material")
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    // 查询物料列表
    @RequestMapping("/material/list")
    public ArrayList<Material> materialList() {
        return materialService.materialList();
    }

    // 根据主键查询物料
    @RequestMapping("/material/get")
    public Material getMaterialById(Long materialId) {
        return materialService.getMaterialById(materialId);
    }

    // 新增物料
    @RequestMapping("/material/insert")
    public void insertMaterial(Material material) {
        materialService.insertMaterial(material);
    }

    // 修改物料
    @RequestMapping("/material/update")
    public void updateMaterial(Material material) {
        materialService.updateMaterial(material);
    }

    // 删除物料
    @RequestMapping("/material/delete")
    public void deleteMaterial(Long materialId) {
        materialService.deleteMaterial(materialId);
    }

    // 查询BOM列表
    @RequestMapping("/bom/list")
    public ArrayList<Bom> bomList() {
        return materialService.bomList();
    }

    // 根据主键查询BOM
    @RequestMapping("/bom/get")
    public Bom getBomById(Long bomId) {
        return materialService.getBomById(bomId);
    }

    // 新增BOM
    @RequestMapping("/bom/insert")
    public void insertBom(Bom bom) {
        materialService.insertBom(bom);
    }

    // 修改BOM
    @RequestMapping("/bom/update")
    public void updateBom(Bom bom) {
        materialService.updateBom(bom);
    }

    // 删除BOM
    @RequestMapping("/bom/delete")
    public void deleteBom(Long bomId) {
        materialService.deleteBom(bomId);
    }

    // 查询库存列表
    @RequestMapping("/inventory/list")
    public ArrayList<Inventory> inventoryList() {
        return materialService.inventoryList();
    }

    // 根据主键查询库存
    @RequestMapping("/inventory/get")
    public Inventory getInventoryById(Long inventoryId) {
        return materialService.getInventoryById(inventoryId);
    }

    // 新增库存
    @RequestMapping("/inventory/insert")
    public void insertInventory(Inventory inventory) {
        materialService.insertInventory(inventory);
    }

    // 修改库存
    @RequestMapping("/inventory/update")
    public void updateInventory(Inventory inventory) {
        materialService.updateInventory(inventory);
    }

    // 删除库存
    @RequestMapping("/inventory/delete")
    public void deleteInventory(Long inventoryId) {
        materialService.deleteInventory(inventoryId);
    }

    // 查询库存流水列表
    @RequestMapping("/transaction/list")
    public ArrayList<InventoryTransaction> transactionList() {
        return materialService.transactionList();
    }

    // 根据主键查询库存流水
    @RequestMapping("/transaction/get")
    public InventoryTransaction getTransactionById(Long transactionId) {
        return materialService.getTransactionById(transactionId);
    }

    // 新增库存流水
    @RequestMapping("/transaction/insert")
    public void insertTransaction(InventoryTransaction transaction) {
        materialService.insertTransaction(transaction);
    }

    // 修改库存流水
    @RequestMapping("/transaction/update")
    public void updateTransaction(InventoryTransaction transaction) {
        materialService.updateTransaction(transaction);
    }

    // 删除库存流水
    @RequestMapping("/transaction/delete")
    public void deleteTransaction(Long transactionId) {
        materialService.deleteTransaction(transactionId);
    }

}
