package com.upc.computer.task;

import com.upc.computer.entity.DispatchTask;
import com.upc.computer.entity.WorkReport;
import com.upc.computer.mapper.DispatchTaskMapper;
import com.upc.computer.mapper.WorkReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 修复大屏模拟任务对派工进度的干扰，恢复操作员手工报工流程。
 */
@Component
public class DispatchProgressRepairTask {

    @Autowired
    private DispatchTaskMapper dispatchTaskMapper;
    @Autowired
    private WorkReportMapper workReportMapper;

    @Transactional
    public void repairFromWorkReports() {
        List<WorkReport> reports = workReportMapper.reportList();
        for (DispatchTask dispatch : dispatchTaskMapper.dispatchList()) {
            BigDecimal assigned = dispatch.getAssignedQuantity() == null ? BigDecimal.ZERO : dispatch.getAssignedQuantity();
            BigDecimal reported = reports.stream()
                    .filter(r -> dispatch.getDispatchId().equals(r.getDispatchId()))
                    .map(r -> r.getCompletedQuantity() == null ? BigDecimal.ZERO : r.getCompletedQuantity())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dispatch.setCompletedQuantity(reported);

            String status = dispatch.getStatus();
            if (reported.compareTo(assigned) >= 0 && assigned.compareTo(BigDecimal.ZERO) > 0) {
                if (!"QC_PENDING".equals(status) && !"COMPLETED".equals(status)) {
                    dispatch.setStatus("PRODUCING");
                }
            } else if ("COMPLETED".equals(status) || "RUNNING".equals(status)) {
                dispatch.setStatus(reported.compareTo(BigDecimal.ZERO) > 0 ? "PRODUCING" : "ACCEPTED");
            } else if ("PRODUCING".equals(status) && reported.compareTo(BigDecimal.ZERO) == 0) {
                dispatch.setStatus("ACCEPTED");
            }
            dispatchTaskMapper.updateDispatch(dispatch);
        }
    }
}
