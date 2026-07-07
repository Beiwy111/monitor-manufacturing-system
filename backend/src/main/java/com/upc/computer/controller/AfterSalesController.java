package com.upc.computer.controller;

import com.upc.computer.service.AfterSalesService;
import com.upc.computer.entity.AfterSalesCase;
import com.upc.computer.entity.CostSettlement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;

@RestController
@RequestMapping("/afterSales")
public class AfterSalesController {

    @Autowired
    private AfterSalesService afterSalesService;

    // 查询售后案例列表
    @RequestMapping("/afterSalesCase/list")
    public ArrayList<AfterSalesCase> afterSalesCaseList() {
        return afterSalesService.afterSalesCaseList();
    }

    // 根据主键查询售后案例
    @RequestMapping("/afterSalesCase/get")
    public AfterSalesCase getAfterSalesCaseById(String caseNo) {
        return afterSalesService.getAfterSalesCaseById(caseNo);
    }

    // 新增售后案例
    @RequestMapping("/afterSalesCase/insert")
    public void insertAfterSalesCase(AfterSalesCase afterSalesCase) {
        afterSalesService.insertAfterSalesCase(afterSalesCase);
    }

    // 修改售后案例
    @RequestMapping("/afterSalesCase/update")
    public void updateAfterSalesCase(AfterSalesCase afterSalesCase) {
        afterSalesService.updateAfterSalesCase(afterSalesCase);
    }

    // 删除售后案例
    @RequestMapping("/afterSalesCase/delete")
    public void deleteAfterSalesCase(String caseNo) {
        afterSalesService.deleteAfterSalesCase(caseNo);
    }

    // 查询成本结算列表
    @RequestMapping("/settlement/list")
    public ArrayList<CostSettlement> settlementList() {
        return afterSalesService.settlementList();
    }

    // 根据主键查询成本结算
    @RequestMapping("/settlement/get")
    public CostSettlement getSettlementById(Long settlementId) {
        return afterSalesService.getSettlementById(settlementId);
    }

    // 新增成本结算
    @RequestMapping("/settlement/insert")
    public void insertSettlement(CostSettlement settlement) {
        afterSalesService.insertSettlement(settlement);
    }

    // 修改成本结算
    @RequestMapping("/settlement/update")
    public void updateSettlement(CostSettlement settlement) {
        afterSalesService.updateSettlement(settlement);
    }

    // 删除成本结算
    @RequestMapping("/settlement/delete")
    public void deleteSettlement(Long settlementId) {
        afterSalesService.deleteSettlement(settlementId);
    }

}
