package com.upc.computer.mapper;

import com.upc.computer.entity.Supplier;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SupplierMapper {

    @Select("SELECT supplier_id, supplier_no, supplier_name, contact_person, contact_phone, contact_email, address, supply_materials, status, remark, created_at, updated_at FROM supplier ORDER BY created_at DESC")
    List<Supplier> listAll();

    @Select("SELECT supplier_id, supplier_no, supplier_name, contact_person, contact_phone, contact_email, address, supply_materials, status, remark, created_at, updated_at FROM supplier WHERE supplier_id = #{supplierId}")
    Supplier getById(Long supplierId);

    @Insert("INSERT INTO supplier (supplier_no, supplier_name, contact_person, contact_phone, contact_email, address, supply_materials, status, remark, created_at, updated_at) VALUES (#{supplierNo}, #{supplierName}, #{contactPerson}, #{contactPhone}, #{contactEmail}, #{address}, #{supplyMaterials}, #{status}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "supplierId")
    void insert(Supplier supplier);

    @Update("UPDATE supplier SET supplier_name=#{supplierName}, contact_person=#{contactPerson}, contact_phone=#{contactPhone}, contact_email=#{contactEmail}, address=#{address}, supply_materials=#{supplyMaterials}, status=#{status}, remark=#{remark}, updated_at=#{updatedAt} WHERE supplier_id=#{supplierId}")
    void update(Supplier supplier);

    @Delete("DELETE FROM supplier WHERE supplier_id = #{supplierId}")
    void delete(Long supplierId);

    @Select("SELECT supplier_id, supplier_name, contact_person, contact_phone FROM supplier WHERE status = 'ACTIVE' ORDER BY supplier_name")
    List<Supplier> listActive();
}
