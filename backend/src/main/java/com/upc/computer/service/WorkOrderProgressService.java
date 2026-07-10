package com.upc.computer.service;

import com.upc.computer.entity.DispatchTask;
import com.upc.computer.entity.ProcessStep;
import com.upc.computer.entity.WorkOrder;
import com.upc.computer.mapper.DispatchTaskMapper;
import com.upc.computer.mapper.ProcessStepMapper;
import com.upc.computer.mapper.WorkOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工单进度：成品数量按各生产工序完成量的最小值计算，禁止工序报工简单累加。
 */
@Service
public class WorkOrderProgressService {

    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private DispatchTaskMapper dispatchTaskMapper;
    @Autowired
    private ProcessStepMapper processStepMapper;

    public void syncWorkOrderProgress(Long workOrderId) {
        if (workOrderId == null) {
            return;
        }
        WorkOrder wo = workOrderMapper.getWorkOrderById(workOrderId);
        if (wo == null) {
            return;
        }
        Map<Long, ProcessStep> stepById = processStepMapper.stepList().stream()
                .collect(Collectors.toMap(ProcessStep::getStepId, s -> s, (a, b) -> a));
        List<DispatchTask> dispatches = dispatchTaskMapper.dispatchList().stream()
                .filter(d -> workOrderId.equals(d.getWorkOrderId()))
                .toList();

        int finished = ProductionWorkshopCatalog.finishedGoodsQty(dispatches, stepById);
        wo.setCompletedQuantity(BigDecimal.valueOf(finished));
        if (finished > 0 && !"COMPLETED".equals(wo.getStatus()) && !"QC_PENDING".equals(wo.getStatus())) {
            wo.setStatus("PRODUCING");
        }
        wo.setUpdatedAt(LocalDateTime.now());
        workOrderMapper.updateWorkOrder(wo);
    }

    public void syncAllWorkOrders() {
        for (WorkOrder wo : workOrderMapper.workOrderList()) {
            syncWorkOrderProgress(wo.getWorkOrderId());
        }
    }
}
