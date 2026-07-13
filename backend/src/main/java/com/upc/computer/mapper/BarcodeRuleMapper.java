package com.upc.computer.mapper;

import com.upc.computer.entity.BarcodeRule;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BarcodeRuleMapper {
    @Select("""
            SELECT rule_id AS ruleId, rule_code AS ruleCode, business_type AS businessType, prefix,
                   date_pattern AS datePattern, serial_length AS serialLength, current_serial AS currentSerial,
                   status, created_at AS createdAt, updated_at AS updatedAt
            FROM barcode_rule
            ORDER BY rule_id
            """)
    List<BarcodeRule> ruleList();

    @Select("""
            SELECT rule_id AS ruleId, rule_code AS ruleCode, business_type AS businessType, prefix,
                   date_pattern AS datePattern, serial_length AS serialLength, current_serial AS currentSerial,
                   status, created_at AS createdAt, updated_at AS updatedAt
            FROM barcode_rule
            WHERE business_type = #{businessType} AND status = 1
            LIMIT 1
            """)
    BarcodeRule getEnabledByBusinessType(String businessType);

    @Insert("""
            INSERT INTO barcode_rule (rule_code, business_type, prefix, date_pattern, serial_length, current_serial, status, created_at, updated_at)
            VALUES (#{ruleCode}, #{businessType}, #{prefix}, #{datePattern}, #{serialLength}, #{currentSerial}, #{status}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "ruleId")
    void insertRule(BarcodeRule rule);

    @Update("""
            UPDATE barcode_rule
            SET rule_code=#{ruleCode}, business_type=#{businessType}, prefix=#{prefix}, date_pattern=#{datePattern},
                serial_length=#{serialLength}, current_serial=#{currentSerial}, status=#{status}, updated_at=#{updatedAt}
            WHERE rule_id=#{ruleId}
            """)
    void updateRule(BarcodeRule rule);

    @Update("UPDATE barcode_rule SET current_serial = #{currentSerial}, updated_at = NOW() WHERE rule_id = #{ruleId}")
    void updateSerial(@Param("ruleId") Long ruleId, @Param("currentSerial") Long currentSerial);
}
