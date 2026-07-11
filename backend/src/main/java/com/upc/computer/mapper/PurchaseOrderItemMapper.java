package com.upc.computer.mapper;

import com.upc.computer.entity.PurchaseOrderItem;
import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface PurchaseOrderItemMapper {

    @Select("SELECT purchase_order_item_id, purchase_order_id, material_id, quantity, received_quantity, unit, unit_price, line_amount, item_status, created_at, updated_at FROM purchase_order_item")
    ArrayList<PurchaseOrderItem> purchaseOrderItemList();

    @Select("SELECT poi.purchase_order_item_id, poi.purchase_order_id, poi.material_id, " +
            "m.material_code, m.material_name, " +
            "poi.quantity, poi.received_quantity, poi.unit, poi.unit_price, poi.line_amount, poi.item_status, poi.created_at, poi.updated_at " +
            "FROM purchase_order_item poi LEFT JOIN material m ON poi.material_id = m.material_id " +
            "WHERE poi.purchase_order_id = #{purchaseOrderId}")
    List<PurchaseOrderItem> listByOrderIdWithMaterial(Long purchaseOrderId);

    @Select("SELECT purchase_order_item_id, purchase_order_id, material_id, quantity, received_quantity, unit, unit_price, line_amount, item_status, created_at, updated_at FROM purchase_order_item WHERE purchase_order_item_id = #{purchaseOrderItemId}")
    PurchaseOrderItem getPurchaseOrderItemById(Long purchaseOrderItemId);

    @Insert("INSERT INTO purchase_order_item (purchase_order_id, material_id, quantity, received_quantity, unit, unit_price, line_amount, item_status, created_at, updated_at) VALUES (#{purchaseOrderId}, #{materialId}, #{quantity}, #{receivedQuantity}, #{unit}, #{unitPrice}, #{lineAmount}, #{itemStatus}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "purchaseOrderItemId")
    void insertPurchaseOrderItem(PurchaseOrderItem purchaseOrderItem);

    @Update("UPDATE purchase_order_item SET purchase_order_id=#{purchaseOrderId}, material_id=#{materialId}, quantity=#{quantity}, received_quantity=#{receivedQuantity}, unit=#{unit}, unit_price=#{unitPrice}, line_amount=#{lineAmount}, item_status=#{itemStatus}, updated_at=#{updatedAt} WHERE purchase_order_item_id = #{purchaseOrderItemId}")
    void updatePurchaseOrderItem(PurchaseOrderItem purchaseOrderItem);

    @Delete("DELETE FROM purchase_order_item WHERE purchase_order_item_id = #{purchaseOrderItemId}")
    void deletePurchaseOrderItem(Long purchaseOrderItemId);
}
