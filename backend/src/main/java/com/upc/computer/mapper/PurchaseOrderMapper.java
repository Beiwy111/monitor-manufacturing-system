package com.upc.computer.mapper;

import com.upc.computer.entity.PurchaseOrder;
import org.apache.ibatis.annotations.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mapper
public interface PurchaseOrderMapper {

    @Select("SELECT DISTINCT supplier_name AS supplierName, MAX(supplier_contact) AS contactPerson, MAX(supplier_phone) AS contactPhone FROM purchase_order WHERE supplier_name IS NOT NULL AND supplier_name != '' GROUP BY supplier_name ORDER BY MAX(updated_at) DESC")
    List<Map<String, String>> selectDistinctSuppliers();

    @Select("SELECT purchase_order_id, purchase_order_no, supplier_name, supplier_contact, supplier_phone, purchase_date, expected_arrival_date, total_amount, status, purchaser_id, approved_by, approved_at, remark, created_at, updated_at FROM purchase_order")
    ArrayList<PurchaseOrder> purchaseOrderList();

    @Select("SELECT purchase_order_id, purchase_order_no, supplier_name, supplier_contact, supplier_phone, purchase_date, expected_arrival_date, total_amount, status, purchaser_id, approved_by, approved_at, remark, created_at, updated_at FROM purchase_order WHERE purchase_order_id = #{purchaseOrderId}")
    PurchaseOrder getPurchaseOrderById(Long purchaseOrderId);

    @Insert("INSERT INTO purchase_order (purchase_order_id, purchase_order_no, supplier_name, supplier_contact, supplier_phone, purchase_date, expected_arrival_date, total_amount, status, purchaser_id, approved_by, approved_at, remark, created_at, updated_at) VALUES (#{purchaseOrderId}, #{purchaseOrderNo}, #{supplierName}, #{supplierContact}, #{supplierPhone}, #{purchaseDate}, #{expectedArrivalDate}, #{totalAmount}, #{status}, #{purchaserId}, #{approvedBy}, #{approvedAt}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "purchaseOrderId")
    void insertPurchaseOrder(PurchaseOrder purchaseOrder);

    @Update("UPDATE purchase_order SET purchase_order_no=#{purchaseOrderNo}, supplier_name=#{supplierName}, supplier_contact=#{supplierContact}, supplier_phone=#{supplierPhone}, purchase_date=#{purchaseDate}, expected_arrival_date=#{expectedArrivalDate}, total_amount=#{totalAmount}, status=#{status}, purchaser_id=#{purchaserId}, approved_by=#{approvedBy}, approved_at=#{approvedAt}, remark=#{remark}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE purchase_order_id = #{purchaseOrderId}")
    void updatePurchaseOrder(PurchaseOrder purchaseOrder);

    @Delete("DELETE FROM purchase_order WHERE purchase_order_id = #{purchaseOrderId}")
    void deletePurchaseOrder(Long purchaseOrderId);
}
