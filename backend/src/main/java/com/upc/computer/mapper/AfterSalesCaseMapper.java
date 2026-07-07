package com.upc.computer.mapper;

import com.upc.computer.entity.AfterSalesCase;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;

@Mapper
public interface AfterSalesCaseMapper {

    // 查询所有售后案例
    @Select("SELECT case_no, order_id, delivery_id, material_id, batch_no, customer_name, contact_name, contact_phone, problem_description, problem_type, case_level, case_status, trace_result, handle_result, service_user_id, opened_at, closed_at, created_at, updated_at FROM after_sales_case")
    public ArrayList<AfterSalesCase> afterSalesCaseList();

    // 根据主键查询售后案例
    @Select("SELECT case_no, order_id, delivery_id, material_id, batch_no, customer_name, contact_name, contact_phone, problem_description, problem_type, case_level, case_status, trace_result, handle_result, service_user_id, opened_at, closed_at, created_at, updated_at FROM after_sales_case WHERE case_no = #{caseNo}")
    public AfterSalesCase getAfterSalesCaseById(String caseNo);

    // 新增售后案例
    @Insert("INSERT INTO after_sales_case (case_no, order_id, delivery_id, material_id, batch_no, customer_name, contact_name, contact_phone, problem_description, problem_type, case_level, case_status, trace_result, handle_result, service_user_id, opened_at, closed_at, created_at, updated_at) VALUES (#{caseNo}, #{orderId}, #{deliveryId}, #{materialId}, #{batchNo}, #{customerName}, #{contactName}, #{contactPhone}, #{problemDescription}, #{problemType}, #{caseLevel}, #{caseStatus}, #{traceResult}, #{handleResult}, #{serviceUserId}, #{openedAt}, #{closedAt}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "caseNo")
    public void insertAfterSalesCase(AfterSalesCase afterSalesCase);

    // 修改售后案例
    @Update("UPDATE after_sales_case SET order_id=#{orderId}, delivery_id=#{deliveryId}, material_id=#{materialId}, batch_no=#{batchNo}, customer_name=#{customerName}, contact_name=#{contactName}, contact_phone=#{contactPhone}, problem_description=#{problemDescription}, problem_type=#{problemType}, case_level=#{caseLevel}, case_status=#{caseStatus}, trace_result=#{traceResult}, handle_result=#{handleResult}, service_user_id=#{serviceUserId}, opened_at=#{openedAt}, closed_at=#{closedAt}, created_at=#{createdAt}, updated_at=#{updatedAt} WHERE case_no = #{caseNo}")
    public void updateAfterSalesCase(AfterSalesCase afterSalesCase);

    // 删除售后案例
    @Delete("DELETE FROM after_sales_case WHERE case_no = #{caseNo}")
    public void deleteAfterSalesCase(String caseNo);

}
