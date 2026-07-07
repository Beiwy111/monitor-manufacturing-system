package com.upc.computer.service.impl;

import com.upc.computer.service.AfterSalesService;
import com.upc.computer.entity.AfterSalesCase;
import com.upc.computer.mapper.AfterSalesCaseMapper;
import com.upc.computer.entity.CostSettlement;
import com.upc.computer.mapper.CostSettlementMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;

@Service
public class AfterSalesServiceImpl implements AfterSalesService {

    @Autowired
    private AfterSalesCaseMapper afterSalesCaseMapper;

    @Autowired
    private CostSettlementMapper costSettlementMapper;

    // 查询所有售后案例
    @Override
    public ArrayList<AfterSalesCase> afterSalesCaseList() {
        return afterSalesCaseMapper.afterSalesCaseList();
    }

    // 根据主键查询售后案例
    @Override
    public AfterSalesCase getAfterSalesCaseById(String caseNo) {
        return afterSalesCaseMapper.getAfterSalesCaseById(caseNo);
    }

    // 新增售后案例
    @Override
    public void insertAfterSalesCase(AfterSalesCase afterSalesCase) {
        afterSalesCaseMapper.insertAfterSalesCase(afterSalesCase);
    }

    // 修改售后案例
    @Override
    public void updateAfterSalesCase(AfterSalesCase afterSalesCase) {
        afterSalesCaseMapper.updateAfterSalesCase(afterSalesCase);
    }

    // 删除售后案例
    @Override
    public void deleteAfterSalesCase(String caseNo) {
        afterSalesCaseMapper.deleteAfterSalesCase(caseNo);
    }

    // 查询所有成本结算
    @Override
    public ArrayList<CostSettlement> settlementList() {
        return costSettlementMapper.settlementList();
    }

    // 根据主键查询成本结算
    @Override
    public CostSettlement getSettlementById(Long settlementId) {
        return costSettlementMapper.getSettlementById(settlementId);
    }

    // 新增成本结算
    @Override
    public void insertSettlement(CostSettlement settlement) {
        costSettlementMapper.insertSettlement(settlement);
    }

    // 修改成本结算
    @Override
    public void updateSettlement(CostSettlement settlement) {
        costSettlementMapper.updateSettlement(settlement);
    }

    // 删除成本结算
    @Override
    public void deleteSettlement(Long settlementId) {
        costSettlementMapper.deleteSettlement(settlementId);
    }

}
