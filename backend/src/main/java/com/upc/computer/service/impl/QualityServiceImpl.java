package com.upc.computer.service.impl;

import com.upc.computer.service.QualityService;
import com.upc.computer.entity.QualityInspection;
import com.upc.computer.mapper.QualityInspectionMapper;
import com.upc.computer.entity.NonconformingProduct;
import com.upc.computer.mapper.NonconformingProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;

@Service
public class QualityServiceImpl implements QualityService {

    @Autowired
    private QualityInspectionMapper qualityInspectionMapper;

    @Autowired
    private NonconformingProductMapper nonconformingProductMapper;

    // 查询所有质量检验
    @Override
    public ArrayList<QualityInspection> inspectionList() {
        return qualityInspectionMapper.inspectionList();
    }

    // 根据主键查询质量检验
    @Override
    public QualityInspection getInspectionById(Long inspectionId) {
        return qualityInspectionMapper.getInspectionById(inspectionId);
    }

    // 新增质量检验
    @Override
    public void insertInspection(QualityInspection inspection) {
        qualityInspectionMapper.insertInspection(inspection);
    }

    // 修改质量检验
    @Override
    public void updateInspection(QualityInspection inspection) {
        qualityInspectionMapper.updateInspection(inspection);
    }

    // 删除质量检验
    @Override
    public void deleteInspection(Long inspectionId) {
        qualityInspectionMapper.deleteInspection(inspectionId);
    }

    // 查询所有不合格品
    @Override
    public ArrayList<NonconformingProduct> nonconformingList() {
        return nonconformingProductMapper.nonconformingList();
    }

    // 根据主键查询不合格品
    @Override
    public NonconformingProduct getNonconformingById(Long nonconformingId) {
        return nonconformingProductMapper.getNonconformingById(nonconformingId);
    }

    // 新增不合格品
    @Override
    public void insertNonconforming(NonconformingProduct nonconforming) {
        nonconformingProductMapper.insertNonconforming(nonconforming);
    }

    // 修改不合格品
    @Override
    public void updateNonconforming(NonconformingProduct nonconforming) {
        nonconformingProductMapper.updateNonconforming(nonconforming);
    }

    // 删除不合格品
    @Override
    public void deleteNonconforming(Long nonconformingId) {
        nonconformingProductMapper.deleteNonconforming(nonconformingId);
    }

}
