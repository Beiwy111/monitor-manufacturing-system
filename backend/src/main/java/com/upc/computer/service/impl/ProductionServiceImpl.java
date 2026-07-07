package com.upc.computer.service.impl;

import com.upc.computer.service.ProductionService;
import com.upc.computer.entity.ProductionPlan;
import com.upc.computer.mapper.ProductionPlanMapper;
import com.upc.computer.entity.ProductionPlanItem;
import com.upc.computer.mapper.ProductionPlanItemMapper;
import com.upc.computer.entity.ProcessRoute;
import com.upc.computer.mapper.ProcessRouteMapper;
import com.upc.computer.entity.ProcessStep;
import com.upc.computer.mapper.ProcessStepMapper;
import com.upc.computer.entity.WorkOrder;
import com.upc.computer.mapper.WorkOrderMapper;
import com.upc.computer.entity.DispatchTask;
import com.upc.computer.mapper.DispatchTaskMapper;
import com.upc.computer.entity.WorkReport;
import com.upc.computer.mapper.WorkReportMapper;
import com.upc.computer.entity.WorkProgress;
import com.upc.computer.mapper.WorkProgressMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;

@Service
public class ProductionServiceImpl implements ProductionService {

    @Autowired
    private ProductionPlanMapper productionPlanMapper;

    @Autowired
    private ProductionPlanItemMapper productionPlanItemMapper;

    @Autowired
    private ProcessRouteMapper processRouteMapper;

    @Autowired
    private ProcessStepMapper processStepMapper;

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Autowired
    private DispatchTaskMapper dispatchTaskMapper;

    @Autowired
    private WorkReportMapper workReportMapper;

    @Autowired
    private WorkProgressMapper workProgressMapper;

    // 查询所有生产计划
    @Override
    public ArrayList<ProductionPlan> planList() {
        return productionPlanMapper.planList();
    }

    // 根据主键查询生产计划
    @Override
    public ProductionPlan getPlanById(Long planId) {
        return productionPlanMapper.getPlanById(planId);
    }

    // 新增生产计划
    @Override
    public void insertPlan(ProductionPlan plan) {
        productionPlanMapper.insertPlan(plan);
    }

    // 修改生产计划
    @Override
    public void updatePlan(ProductionPlan plan) {
        productionPlanMapper.updatePlan(plan);
    }

    // 删除生产计划
    @Override
    public void deletePlan(Long planId) {
        productionPlanMapper.deletePlan(planId);
    }

    // 查询所有计划明细
    @Override
    public ArrayList<ProductionPlanItem> planItemList() {
        return productionPlanItemMapper.planItemList();
    }

    // 根据主键查询计划明细
    @Override
    public ProductionPlanItem getPlanItemById(Long planItemId) {
        return productionPlanItemMapper.getPlanItemById(planItemId);
    }

    // 新增计划明细
    @Override
    public void insertPlanItem(ProductionPlanItem planItem) {
        productionPlanItemMapper.insertPlanItem(planItem);
    }

    // 修改计划明细
    @Override
    public void updatePlanItem(ProductionPlanItem planItem) {
        productionPlanItemMapper.updatePlanItem(planItem);
    }

    // 删除计划明细
    @Override
    public void deletePlanItem(Long planItemId) {
        productionPlanItemMapper.deletePlanItem(planItemId);
    }

    // 查询所有工艺路线
    @Override
    public ArrayList<ProcessRoute> routeList() {
        return processRouteMapper.routeList();
    }

    // 根据主键查询工艺路线
    @Override
    public ProcessRoute getRouteById(Long routeId) {
        return processRouteMapper.getRouteById(routeId);
    }

    // 新增工艺路线
    @Override
    public void insertRoute(ProcessRoute route) {
        processRouteMapper.insertRoute(route);
    }

    // 修改工艺路线
    @Override
    public void updateRoute(ProcessRoute route) {
        processRouteMapper.updateRoute(route);
    }

    // 删除工艺路线
    @Override
    public void deleteRoute(Long routeId) {
        processRouteMapper.deleteRoute(routeId);
    }

    // 查询所有工序
    @Override
    public ArrayList<ProcessStep> stepList() {
        return processStepMapper.stepList();
    }

    // 根据主键查询工序
    @Override
    public ProcessStep getStepById(Long stepId) {
        return processStepMapper.getStepById(stepId);
    }

    // 新增工序
    @Override
    public void insertStep(ProcessStep step) {
        processStepMapper.insertStep(step);
    }

    // 修改工序
    @Override
    public void updateStep(ProcessStep step) {
        processStepMapper.updateStep(step);
    }

    // 删除工序
    @Override
    public void deleteStep(Long stepId) {
        processStepMapper.deleteStep(stepId);
    }

    // 查询所有工单
    @Override
    public ArrayList<WorkOrder> workOrderList() {
        return workOrderMapper.workOrderList();
    }

    // 根据主键查询工单
    @Override
    public WorkOrder getWorkOrderById(Long workOrderId) {
        return workOrderMapper.getWorkOrderById(workOrderId);
    }

    // 新增工单
    @Override
    public void insertWorkOrder(WorkOrder workOrder) {
        workOrderMapper.insertWorkOrder(workOrder);
    }

    // 修改工单
    @Override
    public void updateWorkOrder(WorkOrder workOrder) {
        workOrderMapper.updateWorkOrder(workOrder);
    }

    // 删除工单
    @Override
    public void deleteWorkOrder(Long workOrderId) {
        workOrderMapper.deleteWorkOrder(workOrderId);
    }

    // 查询所有派工任务
    @Override
    public ArrayList<DispatchTask> dispatchList() {
        return dispatchTaskMapper.dispatchList();
    }

    // 根据主键查询派工任务
    @Override
    public DispatchTask getDispatchById(Long dispatchId) {
        return dispatchTaskMapper.getDispatchById(dispatchId);
    }

    // 新增派工任务
    @Override
    public void insertDispatch(DispatchTask dispatch) {
        dispatchTaskMapper.insertDispatch(dispatch);
    }

    // 修改派工任务
    @Override
    public void updateDispatch(DispatchTask dispatch) {
        dispatchTaskMapper.updateDispatch(dispatch);
    }

    // 删除派工任务
    @Override
    public void deleteDispatch(Long dispatchId) {
        dispatchTaskMapper.deleteDispatch(dispatchId);
    }

    // 查询所有报工
    @Override
    public ArrayList<WorkReport> reportList() {
        return workReportMapper.reportList();
    }

    // 根据主键查询报工
    @Override
    public WorkReport getReportById(Long reportId) {
        return workReportMapper.getReportById(reportId);
    }

    // 新增报工
    @Override
    public void insertReport(WorkReport report) {
        workReportMapper.insertReport(report);
    }

    // 修改报工
    @Override
    public void updateReport(WorkReport report) {
        workReportMapper.updateReport(report);
    }

    // 删除报工
    @Override
    public void deleteReport(Long reportId) {
        workReportMapper.deleteReport(reportId);
    }

    // 查询所有生产进度
    @Override
    public ArrayList<WorkProgress> progressList() {
        return workProgressMapper.progressList();
    }

    // 根据主键查询生产进度
    @Override
    public WorkProgress getProgressById(Long progressId) {
        return workProgressMapper.getProgressById(progressId);
    }

    // 新增生产进度
    @Override
    public void insertProgress(WorkProgress progress) {
        workProgressMapper.insertProgress(progress);
    }

    // 修改生产进度
    @Override
    public void updateProgress(WorkProgress progress) {
        workProgressMapper.updateProgress(progress);
    }

    // 删除生产进度
    @Override
    public void deleteProgress(Long progressId) {
        workProgressMapper.deleteProgress(progressId);
    }

}
