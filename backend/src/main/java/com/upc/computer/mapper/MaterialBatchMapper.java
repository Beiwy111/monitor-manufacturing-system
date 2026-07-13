package com.upc.computer.mapper;

import com.upc.computer.entity.MaterialBatch;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MaterialBatchMapper {
    @Select("""
            SELECT batch_id AS batchId, batch_no AS batchNo, material_id AS materialId, source_type AS sourceType,
                   source_no AS sourceNo, quantity, batch_status AS batchStatus, produced_at AS producedAt,
                   received_at AS receivedAt, created_at AS createdAt, updated_at AS updatedAt
            FROM material_batch
            ORDER BY created_at DESC
            """)
    List<MaterialBatch> batchList();

    @Select("""
            SELECT batch_id AS batchId, batch_no AS batchNo, material_id AS materialId, source_type AS sourceType,
                   source_no AS sourceNo, quantity, batch_status AS batchStatus, produced_at AS producedAt,
                   received_at AS receivedAt, created_at AS createdAt, updated_at AS updatedAt
            FROM material_batch
            WHERE batch_no = #{batchNo} AND material_id = #{materialId}
            LIMIT 1
            """)
    MaterialBatch getByBatchNoAndMaterial(@Param("batchNo") String batchNo, @Param("materialId") Long materialId);

    @Insert("""
            INSERT INTO material_batch (batch_no, material_id, source_type, source_no, quantity, batch_status,
                                        produced_at, received_at, created_at, updated_at)
            VALUES (#{batchNo}, #{materialId}, #{sourceType}, #{sourceNo}, #{quantity}, #{batchStatus},
                    #{producedAt}, #{receivedAt}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "batchId")
    void insertBatch(MaterialBatch batch);

    @Update("""
            UPDATE material_batch
            SET source_type=#{sourceType}, source_no=#{sourceNo}, quantity=#{quantity}, batch_status=#{batchStatus},
                produced_at=#{producedAt}, received_at=#{receivedAt}, updated_at=#{updatedAt}
            WHERE batch_id=#{batchId}
            """)
    void updateBatch(MaterialBatch batch);
}
