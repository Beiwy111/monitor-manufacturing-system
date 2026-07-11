package com.upc.computer.service;

import com.upc.computer.entity.PurchaseOrder;
import com.upc.computer.entity.PurchaseOrderItem;
import com.upc.computer.entity.PurchaseRequirement;
import com.upc.computer.dto.GeneratePurchaseRequest;
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
    List<PurchaseRequirement> workbenchList(String materialName, String status, Integer priority);
    PurchaseRequirementDetailVO workbenchDetail(Long requirementId);
    List<PurchaseByOrderVO> workbenchByOrder();

    /** 按供应商自动分组，每组生成一张采购单，返回生成的采购单列表 */
    List<PurchaseOrder> generatePurchaseOrder(GeneratePurchaseRequest request);

    void selectRequirement(Long requirementId);
    void cancelRequirement(Long requirementId);
    void confirmArrival(Long purchaseOrderId);

    void revokePurchaseOrder(Long purchaseOrderId);

    List<Map<String, String>> getSupplierList();
}
