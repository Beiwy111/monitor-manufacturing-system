package com.upc.computer.service;

import com.upc.computer.entity.QualityInspection;
import com.upc.computer.entity.NonconformingProduct;
import java.util.ArrayList;

public interface QualityService {

    public ArrayList<QualityInspection> inspectionList();

    public QualityInspection getInspectionById(Long inspectionId);

    public void insertInspection(QualityInspection inspection);

    public void updateInspection(QualityInspection inspection);

    public void deleteInspection(Long inspectionId);

    public ArrayList<NonconformingProduct> nonconformingList();

    public NonconformingProduct getNonconformingById(Long nonconformingId);

    public void insertNonconforming(NonconformingProduct nonconforming);

    public void updateNonconforming(NonconformingProduct nonconforming);

    public void deleteNonconforming(Long nonconformingId);

}
