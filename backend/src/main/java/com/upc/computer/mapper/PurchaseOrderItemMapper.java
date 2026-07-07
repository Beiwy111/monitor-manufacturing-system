package com.upc.computer.mapper;

import com.upc.computer.entity.PurchaseOrderItem;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface PurchaseOrderItemMapper {

    // 查询所有采购明细
    @Select("SELECT purchase_order_item_id, purchase_order_id, material_id, quantity, received_quantity, unit, unit_price, line_amount, item_status, created_at, updated_at FROM purchase_order_item")
    public ArrayList<PurchaseOrderItem> purchaseOrderItemList();

    // 根据主键查询采购明细
    @Select("SELECT purchase_order_item_id, purchase_order_id, material_id, quantity, received_quantity, unit, unit_price, line_amount, item_status, created_at, updated_at FROM purchase_order_item WHERE purchase_order_item_id = #{purchaseOrderItemId}")
    public PurchaseOrderItem getPurchaseOrderItemById(Long purchaseOrderItemId);

    // 新增采购明细
    @Insert("INSERT INTO purchase_order_item (purchase_order_item_id, purchase_order_id, material_id, quantity, received_quantity, unit, unit_price, line_amount, item_status, created_at, updated_at) VALUES (#{purchaseOrderItemId}, #{purchaseOrderId}, #{materialId}, #{quantity}, #{receivedQuantity}, #{unit}, #{unitPrice}, #{lineAmount}, #{itemStatus}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "purchaseOrderItemId")
    public void insertPurchaseOrderItem(PurchaseOrderItem purchaseOrderItem);

    // 修改采购明细
    @Update("UPDATE purchase_order_item SET purchase_order_id=#{purchaseOrderId}, material_id=#{materialId}, quantity=#{quantity}, received_quantity=#{receivedQuantity}, unit=#{unit}, unit_price=#{unitPrice}, line_amount=#{lineAmount}, item_status=#{itemStatus}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE purchase_order_item_id = #{purchaseOrderItemId}")
    public void updatePurchaseOrderItem(PurchaseOrderItem purchaseOrderItem);

    // 删除采购明细
    @Delete("DELETE FROM purchase_order_item WHERE purchase_order_item_id = #{purchaseOrderItemId}")
    public void deletePurchaseOrderItem(Long purchaseOrderItemId);

}
