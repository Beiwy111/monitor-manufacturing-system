package com.upc.computer.controller;

import com.upc.computer.common.Result;
import com.upc.computer.entity.Supplier;
import com.upc.computer.mapper.SupplierMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/purchase/supplier")
public class SupplierController {

    @Autowired
    private SupplierMapper supplierMapper;

    @GetMapping("/list")
    public Result<List<Supplier>> list() {
        return Result.success(supplierMapper.listAll());
    }

    @GetMapping("/active")
    public Result<List<Supplier>> listActive() {
        return Result.success(supplierMapper.listActive());
    }

    @PostMapping("/add")
    public Result<Supplier> add(@RequestBody Supplier supplier) {
        if (supplier.getSupplierName() == null || supplier.getSupplierName().isBlank()) {
            return Result.fail("供应商名称不能为空");
        }
        supplier.setSupplierNo(generateNo());
        if (supplier.getStatus() == null) supplier.setStatus("ACTIVE");
        supplier.setCreatedAt(LocalDateTime.now());
        supplier.setUpdatedAt(LocalDateTime.now());
        supplierMapper.insert(supplier);
        return Result.success(supplier);
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody Supplier supplier) {
        if (supplier.getSupplierId() == null) {
            return Result.fail("供应商ID不能为空");
        }
        supplier.setUpdatedAt(LocalDateTime.now());
        supplierMapper.update(supplier);
        return Result.success();
    }

    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam Long supplierId) {
        supplierMapper.delete(supplierId);
        return Result.success();
    }

    private String generateNo() {
        return "SUP" + System.currentTimeMillis() % 1000000;
    }
}
