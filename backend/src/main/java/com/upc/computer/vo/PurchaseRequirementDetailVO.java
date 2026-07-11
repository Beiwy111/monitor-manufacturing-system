package com.upc.computer.vo;

import com.upc.computer.entity.PurchaseRequirement;
import com.upc.computer.entity.PurchaseRequirementSource;
import java.util.List;

/**
 * 采购需求详情（物料维度 + 来源追溯）
 */
public class PurchaseRequirementDetailVO {

    private PurchaseRequirement requirement;

    private List<PurchaseRequirementSource> sources;

    public PurchaseRequirement getRequirement() {
        return requirement;
    }

    public void setRequirement(PurchaseRequirement requirement) {
        this.requirement = requirement;
    }

    public List<PurchaseRequirementSource> getSources() {
        return sources;
    }

    public void setSources(List<PurchaseRequirementSource> sources) {
        this.sources = sources;
    }

}
