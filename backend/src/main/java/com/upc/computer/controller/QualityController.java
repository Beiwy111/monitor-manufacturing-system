package com.upc.computer.controller;

import com.upc.computer.service.QualityService;
import com.upc.computer.entity.QualityInspection;
import com.upc.computer.entity.NonconformingProduct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;

@RestController
@RequestMapping("/quality")
public class QualityController {

    @Autowired
    private QualityService qualityService;

    // 查询质量检验列表
    @RequestMapping("/inspection/list")
    public ArrayList<QualityInspection> inspectionList() {
        return qualityService.inspectionList();
    }

    // 根据主键查询质量检验
    @RequestMapping("/inspection/get")
    public QualityInspection getInspectionById(Long inspectionId) {
        return qualityService.getInspectionById(inspectionId);
    }

    // 新增质量检验
    @RequestMapping("/inspection/insert")
    public void insertInspection(QualityInspection inspection) {
        qualityService.insertInspection(inspection);
    }

    // 修改质量检验
    @RequestMapping("/inspection/update")
    public void updateInspection(QualityInspection inspection) {
        qualityService.updateInspection(inspection);
    }

    // 删除质量检验
    @RequestMapping("/inspection/delete")
    public void deleteInspection(Long inspectionId) {
        qualityService.deleteInspection(inspectionId);
    }

    // 查询不合格品列表
    @RequestMapping("/nonconforming/list")
    public ArrayList<NonconformingProduct> nonconformingList() {
        return qualityService.nonconformingList();
    }

    // 根据主键查询不合格品
    @RequestMapping("/nonconforming/get")
    public NonconformingProduct getNonconformingById(Long nonconformingId) {
        return qualityService.getNonconformingById(nonconformingId);
    }

    // 新增不合格品
    @RequestMapping("/nonconforming/insert")
    public void insertNonconforming(NonconformingProduct nonconforming) {
        qualityService.insertNonconforming(nonconforming);
    }

    // 修改不合格品
    @RequestMapping("/nonconforming/update")
    public void updateNonconforming(NonconformingProduct nonconforming) {
        qualityService.updateNonconforming(nonconforming);
    }

    // 删除不合格品
    @RequestMapping("/nonconforming/delete")
    public void deleteNonconforming(Long nonconformingId) {
        qualityService.deleteNonconforming(nonconformingId);
    }

}
