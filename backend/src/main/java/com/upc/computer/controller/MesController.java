package com.upc.computer.controller;

import com.upc.computer.common.Result;
import com.upc.computer.dto.MesActionRequest;
import com.upc.computer.service.MesSnapshotService;
import com.upc.computer.service.MesWorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * MES 实时数据接口
 */
@RestController
@RequestMapping("/mes")
public class MesController {

    @Autowired
    private MesSnapshotService mesSnapshotService;

    @Autowired
    private MesWorkflowService mesWorkflowService;

    @GetMapping("/snapshot")
    public Result<Map<String, Object>> snapshot() {
        return Result.success(mesSnapshotService.buildSnapshot());
    }

    @PostMapping("/action")
    public Result<Object> action(@RequestBody MesActionRequest req) {
        return Result.success(mesWorkflowService.execute(req));
    }
}
