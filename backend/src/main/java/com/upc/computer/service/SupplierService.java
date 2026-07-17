package com.upc.computer.service;

import com.upc.computer.common.BusinessException;
import com.upc.computer.entity.Supplier;
import com.upc.computer.mapper.SupplierMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SupplierService {

    private final SupplierMapper supplierMapper;

    public SupplierService(SupplierMapper supplierMapper) {
        this.supplierMapper = supplierMapper;
    }

    public List<Supplier> listAll() {
        return supplierMapper.listAll();
    }

    public List<Supplier> listActive() {
        return supplierMapper.listActive();
    }

    public Supplier getById(Long supplierId) {
        return listAll().stream().filter(item -> supplierId != null && supplierId.equals(item.getSupplierId()))
                .findFirst().orElse(null);
    }

    @Transactional
    public Supplier create(Supplier supplier) {
        if (supplier == null || supplier.getSupplierName() == null || supplier.getSupplierName().isBlank()) {
            throw new BusinessException("供应商名称不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        supplier.setSupplierNo("SUP" + System.currentTimeMillis() % 1000000);
        if (supplier.getStatus() == null) supplier.setStatus("ACTIVE");
        supplier.setCreatedAt(now);
        supplier.setUpdatedAt(now);
        supplierMapper.insert(supplier);
        return supplier;
    }

    @Transactional
    public Supplier update(Supplier supplier) {
        if (supplier == null || supplier.getSupplierId() == null) throw new BusinessException("供应商ID不能为空");
        supplier.setUpdatedAt(LocalDateTime.now());
        supplierMapper.update(supplier);
        return supplier;
    }

    @Transactional
    public void delete(Long supplierId) {
        if (supplierId == null) throw new BusinessException("供应商ID不能为空");
        supplierMapper.delete(supplierId);
    }
}
