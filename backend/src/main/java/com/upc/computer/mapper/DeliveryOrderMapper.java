package com.upc.computer.mapper;

import com.upc.computer.entity.DeliveryOrder;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper
public interface DeliveryOrderMapper {

    // 查询所有发货单
    @Select("SELECT delivery_id, delivery_no, order_id, work_order_id, customer_name, material_id, batch_no, delivery_quantity, delivery_date, logistics_company, logistics_no, receiver_name, receiver_phone, receiver_address, delivery_status, shipped_by, remark, created_at, updated_at FROM delivery_order ORDER BY created_at DESC, delivery_id DESC")
    public ArrayList<DeliveryOrder> deliveryList();

    // 根据主键查询发货单
    @Select("SELECT delivery_id, delivery_no, order_id, work_order_id, customer_name, material_id, batch_no, delivery_quantity, delivery_date, logistics_company, logistics_no, receiver_name, receiver_phone, receiver_address, delivery_status, shipped_by, remark, created_at, updated_at FROM delivery_order WHERE delivery_id = #{deliveryId}")
    public DeliveryOrder getDeliveryById(Long deliveryId);

    // 新增发货单
    @Insert("INSERT INTO delivery_order (delivery_id, delivery_no, order_id, work_order_id, customer_name, material_id, batch_no, delivery_quantity, delivery_date, logistics_company, logistics_no, receiver_name, receiver_phone, receiver_address, delivery_status, shipped_by, remark, created_at, updated_at) VALUES (#{deliveryId}, #{deliveryNo}, #{orderId}, #{workOrderId}, #{customerName}, #{materialId}, #{batchNo}, #{deliveryQuantity}, #{deliveryDate}, #{logisticsCompany}, #{logisticsNo}, #{receiverName}, #{receiverPhone}, #{receiverAddress}, #{deliveryStatus}, #{shippedBy}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "deliveryId")
    public void insertDelivery(DeliveryOrder delivery);

    // 修改发货单
    @Update("UPDATE delivery_order SET delivery_no=#{deliveryNo}, order_id=#{orderId}, work_order_id=#{workOrderId}, customer_name=#{customerName}, material_id=#{materialId}, batch_no=#{batchNo}, delivery_quantity=#{deliveryQuantity}, delivery_date=#{deliveryDate}, logistics_company=#{logisticsCompany}, logistics_no=#{logisticsNo}, receiver_name=#{receiverName}, receiver_phone=#{receiverPhone}, receiver_address=#{receiverAddress}, delivery_status=#{deliveryStatus}, shipped_by=#{shippedBy}, remark=#{remark}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE delivery_id = #{deliveryId}")
    public void updateDelivery(DeliveryOrder delivery);

    // 删除发货单
    @Delete("DELETE FROM delivery_order WHERE delivery_id = #{deliveryId}")
    public void deleteDelivery(Long deliveryId);

}
