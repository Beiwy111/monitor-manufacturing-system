package com.upc.computer.service;

import com.upc.computer.common.BusinessException;
import com.upc.computer.entity.Material;
import com.upc.computer.entity.ProcessRoute;
import com.upc.computer.entity.ProcessStep;
import com.upc.computer.entity.User;
import com.upc.computer.mapper.MaterialMapper;
import com.upc.computer.mapper.ProcessRouteMapper;
import com.upc.computer.mapper.ProcessStepMapper;
import com.upc.computer.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 计划员维护工序设置与工艺路线。 */
@Service
public class ProductionProcessService {
    @Autowired
    private ProcessRouteMapper processRouteMapper;
    @Autowired
    private ProcessStepMapper processStepMapper;
    @Autowired
    private MaterialMapper materialMapper;
    @Autowired
    private UserMapper userMapper;

    public Map<String, Object> snapshot() {
        Map<Long, Material> materialById = materialMapper.materialList().stream()
                .collect(java.util.stream.Collectors.toMap(Material::getMaterialId, m -> m, (a, b) -> a));
        List<Map<String, Object>> routes = processRouteMapper.routeList().stream()
                .map(route -> routeRow(route, materialById.get(route.getMaterialId())))
                .toList();
        List<Map<String, Object>> steps = processStepMapper.stepList().stream()
                .sorted(java.util.Comparator
                        .comparing(ProcessStep::getRouteId, java.util.Comparator.nullsLast(Long::compareTo))
                        .thenComparing(ProcessStep::getStepNo, java.util.Comparator.nullsLast(Integer::compareTo)))
                .map(this::stepRow)
                .toList();
        List<Map<String, Object>> materials = materialMapper.materialList().stream()
                .filter(m -> "FINISHED".equals(m.getMaterialType()))
                .map(m -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("materialId", m.getMaterialId());
                    row.put("materialCode", m.getMaterialCode());
                    row.put("materialName", m.getMaterialName());
                    row.put("specification", m.getSpecification());
                    return row;
                })
                .toList();
        return Map.of("routes", routes, "steps", steps, "materials", materials);
    }

    @Transactional
    public ProcessRoute saveRoute(ProcessRoute route, String operator) {
        if (route == null || route.getMaterialId() == null || blank(route.getRouteCode()) || blank(route.getRouteName())) {
            throw new BusinessException("工艺路线的产品、编码和名称不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        route.setVersionNo(blank(route.getVersionNo()) ? "V1.0" : route.getVersionNo().trim());
        route.setStatus(route.getStatus() != null ? route.getStatus() : 1);
        route.setUpdatedAt(now);
        if (route.getRouteId() == null) {
            route.setCreatedBy(resolveUserId(operator));
            route.setCreatedAt(now);
            processRouteMapper.insertRoute(route);
        } else {
            ProcessRoute old = processRouteMapper.getRouteById(route.getRouteId());
            if (old == null) {
                throw new BusinessException("工艺路线不存在");
            }
            route.setCreatedBy(old.getCreatedBy());
            route.setCreatedAt(old.getCreatedAt());
            processRouteMapper.updateRoute(route);
        }
        return route;
    }

    @Transactional
    public ProcessStep saveStep(ProcessStep step) {
        if (step == null || step.getRouteId() == null || step.getStepNo() == null
                || blank(step.getStepCode()) || blank(step.getStepName())) {
            throw new BusinessException("工序的路线、序号、编码和名称不能为空");
        }
        if (processRouteMapper.getRouteById(step.getRouteId()) == null) {
            throw new BusinessException("工艺路线不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        step.setStandardWorkHours(step.getStandardWorkHours() != null ? step.getStandardWorkHours() : BigDecimal.ZERO);
        step.setQualityRequired(step.getQualityRequired() != null ? step.getQualityRequired() : 0);
        step.setStatus(step.getStatus() != null ? step.getStatus() : 1);
        step.setUpdatedAt(now);
        if (step.getStepId() == null) {
            step.setCreatedAt(now);
            processStepMapper.insertStep(step);
        } else {
            ProcessStep old = processStepMapper.getStepById(step.getStepId());
            if (old == null) {
                throw new BusinessException("工序不存在");
            }
            step.setCreatedAt(old.getCreatedAt());
            processStepMapper.updateStep(step);
        }
        return step;
    }

    @Transactional
    public void reorderSteps(Long routeId, List<Long> stepIds) {
        if (routeId == null || stepIds == null || stepIds.isEmpty()) {
            throw new BusinessException("工序排序参数无效");
        }
        LocalDateTime now = LocalDateTime.now();
        int no = 10;
        for (Long stepId : stepIds) {
            ProcessStep step = processStepMapper.getStepById(stepId);
            if (step == null || !routeId.equals(step.getRouteId())) {
                continue;
            }
            step.setStepNo(no);
            step.setUpdatedAt(now);
            processStepMapper.updateStep(step);
            no += 10;
        }
    }

    @Transactional
    public void disableRoute(Long routeId) {
        if (routeId == null) {
            throw new BusinessException("路线ID不能为空");
        }
        processStepMapper.disableByRouteId(routeId);
        processRouteMapper.disableRoute(routeId);
    }

    @Transactional
    public void disableStep(Long stepId) {
        if (stepId == null) {
            throw new BusinessException("工序ID不能为空");
        }
        processStepMapper.disableStep(stepId);
    }

    private Map<String, Object> routeRow(ProcessRoute route, Material material) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("routeId", route.getRouteId());
        row.put("materialId", route.getMaterialId());
        row.put("materialCode", material != null ? material.getMaterialCode() : "");
        row.put("materialName", material != null ? material.getMaterialName() : "");
        row.put("routeCode", route.getRouteCode());
        row.put("routeName", route.getRouteName());
        row.put("versionNo", route.getVersionNo());
        row.put("status", route.getStatus());
        row.put("statusText", Objects.equals(route.getStatus(), 1) ? "启用" : "停用");
        row.put("createdAt", route.getCreatedAt());
        row.put("updatedAt", route.getUpdatedAt());
        row.put("steps", processStepMapper.listByRouteId(route.getRouteId()).stream().map(this::stepRow).toList());
        return row;
    }

    private Map<String, Object> stepRow(ProcessStep step) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("stepId", step.getStepId());
        row.put("routeId", step.getRouteId());
        row.put("stepNo", step.getStepNo());
        row.put("stepCode", step.getStepCode());
        row.put("stepName", step.getStepName());
        row.put("standardWorkHours", step.getStandardWorkHours());
        row.put("standardEquipmentType", step.getStandardEquipmentType());
        row.put("qualityRequired", step.getQualityRequired());
        row.put("qualityRequiredText", Objects.equals(step.getQualityRequired(), 1) ? "是" : "否");
        row.put("status", step.getStatus());
        row.put("statusText", Objects.equals(step.getStatus(), 1) ? "启用" : "停用");
        row.put("createdAt", step.getCreatedAt());
        row.put("updatedAt", step.getUpdatedAt());
        return row;
    }

    private Long resolveUserId(String username) {
        if (blank(username)) {
            return null;
        }
        return userMapper.userList().stream()
                .filter(u -> username.equals(u.getUsername()) || username.equals(u.getRealName()))
                .map(User::getUserId)
                .findFirst().orElse(null);
    }

    private boolean blank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
