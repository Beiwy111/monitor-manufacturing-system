package com.upc.computer.mapper;

import com.upc.computer.entity.ProdHourlyMetric;
import java.time.LocalDate;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ProdHourlyMetricMapper {

    @Select("SELECT metric_id, stat_date, stat_hour, planned_output, actual_output, qualified_qty, unqualified_qty, alarm_count, created_at, updated_at FROM prod_hourly_metric WHERE stat_date = #{statDate} ORDER BY stat_hour")
    ArrayList<ProdHourlyMetric> listByDate(LocalDate statDate);

    @Select("SELECT metric_id, stat_date, stat_hour, planned_output, actual_output, qualified_qty, unqualified_qty, alarm_count, created_at, updated_at FROM prod_hourly_metric WHERE stat_date = #{statDate} AND stat_hour = #{statHour} LIMIT 1")
    ProdHourlyMetric getByDateHour(@Param("statDate") LocalDate statDate, @Param("statHour") int statHour);

    @Select("SELECT COUNT(1) FROM prod_hourly_metric WHERE stat_date = #{statDate}")
    int countByDate(LocalDate statDate);

    @Insert("INSERT INTO prod_hourly_metric (stat_date, stat_hour, planned_output, actual_output, qualified_qty, unqualified_qty, alarm_count) VALUES (#{statDate}, #{statHour}, #{plannedOutput}, #{actualOutput}, #{qualifiedQty}, #{unqualifiedQty}, #{alarmCount})")
    @Options(useGeneratedKeys = true, keyProperty = "metricId")
    void insert(ProdHourlyMetric metric);

    @Update("UPDATE prod_hourly_metric SET planned_output=#{plannedOutput}, actual_output=#{actualOutput}, qualified_qty=#{qualifiedQty}, unqualified_qty=#{unqualifiedQty}, alarm_count=#{alarmCount}, updated_at=NOW() WHERE metric_id=#{metricId}")
    void update(ProdHourlyMetric metric);
}
