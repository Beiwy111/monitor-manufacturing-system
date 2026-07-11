package com.upc.computer.controller;

import com.upc.computer.mapper.PurchaseOrderItemMapper;
import com.upc.computer.service.PurchaseService;
import com.upc.computer.service.AiDocumentService;
import com.upc.computer.common.Result;
import com.upc.computer.dto.AiDocumentConfirmRequest;
import com.upc.computer.dto.AiDocumentConfirmResponse;
import com.upc.computer.dto.AiDocumentParseResult;
import com.upc.computer.dto.GeneratePurchaseRequest;
import com.upc.computer.entity.PurchaseOrder;
import com.upc.computer.entity.PurchaseOrderItem;
import com.upc.computer.entity.PurchaseRequirement;
import com.upc.computer.vo.PurchaseByOrderVO;
import com.upc.computer.vo.PurchaseRequirementDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/purchase")
public class PurchaseController {

    @Autowired
    private PurchaseService purchaseService;

    @Autowired
    private PurchaseOrderItemMapper purchaseOrderItemMapper;

    @Autowired
    private AiDocumentService aiDocumentService;

    // 查询采购订单列表
    @RequestMapping("/purchaseOrder/list")
    public ArrayList<PurchaseOrder> purchaseOrderList() {
        return purchaseService.purchaseOrderList();
    }

    // 根据主键查询采购订单
    @RequestMapping("/purchaseOrder/get")
    public PurchaseOrder getPurchaseOrderById(Long purchaseOrderId) {
        return purchaseService.getPurchaseOrderById(purchaseOrderId);
    }

    // 新增采购订单
    @RequestMapping("/purchaseOrder/insert")
    public void insertPurchaseOrder(PurchaseOrder purchaseOrder) {
        purchaseService.insertPurchaseOrder(purchaseOrder);
    }

    // 修改采购订单
    @RequestMapping("/purchaseOrder/update")
    public void updatePurchaseOrder(PurchaseOrder purchaseOrder) {
        purchaseService.updatePurchaseOrder(purchaseOrder);
    }

    // 删除采购订单
    @RequestMapping("/purchaseOrder/delete")
    public void deletePurchaseOrder(Long purchaseOrderId) {
        purchaseService.deletePurchaseOrder(purchaseOrderId);
    }

    // 查询采购明细列表
    @RequestMapping("/purchaseOrderItem/list")
    public ArrayList<PurchaseOrderItem> purchaseOrderItemList() {
        return purchaseService.purchaseOrderItemList();
    }

    // 按采购单ID查询明细（含物料名称，JOIN material表）
    @GetMapping("/purchaseOrderItem/listByOrder")
    public Result<List<PurchaseOrderItem>> listItemsByOrder(@RequestParam Long purchaseOrderId) {
        return Result.success(purchaseOrderItemMapper.listByOrderIdWithMaterial(purchaseOrderId));
    }

    // 根据主键查询采购明细
    @RequestMapping("/purchaseOrderItem/get")
    public PurchaseOrderItem getPurchaseOrderItemById(Long purchaseOrderItemId) {
        return purchaseService.getPurchaseOrderItemById(purchaseOrderItemId);
    }

    // 新增采购明细
    @RequestMapping("/purchaseOrderItem/insert")
    public void insertPurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) {
        purchaseService.insertPurchaseOrderItem(purchaseOrderItem);
    }

    // 修改采购明细
    @RequestMapping("/purchaseOrderItem/update")
    public void updatePurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) {
        purchaseService.updatePurchaseOrderItem(purchaseOrderItem);
    }

    // 删除采购明细
    @RequestMapping("/purchaseOrderItem/delete")
    public void deletePurchaseOrderItem(Long purchaseOrderItemId) {
        purchaseService.deletePurchaseOrderItem(purchaseOrderItemId);
    }

    // ============ 采购工作台：缺料需求 ============

    // 重新计算缺料需求并写入工作台
    @PostMapping("/workbench/calculate")
    public Result<List<PurchaseRequirement>> calculateRequirements() {
        return Result.success(purchaseService.calculateRequirements());
    }

    // 物料维度：待采购清单
    @GetMapping("/workbench/list")
    public Result<List<PurchaseRequirement>> workbenchList(
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer priority) {
        return Result.success(purchaseService.workbenchList(materialName, status, priority));
    }

    // 物料维度：查看某物料需求的来源订单明细
    @GetMapping("/workbench/detail")
    public Result<PurchaseRequirementDetailVO> workbenchDetail(@RequestParam Long requirementId) {
        return Result.success(purchaseService.workbenchDetail(requirementId));
    }

    // 订单维度：按销售订单/工单分组展开缺料零部件
    @GetMapping("/workbench/byOrder")
    public Result<List<PurchaseByOrderVO>> workbenchByOrder() {
        return Result.success(purchaseService.workbenchByOrder());
    }

    // 一键生成采购单（按供应商自动拆单，返回生成的采购单列表）
    @PostMapping("/workbench/generate")
    public Result<List<PurchaseOrder>> generatePurchaseOrder(@RequestBody GeneratePurchaseRequest request) {
        return Result.success(purchaseService.generatePurchaseOrder(request));
    }

    // 标记采购需求已选中
    @PostMapping("/workbench/select")
    public Result<Void> selectRequirement(@RequestParam Long requirementId) {
        purchaseService.selectRequirement(requirementId);
        return Result.success();
    }

    // 取消某条待采购需求
    @PostMapping("/workbench/cancel")
    public Result<Void> cancelRequirement(@RequestParam Long requirementId) {
        purchaseService.cancelRequirement(requirementId);
        return Result.success();
    }

    // ============ 到货确认 ============

    // 采购到货确认 + 入库预留联动
    @PostMapping("/order/confirmArrival")
    public Result<Void> confirmArrival(@RequestParam Long purchaseOrderId) {
        purchaseService.confirmArrival(purchaseOrderId);
        return Result.success();
    }

    // 撤销采购单，关联需求恢复为待采购
    @PostMapping("/order/revoke")
    public Result<Void> revokePurchaseOrder(@RequestParam Long purchaseOrderId) {
        purchaseService.revokePurchaseOrder(purchaseOrderId);
        return Result.success();
    }

    // ============ AI 采购单据解析 ============

    // AI 解析上传的采购单据图片
    @PostMapping("/ai/document/parse")
    public Result<AiDocumentParseResult> parseAiDocument(@RequestParam("file") MultipartFile file) {
        return Result.success(aiDocumentService.parseDocument(file));
    }

    // 确认 AI 解析结果，录入采购单草稿
    @PostMapping("/ai/document/confirm")
    public Result<AiDocumentConfirmResponse> confirmAiDocument(@RequestBody AiDocumentConfirmRequest request) {
        return Result.success(aiDocumentService.confirmDocument(request));
    }

}
