package com.upc.computer.mapper;

import com.upc.computer.entity.ProdDowntimeReason;
import java.time.LocalDate;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ProdDowntimeReasonMapper {

    @Select("SELECT reason_id, stat_date, reason_code, reason_name, downtime_minutes, occurrence_count, created_at, updated_at FROM prod_downtime_reason WHERE stat_date = #{statDate} ORDER BY downtime_minutes DESC")
    ArrayList<ProdDowntimeReason> listByDate(LocalDate statDate);

    @Select("SELECT COUNT(1) FROM prod_downtime_reason WHERE stat_date = #{statDate}")
    int countByDate(LocalDate statDate);

    @Insert("INSERT INTO prod_downtime_reason (stat_date, reason_code, reason_name, downtime_minutes, occurrence_count) VALUES (#{statDate}, #{reasonCode}, #{reasonName}, #{downtimeMinutes}, #{occurrenceCount})")
    @Options(useGeneratedKeys = true, keyProperty = "reasonId")
    void insert(ProdDowntimeReason reason);

    @Update("UPDATE prod_downtime_reason SET downtime_minutes=#{downtimeMinutes}, occurrence_count=#{occurrenceCount}, updated_at=NOW() WHERE reason_id=#{reasonId}")
    void update(ProdDowntimeReason reason);
}
