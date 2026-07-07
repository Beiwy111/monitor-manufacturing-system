package com.upc.computer.service;

import com.upc.computer.entity.AfterSalesCase;
import com.upc.computer.entity.CostSettlement;
import java.util.ArrayList;

public interface AfterSalesService {

    public ArrayList<AfterSalesCase> afterSalesCaseList();

    public AfterSalesCase getAfterSalesCaseById(String caseNo);

    public void insertAfterSalesCase(AfterSalesCase afterSalesCase);

    public void updateAfterSalesCase(AfterSalesCase afterSalesCase);

    public void deleteAfterSalesCase(String caseNo);

    public ArrayList<CostSettlement> settlementList();

    public CostSettlement getSettlementById(Long settlementId);

    public void insertSettlement(CostSettlement settlement);

    public void updateSettlement(CostSettlement settlement);

    public void deleteSettlement(Long settlementId);

}
