package com.upc.computer.service;

import com.upc.computer.entity.PurchaseOrder;
import com.upc.computer.entity.PurchaseOrderItem;
import com.upc.computer.entity.PurchaseRequirement;
import com.upc.computer.dto.GeneratePurchaseRequest;
import com.upc.computer.dto.UpdatePurchaseOrderDraftRequest;
import com.upc.computer.vo.PurchaseRequirementDetailVO;
import com.upc.computer.vo.PurchaseByOrderVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface PurchaseService {

    ArrayList<PurchaseOrder> purchaseOrderList();
    PurchaseOrder getPurchaseOrderById(Long purchaseOrderId);
    void insertPurchaseOrder(PurchaseOrder purchaseOrder);
    void updatePurchaseOrder(PurchaseOrder purchaseOrder);
    void deletePurchaseOrder(Long purchaseOrderId);

    ArrayList<PurchaseOrderItem> purchaseOrderItemList();
    PurchaseOrderItem getPurchaseOrderItemById(Long purchaseOrderItemId);
    void insertPurchaseOrderItem(PurchaseOrderItem purchaseOrderItem);
    void updatePurchaseOrderItem(PurchaseOrderItem purchaseOrderItem);
    void deletePurchaseOrderItem(Long purchaseOrderItemId);

    List<PurchaseRequirement> calculateRequirements();
    List<PurchaseRequirement> workbenchList(String materialName, String status, Integer priority, String scope);
    PurchaseRequirementDetailVO workbenchDetail(Long requirementId);
    List<PurchaseByOrderVO> workbenchByOrder();

    /** 按供应商自动分组，每组生成一张采购单，返回生成的采购单列表 */
    List<PurchaseOrder> generatePurchaseOrder(GeneratePurchaseRequest request);

    void selectRequirement(Long requirementId);
    void cancelRequirement(Long requirementId);
    void confirmArrival(Long purchaseOrderId);

    void confirmArrivalWithSlots(Long purchaseOrderId, List<Map<String, Object>> assignments);

    void revokePurchaseOrder(Long purchaseOrderId);

    /** 保存草稿采购单（修改数量、单价、供应商等，自动重算金额） */
    PurchaseOrder savePurchaseOrderDraft(UpdatePurchaseOrderDraftRequest request);

    List<Map<String, String>> getSupplierList();

    /** 订单需求总览：订单数量、成品库存、需生产量、缺料统计（活数据） */
    List<Map<String, Object>> listOrderDemandOverview();
}
