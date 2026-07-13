package com.upc.computer.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备实体，对应表 equipment
 */
public class Equipment {

    /** equipment_id */
    private Long equipmentId;

    /** equipment_code */
    private String equipmentCode;

    /** equipment_name */
    private String equipmentName;

    /** equipment_type */
    private String equipmentType;

    /** workshop */
    private String workshop;

    /** workstation */
    private String workstation;

    /** manufacturer */
    private String manufacturer;

    /** model */
    private String model;

    /** purchase_date */
    private LocalDate purchaseDate;

    /** status */
    private String status;

    /** last_maintenance_at */
    private LocalDateTime lastMaintenanceAt;

    /** created_at */
    private LocalDateTime createdAt;

    /** updated_at */
    private LocalDateTime updatedAt;

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getEquipmentCode() {
        return equipmentCode;
    }

    public void setEquipmentCode(String equipmentCode) {
        this.equipmentCode = equipmentCode;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public String getEquipmentType() {
        return equipmentType;
    }

    public void setEquipmentType(String equipmentType) {
        this.equipmentType = equipmentType;
    }

    public String getWorkshop() {
        return workshop;
    }

    public void setWorkshop(String workshop) {
        this.workshop = workshop;
    }

    public String getWorkstation() {
        return workstation;
    }

    public void setWorkstation(String workstation) {
        this.workstation = workstation;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastMaintenanceAt() {
        return lastMaintenanceAt;
    }

    public void setLastMaintenanceAt(LocalDateTime lastMaintenanceAt) {
        this.lastMaintenanceAt = lastMaintenanceAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
