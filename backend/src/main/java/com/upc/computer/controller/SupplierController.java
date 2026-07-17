package com.upc.computer.controller;

import com.upc.computer.common.Result;
import com.upc.computer.entity.Supplier;
import com.upc.computer.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/purchase/supplier")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @GetMapping("/list")
    public Result<List<Supplier>> list() {
        return Result.success(supplierService.listAll());
    }

    @GetMapping("/active")
    public Result<List<Supplier>> listActive() {
        return Result.success(supplierService.listActive());
    }

    @PostMapping("/add")
    public Result<Supplier> add(@RequestBody Supplier supplier) {
        return Result.success(supplierService.create(supplier));
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody Supplier supplier) {
        supplierService.update(supplier);
        return Result.success();
    }

    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam Long supplierId) {
        supplierService.delete(supplierId);
        return Result.success();
    }
}
