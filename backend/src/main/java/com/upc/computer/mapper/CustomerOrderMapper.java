package com.upc.computer.mapper;

import com.upc.computer.entity.CustomerOrder;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper
public interface CustomerOrderMapper {

    // 查询所有客户订单
    @Select("SELECT order_id, order_no, customer_name, customer_contact, customer_phone, order_date, required_delivery_date, order_amount, audit_status, audit_user_id, audit_at, audit_opinion, remark, created_by, created_at, updated_at FROM customer_order ORDER BY created_at DESC, order_id DESC")
    public ArrayList<CustomerOrder> customerOrderList();

    // 根据主键查询客户订单
    @Select("SELECT order_id, order_no, customer_name, customer_contact, customer_phone, order_date, required_delivery_date, order_amount, audit_status, audit_user_id, audit_at, audit_opinion, remark, created_by, created_at, updated_at FROM customer_order WHERE order_id = #{orderId}")
    public CustomerOrder getCustomerOrderById(Long orderId);

    // 新增客户订单
    @Insert("INSERT INTO customer_order (order_id, order_no, customer_name, customer_contact, customer_phone, order_date, required_delivery_date, order_amount, audit_status, audit_user_id, audit_at, audit_opinion, remark, created_by, created_at, updated_at) VALUES (#{orderId}, #{orderNo}, #{customerName}, #{customerContact}, #{customerPhone}, #{orderDate}, #{requiredDeliveryDate}, #{orderAmount}, #{auditStatus}, #{auditUserId}, #{auditAt}, #{auditOpinion}, #{remark}, #{createdBy}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "orderId")
    public void insertCustomerOrder(CustomerOrder customerOrder);

    // 修改客户订单
    @Update("UPDATE customer_order SET order_no=#{orderNo}, customer_name=#{customerName}, customer_contact=#{customerContact}, customer_phone=#{customerPhone}, order_date=#{orderDate}, required_delivery_date=#{requiredDeliveryDate}, order_amount=#{orderAmount}, audit_status=#{auditStatus}, audit_user_id=#{auditUserId}, audit_at=#{auditAt}, audit_opinion=#{auditOpinion}, remark=#{remark}, created_by=#{createdBy}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE order_id = #{orderId}")
    public void updateCustomerOrder(CustomerOrder customerOrder);

    // 删除客户订单
    @Delete("DELETE FROM customer_order WHERE order_id = #{orderId}")
    public void deleteCustomerOrder(Long orderId);

}
