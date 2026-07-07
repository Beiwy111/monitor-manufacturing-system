package com.upc.computer.service;

import com.upc.computer.entity.Material;
import com.upc.computer.entity.Bom;
import com.upc.computer.entity.Inventory;
import com.upc.computer.entity.InventoryTransaction;
import java.util.ArrayList;

public interface MaterialService {

    public ArrayList<Material> materialList();

    public Material getMaterialById(Long materialId);

    public void insertMaterial(Material material);

    public void updateMaterial(Material material);

    public void deleteMaterial(Long materialId);

    public ArrayList<Bom> bomList();

    public Bom getBomById(Long bomId);

    public void insertBom(Bom bom);

    public void updateBom(Bom bom);

    public void deleteBom(Long bomId);

    public ArrayList<Inventory> inventoryList();

    public Inventory getInventoryById(Long inventoryId);

    public void insertInventory(Inventory inventory);

    public void updateInventory(Inventory inventory);

    public void deleteInventory(Long inventoryId);

    public ArrayList<InventoryTransaction> transactionList();

    public InventoryTransaction getTransactionById(Long transactionId);

    public void insertTransaction(InventoryTransaction transaction);

    public void updateTransaction(InventoryTransaction transaction);

    public void deleteTransaction(Long transactionId);

}
