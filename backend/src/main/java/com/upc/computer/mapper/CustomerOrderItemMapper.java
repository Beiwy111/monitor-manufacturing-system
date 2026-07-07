package com.upc.computer.mapper;

import com.upc.computer.entity.CustomerOrderItem;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper
public interface CustomerOrderItemMapper {

    // 查询所有订单明细
    @Select("SELECT order_item_id, order_id, material_id, product_name, specification, quantity, unit, unit_price, line_amount, delivery_date, item_status, created_at, updated_at FROM customer_order_item")
    public ArrayList<CustomerOrderItem> orderItemList();

    // 根据主键查询订单明细
    @Select("SELECT order_item_id, order_id, material_id, product_name, specification, quantity, unit, unit_price, line_amount, delivery_date, item_status, created_at, updated_at FROM customer_order_item WHERE order_item_id = #{orderItemId}")
    public CustomerOrderItem getOrderItemById(Long orderItemId);

    // 新增订单明细
    @Insert("INSERT INTO customer_order_item (order_item_id, order_id, material_id, product_name, specification, quantity, unit, unit_price, line_amount, delivery_date, item_status, created_at, updated_at) VALUES (#{orderItemId}, #{orderId}, #{materialId}, #{productName}, #{specification}, #{quantity}, #{unit}, #{unitPrice}, #{lineAmount}, #{deliveryDate}, #{itemStatus}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "orderItemId")
    public void insertOrderItem(CustomerOrderItem orderItem);

    // 修改订单明细
    @Update("UPDATE customer_order_item SET order_id=#{orderId}, material_id=#{materialId}, product_name=#{productName}, specification=#{specification}, quantity=#{quantity}, unit=#{unit}, unit_price=#{unitPrice}, line_amount=#{lineAmount}, delivery_date=#{deliveryDate}, item_status=#{itemStatus}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE order_item_id = #{orderItemId}")
    public void updateOrderItem(CustomerOrderItem orderItem);

    // 删除订单明细
    @Delete("DELETE FROM customer_order_item WHERE order_item_id = #{orderItemId}")
    public void deleteOrderItem(Long orderItemId);

}
