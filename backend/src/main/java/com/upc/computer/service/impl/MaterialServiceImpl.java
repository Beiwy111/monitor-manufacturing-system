package com.upc.computer.service.impl;

import com.upc.computer.service.MaterialService;
import com.upc.computer.entity.Material;
import com.upc.computer.mapper.MaterialMapper;
import com.upc.computer.entity.Bom;
import com.upc.computer.mapper.BomMapper;
import com.upc.computer.entity.Inventory;
import com.upc.computer.mapper.InventoryMapper;
import com.upc.computer.entity.InventoryTransaction;
import com.upc.computer.mapper.InventoryTransactionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;

@Service
public class MaterialServiceImpl implements MaterialService {

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private BomMapper bomMapper;

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private InventoryTransactionMapper inventoryTransactionMapper;

    // 查询所有物料
    @Override
    public ArrayList<Material> materialList() {
        return materialMapper.materialList();
    }

    // 根据主键查询物料
    @Override
    public Material getMaterialById(Long materialId) {
        return materialMapper.getMaterialById(materialId);
    }

    // 新增物料
    @Override
    public void insertMaterial(Material material) {
        materialMapper.insertMaterial(material);
    }

    // 修改物料
    @Override
    public void updateMaterial(Material material) {
        materialMapper.updateMaterial(material);
    }

    // 删除物料
    @Override
    public void deleteMaterial(Long materialId) {
        materialMapper.deleteMaterial(materialId);
    }

    @Override
    public void bindSupplier(Long materialId, Long supplierId) {
        materialMapper.updateSupplier(materialId, supplierId);
    }

    // 查询所有BOM
    @Override
    public ArrayList<Bom> bomList() {
        return bomMapper.bomList();
    }

    // 根据主键查询BOM
    @Override
    public Bom getBomById(Long bomId) {
        return bomMapper.getBomById(bomId);
    }

    // 新增BOM
    @Override
    public void insertBom(Bom bom) {
        bomMapper.insertBom(bom);
    }

    // 修改BOM
    @Override
    public void updateBom(Bom bom) {
        bomMapper.updateBom(bom);
    }

    // 删除BOM
    @Override
    public void deleteBom(Long bomId) {
        bomMapper.deleteBom(bomId);
    }

    // 查询所有库存
    @Override
    public ArrayList<Inventory> inventoryList() {
        return inventoryMapper.inventoryList();
    }

    // 根据主键查询库存
    @Override
    public Inventory getInventoryById(Long inventoryId) {
        return inventoryMapper.getInventoryById(inventoryId);
    }

    // 新增库存
    @Override
    public void insertInventory(Inventory inventory) {
        inventoryMapper.insertInventory(inventory);
    }

    // 修改库存
    @Override
    public void updateInventory(Inventory inventory) {
        inventoryMapper.updateInventory(inventory);
    }

    // 删除库存
    @Override
    public void deleteInventory(Long inventoryId) {
        inventoryMapper.deleteInventory(inventoryId);
    }

    // 查询所有库存流水
    @Override
    public ArrayList<InventoryTransaction> transactionList() {
        return inventoryTransactionMapper.transactionList();
    }

    // 根据主键查询库存流水
    @Override
    public InventoryTransaction getTransactionById(Long transactionId) {
        return inventoryTransactionMapper.getTransactionById(transactionId);
    }

    // 新增库存流水
    @Override
    public void insertTransaction(InventoryTransaction transaction) {
        inventoryTransactionMapper.insertTransaction(transaction);
    }

    // 修改库存流水
    @Override
    public void updateTransaction(InventoryTransaction transaction) {
        inventoryTransactionMapper.updateTransaction(transaction);
    }

    // 删除库存流水
    @Override
    public void deleteTransaction(Long transactionId) {
        inventoryTransactionMapper.deleteTransaction(transactionId);
    }

}
