package com.upc.computer.controller;

import com.upc.computer.service.ProductionService;
import com.upc.computer.entity.ProductionPlan;
import com.upc.computer.entity.ProductionPlanItem;
import com.upc.computer.entity.ProcessRoute;
import com.upc.computer.entity.ProcessStep;
import com.upc.computer.entity.WorkOrder;
import com.upc.computer.entity.DispatchTask;
import com.upc.computer.entity.WorkReport;
import com.upc.computer.entity.WorkProgress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;

@RestController
@RequestMapping("/production")
public class ProductionController {

    @Autowired
    private ProductionService productionService;

    // 查询生产计划列表
    @RequestMapping("/plan/list")
    public ArrayList<ProductionPlan> planList() {
        return productionService.planList();
    }

    // 根据主键查询生产计划
    @RequestMapping("/plan/get")
    public ProductionPlan getPlanById(Long planId) {
        return productionService.getPlanById(planId);
    }

    // 新增生产计划
    @RequestMapping("/plan/insert")
    public void insertPlan(ProductionPlan plan) {
        productionService.insertPlan(plan);
    }

    // 修改生产计划
    @RequestMapping("/plan/update")
    public void updatePlan(ProductionPlan plan) {
        productionService.updatePlan(plan);
    }

    // 删除生产计划
    @RequestMapping("/plan/delete")
    public void deletePlan(Long planId) {
        productionService.deletePlan(planId);
    }

    // 查询计划明细列表
    @RequestMapping("/planItem/list")
    public ArrayList<ProductionPlanItem> planItemList() {
        return productionService.planItemList();
    }

    // 根据主键查询计划明细
    @RequestMapping("/planItem/get")
    public ProductionPlanItem getPlanItemById(Long planItemId) {
        return productionService.getPlanItemById(planItemId);
    }

    // 新增计划明细
    @RequestMapping("/planItem/insert")
    public void insertPlanItem(ProductionPlanItem planItem) {
        productionService.insertPlanItem(planItem);
    }

    // 修改计划明细
    @RequestMapping("/planItem/update")
    public void updatePlanItem(ProductionPlanItem planItem) {
        productionService.updatePlanItem(planItem);
    }

    // 删除计划明细
    @RequestMapping("/planItem/delete")
    public void deletePlanItem(Long planItemId) {
        productionService.deletePlanItem(planItemId);
    }

    // 查询工艺路线列表
    @RequestMapping("/route/list")
    public ArrayList<ProcessRoute> routeList() {
        return productionService.routeList();
    }

    // 根据主键查询工艺路线
    @RequestMapping("/route/get")
    public ProcessRoute getRouteById(Long routeId) {
        return productionService.getRouteById(routeId);
    }

    // 新增工艺路线
    @RequestMapping("/route/insert")
    public void insertRoute(ProcessRoute route) {
        productionService.insertRoute(route);
    }

    // 修改工艺路线
    @RequestMapping("/route/update")
    public void updateRoute(ProcessRoute route) {
        productionService.updateRoute(route);
    }

    // 删除工艺路线
    @RequestMapping("/route/delete")
    public void deleteRoute(Long routeId) {
        productionService.deleteRoute(routeId);
    }

    // 查询工序列表
    @RequestMapping("/step/list")
    public ArrayList<ProcessStep> stepList() {
        return productionService.stepList();
    }

    // 根据主键查询工序
    @RequestMapping("/step/get")
    public ProcessStep getStepById(Long stepId) {
        return productionService.getStepById(stepId);
    }

    // 新增工序
    @RequestMapping("/step/insert")
    public void insertStep(ProcessStep step) {
        productionService.insertStep(step);
    }

    // 修改工序
    @RequestMapping("/step/update")
    public void updateStep(ProcessStep step) {
        productionService.updateStep(step);
    }

    // 删除工序
    @RequestMapping("/step/delete")
    public void deleteStep(Long stepId) {
        productionService.deleteStep(stepId);
    }

    // 查询工单列表
    @RequestMapping("/workOrder/list")
    public ArrayList<WorkOrder> workOrderList() {
        return productionService.workOrderList();
    }

    // 根据主键查询工单
    @RequestMapping("/workOrder/get")
    public WorkOrder getWorkOrderById(Long workOrderId) {
        return productionService.getWorkOrderById(workOrderId);
    }

    // 新增工单
    @RequestMapping("/workOrder/insert")
    public void insertWorkOrder(WorkOrder workOrder) {
        productionService.insertWorkOrder(workOrder);
    }

    // 修改工单
    @RequestMapping("/workOrder/update")
    public void updateWorkOrder(WorkOrder workOrder) {
        productionService.updateWorkOrder(workOrder);
    }

    // 删除工单
    @RequestMapping("/workOrder/delete")
    public void deleteWorkOrder(Long workOrderId) {
        productionService.deleteWorkOrder(workOrderId);
    }

    // 查询派工任务列表
    @RequestMapping("/dispatch/list")
    public ArrayList<DispatchTask> dispatchList() {
        return productionService.dispatchList();
    }

    // 根据主键查询派工任务
    @RequestMapping("/dispatch/get")
    public DispatchTask getDispatchById(Long dispatchId) {
        return productionService.getDispatchById(dispatchId);
    }

    // 新增派工任务
    @RequestMapping("/dispatch/insert")
    public void insertDispatch(DispatchTask dispatch) {
        productionService.insertDispatch(dispatch);
    }

    // 修改派工任务
    @RequestMapping("/dispatch/update")
    public void updateDispatch(DispatchTask dispatch) {
        productionService.updateDispatch(dispatch);
    }

    // 删除派工任务
    @RequestMapping("/dispatch/delete")
    public void deleteDispatch(Long dispatchId) {
        productionService.deleteDispatch(dispatchId);
    }

    // 查询报工列表
    @RequestMapping("/report/list")
    public ArrayList<WorkReport> reportList() {
        return productionService.reportList();
    }

    // 根据主键查询报工
    @RequestMapping("/report/get")
    public WorkReport getReportById(Long reportId) {
        return productionService.getReportById(reportId);
    }

    // 新增报工
    @RequestMapping("/report/insert")
    public void insertReport(WorkReport report) {
        productionService.insertReport(report);
    }

    // 修改报工
    @RequestMapping("/report/update")
    public void updateReport(WorkReport report) {
        productionService.updateReport(report);
    }

    // 删除报工
    @RequestMapping("/report/delete")
    public void deleteReport(Long reportId) {
        productionService.deleteReport(reportId);
    }

    // 查询生产进度列表
    @RequestMapping("/progress/list")
    public ArrayList<WorkProgress> progressList() {
        return productionService.progressList();
    }

    // 根据主键查询生产进度
    @RequestMapping("/progress/get")
    public WorkProgress getProgressById(Long progressId) {
        return productionService.getProgressById(progressId);
    }

    // 新增生产进度
    @RequestMapping("/progress/insert")
    public void insertProgress(WorkProgress progress) {
        productionService.insertProgress(progress);
    }

    // 修改生产进度
    @RequestMapping("/progress/update")
    public void updateProgress(WorkProgress progress) {
        productionService.updateProgress(progress);
    }

    // 删除生产进度
    @RequestMapping("/progress/delete")
    public void deleteProgress(Long progressId) {
        productionService.deleteProgress(progressId);
    }

}
