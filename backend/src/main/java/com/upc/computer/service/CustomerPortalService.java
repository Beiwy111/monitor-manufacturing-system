package com.upc.computer.service;

import com.upc.computer.dto.CustomerCreateOrderRequest;
import com.upc.computer.dto.CustomerFeedbackRequest;
import com.upc.computer.dto.CustomerProfileUpdateRequest;
import com.upc.computer.dto.LoginResponse;

import java.util.List;
import java.util.Map;

public interface CustomerPortalService {

    void requireCustomerRole(LoginResponse session);

    String resolveCustomerName(LoginResponse session);

    Map<String, Object> dashboard(LoginResponse session);

    List<Map<String, Object>> listOrders(LoginResponse session);

    Map<String, Object> getOrderDetail(LoginResponse session, Long orderId);

    Map<String, Object> createOrder(LoginResponse session, CustomerCreateOrderRequest request);

    List<Map<String, Object>> listProducts();

    Map<String, Object> getProductDetail(Long materialId);

    List<Map<String, Object>> listFeedbacks(LoginResponse session);

    Map<String, Object> submitFeedback(LoginResponse session, CustomerFeedbackRequest request);

    Map<String, Object> getProfile(LoginResponse session);

    Map<String, Object> updateProfile(LoginResponse session, CustomerProfileUpdateRequest request);

    String saveUpload(LoginResponse session, org.springframework.web.multipart.MultipartFile file);
}
