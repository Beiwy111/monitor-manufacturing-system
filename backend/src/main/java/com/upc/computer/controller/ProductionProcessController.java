package com.upc.computer.controller;

import com.upc.computer.common.Result;
import com.upc.computer.entity.ProcessRoute;
import com.upc.computer.entity.ProcessStep;
import com.upc.computer.service.ProductionProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 计划员工序设置与工艺流程维护接口。 */
@RestController
@RequestMapping("/production/process")
public class ProductionProcessController {
    @Autowired
    private ProductionProcessService productionProcessService;

    @GetMapping("/snapshot")
    public Result<Object> snapshot() {
        return Result.success(productionProcessService.snapshot());
    }

    @PostMapping("/routes")
    public Result<Object> saveRoute(@RequestBody ProcessRoute route,
                                    @RequestParam(required = false, defaultValue = "system") String operator) {
        return Result.success(productionProcessService.saveRoute(route, operator));
    }

    @DeleteMapping("/routes/{routeId}")
    public Result<Object> disableRoute(@PathVariable Long routeId) {
        productionProcessService.disableRoute(routeId);
        return Result.success(true);
    }

    @PostMapping("/steps")
    public Result<Object> saveStep(@RequestBody ProcessStep step) {
        return Result.success(productionProcessService.saveStep(step));
    }

    @DeleteMapping("/steps/{stepId}")
    public Result<Object> disableStep(@PathVariable Long stepId) {
        productionProcessService.disableStep(stepId);
        return Result.success(true);
    }

    @PostMapping("/steps/reorder")
    public Result<Object> reorderSteps(@RequestBody Map<String, Object> body) {
        Long routeId = body.get("routeId") instanceof Number n ? n.longValue() : null;
        @SuppressWarnings("unchecked")
        List<Long> stepIds = body.get("stepIds") instanceof List<?> list
                ? list.stream().map(v -> v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v))).toList()
                : List.of();
        productionProcessService.reorderSteps(routeId, stepIds);
        return Result.success(true);
    }
}
