package com.upc.computer.mapper;

import com.upc.computer.entity.PurchaseOrder;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper
public interface PurchaseOrderMapper {

    // 查询所有采购订单
    @Select("SELECT purchase_order_id, purchase_order_no, supplier_name, supplier_contact, supplier_phone, purchase_date, expected_arrival_date, total_amount, status, purchaser_id, approved_by, approved_at, remark, created_at, updated_at FROM purchase_order")
    public ArrayList<PurchaseOrder> purchaseOrderList();

    // 根据主键查询采购订单
    @Select("SELECT purchase_order_id, purchase_order_no, supplier_name, supplier_contact, supplier_phone, purchase_date, expected_arrival_date, total_amount, status, purchaser_id, approved_by, approved_at, remark, created_at, updated_at FROM purchase_order WHERE purchase_order_id = #{purchaseOrderId}")
    public PurchaseOrder getPurchaseOrderById(Long purchaseOrderId);

    // 新增采购订单
    @Insert("INSERT INTO purchase_order (purchase_order_id, purchase_order_no, supplier_name, supplier_contact, supplier_phone, purchase_date, expected_arrival_date, total_amount, status, purchaser_id, approved_by, approved_at, remark, created_at, updated_at) VALUES (#{purchaseOrderId}, #{purchaseOrderNo}, #{supplierName}, #{supplierContact}, #{supplierPhone}, #{purchaseDate}, #{expectedArrivalDate}, #{totalAmount}, #{status}, #{purchaserId}, #{approvedBy}, #{approvedAt}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "purchaseOrderId")
    public void insertPurchaseOrder(PurchaseOrder purchaseOrder);

    // 修改采购订单
    @Update("UPDATE purchase_order SET purchase_order_no=#{purchaseOrderNo}, supplier_name=#{supplierName}, supplier_contact=#{supplierContact}, supplier_phone=#{supplierPhone}, purchase_date=#{purchaseDate}, expected_arrival_date=#{expectedArrivalDate}, total_amount=#{totalAmount}, status=#{status}, purchaser_id=#{purchaserId}, approved_by=#{approvedBy}, approved_at=#{approvedAt}, remark=#{remark}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE purchase_order_id = #{purchaseOrderId}")
    public void updatePurchaseOrder(PurchaseOrder purchaseOrder);

    // 删除采购订单
    @Delete("DELETE FROM purchase_order WHERE purchase_order_id = #{purchaseOrderId}")
    public void deletePurchaseOrder(Long purchaseOrderId);

}
