package com.upc.computer.mapper;

import com.upc.computer.entity.ProdLineStation;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ProdLineStationMapper {

    @Select("SELECT station_id, station_code, station_name, line_code, sort_no, station_status, current_qty, throughput_per_hour, equipment_id, work_order_id, work_order_no, alarm_flag, remark, created_at, updated_at FROM prod_line_station ORDER BY sort_no")
    ArrayList<ProdLineStation> listAll();

    @Select("SELECT COUNT(1) FROM prod_line_station")
    int countAll();

    @Insert("INSERT INTO prod_line_station (station_code, station_name, line_code, sort_no, station_status, current_qty, throughput_per_hour, equipment_id, work_order_id, work_order_no, alarm_flag, remark) VALUES (#{stationCode}, #{stationName}, #{lineCode}, #{sortNo}, #{stationStatus}, #{currentQty}, #{throughputPerHour}, #{equipmentId}, #{workOrderId}, #{workOrderNo}, #{alarmFlag}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "stationId")
    void insert(ProdLineStation station);

    @Update("UPDATE prod_line_station SET station_status=#{stationStatus}, current_qty=#{currentQty}, throughput_per_hour=#{throughputPerHour}, equipment_id=#{equipmentId}, work_order_id=#{workOrderId}, work_order_no=#{workOrderNo}, alarm_flag=#{alarmFlag}, remark=#{remark}, updated_at=NOW() WHERE station_id=#{stationId}")
    void update(ProdLineStation station);
}
