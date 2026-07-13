package com.upc.computer.mapper;

import com.upc.computer.entity.ProdShiftCapacity;
import java.time.LocalDate;
import java.util.ArrayList;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ProdShiftCapacityMapper {

    @Select("SELECT capacity_id, stat_date, team_code, team_name, leader_name, planned_qty, actual_qty, yield_rate, rank_no, created_at, updated_at FROM prod_shift_capacity WHERE stat_date = #{statDate} ORDER BY rank_no")
    ArrayList<ProdShiftCapacity> listByDate(LocalDate statDate);

    @Select("SELECT COUNT(1) FROM prod_shift_capacity WHERE stat_date = #{statDate}")
    int countByDate(LocalDate statDate);

    @Insert("INSERT INTO prod_shift_capacity (stat_date, team_code, team_name, leader_name, planned_qty, actual_qty, yield_rate, rank_no) VALUES (#{statDate}, #{teamCode}, #{teamName}, #{leaderName}, #{plannedQty}, #{actualQty}, #{yieldRate}, #{rankNo})")
    @Options(useGeneratedKeys = true, keyProperty = "capacityId")
    void insert(ProdShiftCapacity capacity);

    @Update("UPDATE prod_shift_capacity SET planned_qty=#{plannedQty}, actual_qty=#{actualQty}, yield_rate=#{yieldRate}, rank_no=#{rankNo}, updated_at=NOW() WHERE capacity_id=#{capacityId}")
    void update(ProdShiftCapacity capacity);
}
