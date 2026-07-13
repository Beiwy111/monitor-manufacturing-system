package com.upc.computer.service;

import com.upc.computer.entity.ProductionPlan;
import com.upc.computer.entity.ProductionPlanItem;
import com.upc.computer.entity.ProcessRoute;
import com.upc.computer.entity.ProcessStep;
import com.upc.computer.entity.WorkOrder;
import com.upc.computer.entity.DispatchTask;
import com.upc.computer.entity.WorkReport;
import com.upc.computer.entity.WorkProgress;
import java.util.ArrayList;

public interface ProductionService {

    public ArrayList<ProductionPlan> planList();

    public ProductionPlan getPlanById(Long planId);

    public void insertPlan(ProductionPlan plan);

    public void updatePlan(ProductionPlan plan);

    public void deletePlan(Long planId);

    public ArrayList<ProductionPlanItem> planItemList();

    public ProductionPlanItem getPlanItemById(Long planItemId);

    public void insertPlanItem(ProductionPlanItem planItem);

    public void updatePlanItem(ProductionPlanItem planItem);

    public void deletePlanItem(Long planItemId);

    public ArrayList<ProcessRoute> routeList();

    public ProcessRoute getRouteById(Long routeId);

    public void insertRoute(ProcessRoute route);

    public void updateRoute(ProcessRoute route);

    public void deleteRoute(Long routeId);

    public ArrayList<ProcessStep> stepList();

    public ProcessStep getStepById(Long stepId);

    public void insertStep(ProcessStep step);

    public void updateStep(ProcessStep step);

    public void deleteStep(Long stepId);

    public ArrayList<WorkOrder> workOrderList();

    public WorkOrder getWorkOrderById(Long workOrderId);

    public void insertWorkOrder(WorkOrder workOrder);

    public void updateWorkOrder(WorkOrder workOrder);

    public void deleteWorkOrder(Long workOrderId);

    public ArrayList<DispatchTask> dispatchList();

    public DispatchTask getDispatchById(Long dispatchId);

    public void insertDispatch(DispatchTask dispatch);

    public void updateDispatch(DispatchTask dispatch);

    public void deleteDispatch(Long dispatchId);

    public ArrayList<WorkReport> reportList();

    public WorkReport getReportById(Long reportId);

    public void insertReport(WorkReport report);

    public void updateReport(WorkReport report);

    public void deleteReport(Long reportId);

    public ArrayList<WorkProgress> progressList();

    public WorkProgress getProgressById(Long progressId);

    public void insertProgress(WorkProgress progress);

    public void updateProgress(WorkProgress progress);

    public void deleteProgress(Long progressId);

}
