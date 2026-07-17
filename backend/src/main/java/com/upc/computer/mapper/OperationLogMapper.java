package com.upc.computer.mapper;

import com.upc.computer.entity.OperationLog;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;

@Mapper
public interface OperationLogMapper {

    // 查询所有操作日志
    @Select("SELECT log_id, user_id, module_name, operation_type, business_table, business_id, operation_content, ip_address, result_status, error_message, operated_at FROM operation_log ORDER BY operated_at DESC, log_id DESC")
    public ArrayList<OperationLog> operationLogList();

    // 根据主键查询操作日志
    @Select("SELECT log_id, user_id, module_name, operation_type, business_table, business_id, operation_content, ip_address, result_status, error_message, operated_at FROM operation_log WHERE log_id = #{logId}")
    public OperationLog getOperationLogById(Long logId);

    // 新增操作日志
    @Insert("INSERT INTO operation_log (log_id, user_id, module_name, operation_type, business_table, business_id, operation_content, ip_address, result_status, error_message, operated_at) VALUES (#{logId}, #{userId}, #{moduleName}, #{operationType}, #{businessTable}, #{businessId}, #{operationContent}, #{ipAddress}, #{resultStatus}, #{errorMessage}, #{operatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "logId")
    public void insertOperationLog(OperationLog operationLog);

    // 修改操作日志
    @Update("UPDATE operation_log SET user_id=#{userId}, module_name=#{moduleName}, operation_type=#{operationType}, business_table=#{businessTable}, business_id=#{businessId}, operation_content=#{operationContent}, ip_address=#{ipAddress}, result_status=#{resultStatus}, error_message=#{errorMessage}, operated_at=#{operatedAt} WHERE log_id = #{logId}")
    public void updateOperationLog(OperationLog operationLog);

    // 删除操作日志
    @Delete("DELETE FROM operation_log WHERE log_id = #{logId}")
    public void deleteOperationLog(Long logId);

}
