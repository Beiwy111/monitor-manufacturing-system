package com.upc.computer.service.impl;

import com.upc.computer.common.BusinessException;
import com.upc.computer.dto.CustomerCreateOrderRequest;
import com.upc.computer.dto.CustomerFeedbackRequest;
import com.upc.computer.dto.CustomerProfileUpdateRequest;
import com.upc.computer.dto.LoginResponse;
import com.upc.computer.entity.AfterSalesCase;
import com.upc.computer.entity.CustomerOrder;
import com.upc.computer.entity.CustomerOrderItem;
import com.upc.computer.entity.Material;
import com.upc.computer.entity.User;
import com.upc.computer.mapper.AfterSalesCaseMapper;
import com.upc.computer.mapper.CustomerOrderItemMapper;
import com.upc.computer.mapper.CustomerOrderMapper;
import com.upc.computer.mapper.CustomerPortalMapper;
import com.upc.computer.mapper.MaterialMapper;
import com.upc.computer.mapper.UserMapper;
import com.upc.computer.service.AuthService;
import com.upc.computer.service.CustomerPortalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerPortalServiceImpl implements CustomerPortalService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private CustomerPortalMapper customerPortalMapper;

    @Autowired
    private CustomerOrderMapper customerOrderMapper;

    @Autowired
    private CustomerOrderItemMapper customerOrderItemMapper;

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private AfterSalesCaseMapper afterSalesCaseMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AuthService authService;

    @Value("${file.upload.root-path:D:/upload/file/}")
    private String uploadRoot;

    @Value("${file.upload.allow-types:jpg,png,jpeg,gif,pdf}")
    private String allowTypes;

    @Override
    public void requireCustomerRole(LoginResponse session) {
        if (session == null || !"CUSTOMER".equalsIgnoreCase(session.getRoleCode())) {
            throw new BusinessException(403, "仅客户用户可访问");
        }
    }

    @Override
    public String resolveCustomerName(LoginResponse session) {
        requireCustomerRole(session);
        if (StringUtils.hasText(session.getCustomerName())) {
            return session.getCustomerName().trim();
        }
        User user = authService.getCurrentUser(session.getUserId());
        if (user != null && StringUtils.hasText(user.getCustomerName())) {
            return user.getCustomerName().trim();
        }
        throw new BusinessException("客户账号未绑定企业名称，请联系管理员");
    }

    @Override
    public Map<String, Object> dashboard(LoginResponse session) {
        String customerName = resolveCustomerName(session);
        List<Map<String, Object>> orders = customerPortalMapper.listOrdersByCustomer(customerName);

        List<Map<String, Object>> recentOrders = new ArrayList<>();
        for (int i = 0; i < Math.min(5, orders.size()); i++) {
            Map<String, Object> row = new LinkedHashMap<>(orders.get(i));
            enrichOrderProgress(row);
            recentOrders.add(row);
        }

        List<Map<String, Object>> pendingItems = new ArrayList<>();
        for (Map<String, Object> order : orders) {
            if ("PENDING".equals(order.get("auditStatus"))) {
                pendingItems.add(Map.of(
                        "type", "ORDER_AUDIT",
                        "title", "订单待审核",
                        "refNo", order.get("orderNo"),
                        "detail", "您提交的订单等待工厂审核确认",
                        "at", order.get("createdAt")
                ));
            }
        }
        List<Map<String, Object>> deliveries = new ArrayList<>();
        for (Map<String, Object> order : orders) {
            Long orderId = toLong(order.get("orderId"));
            if (orderId == null) continue;
            List<Map<String, Object>> ds = customerPortalMapper.listDeliveriesByOrder(orderId, customerName);
            for (Map<String, Object> d : ds) {
                if ("PREPARED".equals(d.get("deliveryStatus"))) {
                    deliveries.add(Map.of(
                            "type", "DELIVERY_CONFIRM",
                            "title", "发货待确认",
                            "refNo", d.get("deliveryNo"),
                            "detail", "批次已备货，请确认收货信息",
                            "at", d.get("createdAt")
                    ));
                }
            }
        }
        pendingItems.addAll(deliveries);

        List<Map<String, Object>> feedbacks = customerPortalMapper.listFeedbacksByCustomer(customerName);
        List<Map<String, Object>> recentFeedbacks = feedbacks.stream().limit(5).collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("orderCount", orders.size());
        stats.put("pendingOrderCount", customerPortalMapper.countPendingOrders(customerName));
        stats.put("openFeedbackCount", customerPortalMapper.countOpenFeedbacks(customerName));
        stats.put("inProgressCount", orders.stream().filter(o -> {
            Object st = o.get("auditStatus");
            return "APPROVED".equals(st);
        }).count());

        Map<String, Object> result = new HashMap<>();
        result.put("customerName", customerName);
        result.put("stats", stats);
        result.put("recentOrders", recentOrders);
        result.put("pendingItems", pendingItems);
        result.put("recentFeedbacks", recentFeedbacks);
        return result;
    }

    @Override
    public List<Map<String, Object>> listOrders(LoginResponse session) {
        String customerName = resolveCustomerName(session);
        List<Map<String, Object>> orders = customerPortalMapper.listOrdersByCustomer(customerName);
        for (Map<String, Object> order : orders) {
            enrichOrderProgress(order);
            Long orderId = toLong(order.get("orderId"));
            if (orderId != null) {
                order.put("items", customerPortalMapper.listOrderItems(orderId, customerName));
            }
        }
        return orders;
    }

    @Override
    public Map<String, Object> getOrderDetail(LoginResponse session, Long orderId) {
        String customerName = resolveCustomerName(session);
        Map<String, Object> order = customerPortalMapper.getOrderByIdAndCustomer(orderId, customerName);
        if (order == null) {
            throw new BusinessException("订单不存在或无权查看");
        }
        enrichOrderProgress(order);
        order.put("items", customerPortalMapper.listOrderItems(orderId, customerName));
        order.put("deliveries", customerPortalMapper.listDeliveriesByOrder(orderId, customerName));
        order.put("timeline", buildTimeline(orderId, customerName, order));
        return order;
    }

    @Override
    public Map<String, Object> createOrder(LoginResponse session, CustomerCreateOrderRequest request) {
        String customerName = resolveCustomerName(session);
        if (request == null || request.getMaterialId() == null) {
            throw new BusinessException("请选择产品");
        }
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("数量必须大于 0");
        }
        if (request.getRequiredDeliveryDate() == null) {
            throw new BusinessException("请填写要求交期");
        }

        Material material = materialMapper.getMaterialById(request.getMaterialId());
        if (material == null || !"FINISHED".equals(material.getMaterialType())) {
            throw new BusinessException("产品不存在或不可订购");
        }

        User user = authService.getCurrentUser(session.getUserId());
        BigDecimal unitPrice = material.getStandardCost() != null ? material.getStandardCost() : BigDecimal.ZERO;
        BigDecimal lineAmount = unitPrice.multiply(request.getQuantity()).setScale(2, RoundingMode.HALF_UP);

        LocalDateTime now = LocalDateTime.now();
        String orderNo = nextOrderNo();

        CustomerOrder order = new CustomerOrder();
        order.setOrderNo(orderNo);
        order.setCustomerName(customerName);
        order.setCustomerContact(user.getRealName());
        order.setCustomerPhone(StringUtils.hasText(request.getReceiverPhone()) ? request.getReceiverPhone() : user.getPhone());
        order.setOrderDate(LocalDate.now());
        order.setRequiredDeliveryDate(request.getRequiredDeliveryDate());
        order.setOrderAmount(lineAmount);
        order.setAuditStatus("PENDING");
        StringBuilder remark = new StringBuilder("客户门户提交");
        if (StringUtils.hasText(request.getReceiverAddress())) {
            remark.append("；收货：").append(request.getReceiverName()).append(" ")
                    .append(request.getReceiverPhone()).append(" ").append(request.getReceiverAddress());
        }
        if (request.getAttachmentUrls() != null && !request.getAttachmentUrls().isEmpty()) {
            remark.append("；附件：").append(String.join(",", request.getAttachmentUrls()));
        }
        if (StringUtils.hasText(request.getRemark())) {
            remark.append("；").append(request.getRemark());
        }
        order.setRemark(remark.toString());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        customerOrderMapper.insertCustomerOrder(order);

        CustomerOrderItem item = new CustomerOrderItem();
        item.setOrderId(order.getOrderId());
        item.setMaterialId(material.getMaterialId());
        item.setProductName(StringUtils.hasText(request.getProductName()) ? request.getProductName() : material.getMaterialName());
        item.setSpecification(StringUtils.hasText(request.getSpecification()) ? request.getSpecification() : material.getSpecification());
        item.setQuantity(request.getQuantity());
        item.setUnit(StringUtils.hasText(request.getUnit()) ? request.getUnit() : material.getUnit());
        item.setUnitPrice(unitPrice);
        item.setLineAmount(lineAmount);
        item.setDeliveryDate(request.getRequiredDeliveryDate());
        item.setItemStatus("PENDING");
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        customerOrderItemMapper.insertOrderItem(item);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getOrderId());
        result.put("orderNo", orderNo);
        result.put("message", "订单已提交，等待审核");
        return result;
    }

    @Override
    public List<Map<String, Object>> listProducts() {
        return customerPortalMapper.listFinishedProducts();
    }

    @Override
    public List<Map<String, Object>> listFeedbacks(LoginResponse session) {
        String customerName = resolveCustomerName(session);
        List<Map<String, Object>> list = customerPortalMapper.listFeedbacksByCustomer(customerName);
        for (Map<String, Object> row : list) {
            row.put("progressSteps", buildFeedbackProgress(row));
        }
        return list;
    }

    @Override
    public Map<String, Object> submitFeedback(LoginResponse session, CustomerFeedbackRequest request) {
        String customerName = resolveCustomerName(session);
        if (request == null || !StringUtils.hasText(request.getProblemDescription())) {
            throw new BusinessException("请填写问题描述");
        }
        if (!StringUtils.hasText(request.getProblemType())) {
            throw new BusinessException("请选择问题类型");
        }

        User user = authService.getCurrentUser(session.getUserId());
        if (request.getOrderId() != null) {
            Map<String, Object> order = customerPortalMapper.getOrderByIdAndCustomer(request.getOrderId(), customerName);
            if (order == null) {
                throw new BusinessException("关联订单不存在");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        String caseNo = nextCaseNo();

        AfterSalesCase c = new AfterSalesCase();
        c.setCaseNo(caseNo);
        c.setOrderId(request.getOrderId());
        c.setMaterialId(request.getMaterialId());
        c.setBatchNo(StringUtils.hasText(request.getSerialNo()) ? request.getSerialNo().trim() : null);
        c.setCustomerName(customerName);
        c.setContactName(user.getRealName());
        c.setContactPhone(user.getPhone());
        c.setProblemType(request.getProblemType());
        c.setProblemDescription(request.getProblemDescription());
        if (request.getAttachmentUrls() != null && !request.getAttachmentUrls().isEmpty()) {
            c.setAttachmentUrls(String.join(",", request.getAttachmentUrls()));
        }
        c.setCaseLevel("GENERAL");
        c.setCaseStatus("OPEN");
        c.setOpenedAt(now);
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        afterSalesCaseMapper.insertAfterSalesCase(c);

        return Map.of("caseNo", caseNo, "message", "反馈已提交");
    }

    @Override
    public Map<String, Object> getProfile(LoginResponse session) {
        requireCustomerRole(session);
        User user = authService.getCurrentUser(session.getUserId());
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userId", user.getUserId());
        profile.put("username", user.getUsername());
        profile.put("realName", user.getRealName());
        profile.put("phone", user.getPhone());
        profile.put("email", user.getEmail());
        profile.put("customerName", user.getCustomerName());
        profile.put("shippingAddress", user.getShippingAddress());
        return profile;
    }

    @Override
    public Map<String, Object> updateProfile(LoginResponse session, CustomerProfileUpdateRequest request) {
        requireCustomerRole(session);
        User user = authService.getCurrentUser(session.getUserId());
        if (request != null) {
            if (StringUtils.hasText(request.getRealName())) user.setRealName(request.getRealName().trim());
            if (request.getPhone() != null) user.setPhone(request.getPhone().trim());
            if (request.getEmail() != null) user.setEmail(request.getEmail().trim());
            if (request.getShippingAddress() != null) user.setShippingAddress(request.getShippingAddress().trim());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateUser(user);

        if (StringUtils.hasText(user.getCustomerName())) {
            session.setCustomerName(user.getCustomerName());
        }
        session.setRealName(user.getRealName());
        return getProfile(session);
    }

    @Override
    public String saveUpload(LoginResponse session, MultipartFile file) {
        requireCustomerRole(session);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择文件");
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        }
        List<String> allowed = List.of(allowTypes.split(","));
        if (!allowed.contains(ext)) {
            throw new BusinessException("不支持的文件类型");
        }
        try {
            Path dir = Paths.get(uploadRoot, "customer");
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());
            return "/api/customer/portal/file/" + filename;
        } catch (IOException e) {
            throw new BusinessException("文件保存失败");
        }
    }

    private void enrichOrderProgress(Map<String, Object> order) {
        Long orderId = toLong(order.get("orderId"));
        if (orderId == null) return;
        Map<String, Object> plan = customerPortalMapper.getPlanProgressByOrder(orderId);
        int progress = 0;
        String stage = "待审核";
        if ("APPROVED".equals(order.get("auditStatus"))) {
            stage = "已审核";
            progress = 10;
        }
        if ("PENDING".equals(order.get("auditStatus"))) {
            order.put("progressPercent", 0);
            order.put("currentStage", stage);
            return;
        }
        if (plan != null && !plan.isEmpty()) {
            BigDecimal planned = toDecimal(plan.get("plannedQty"));
            BigDecimal completed = toDecimal(plan.get("completedQty"));
            if (planned.compareTo(BigDecimal.ZERO) > 0) {
                int prod = completed.multiply(BigDecimal.valueOf(70))
                        .divide(planned, 0, RoundingMode.HALF_UP).intValue();
                progress = Math.max(progress, 10 + prod);
            }
            String planStatus = str(plan.get("planStatus"));
            if ("RUNNING".equals(planStatus) || "RELEASED".equals(planStatus)) {
                stage = "生产中";
            }
        }
        Map<String, Object> inspection = customerPortalMapper.getLatestInspectionByOrder(orderId);
        if (inspection != null && !inspection.isEmpty()) {
            progress = Math.max(progress, 75);
            stage = "质检中";
            if ("PASSED".equals(inspection.get("inspectionStatus")) || "QUALIFIED".equals(inspection.get("inspectionResult"))) {
                progress = Math.max(progress, 82);
                stage = "质检完成";
            }
        }
        Map<String, Object> inbound = customerPortalMapper.getLatestInboundByOrder(orderId);
        if (inbound != null && !inbound.isEmpty()) {
            progress = Math.max(progress, 88);
            stage = "已入库";
        }
        List<Map<String, Object>> deliveries = customerPortalMapper.listDeliveriesByOrder(orderId, str(order.get("customerName")));
        boolean shipped = deliveries.stream().anyMatch(d -> "SHIPPED".equals(d.get("deliveryStatus")));
        if (shipped) {
            progress = 100;
            stage = "已发货";
        } else if (!deliveries.isEmpty()) {
            progress = Math.max(progress, 92);
            stage = "待发货";
        }
        order.put("progressPercent", Math.min(progress, 100));
        order.put("currentStage", stage);
        order.put("estimatedDelivery", order.get("requiredDeliveryDate"));
    }

    private List<Map<String, Object>> buildTimeline(Long orderId, String customerName, Map<String, Object> order) {
        List<Map<String, Object>> steps = new ArrayList<>();
        String auditStatus = str(order.get("auditStatus"));
        steps.add(step("审核", auditStepStatus(auditStatus), order.get("auditAt"),
                "PENDING".equals(auditStatus) ? "等待工厂审核" : str(order.get("auditOpinion"))));

        Map<String, Object> plan = customerPortalMapper.getPlanProgressByOrder(orderId);
        String planStatus = plan != null ? str(plan.get("planStatus")) : null;
        steps.add(step("排产", planStepStatus(planStatus), plan != null ? plan.get("approvedAt") : null,
                plan != null ? str(plan.get("planName")) : "尚未排产"));

        BigDecimal planned = plan != null ? toDecimal(plan.get("plannedQty")) : BigDecimal.ZERO;
        BigDecimal completed = plan != null ? toDecimal(plan.get("completedQty")) : BigDecimal.ZERO;
        String prodDetail = planned.compareTo(BigDecimal.ZERO) > 0
                ? "完成 " + completed.stripTrailingZeros().toPlainString() + " / " + planned.stripTrailingZeros().toPlainString()
                : "等待排产";
        steps.add(step("生产", prodStepStatus(planStatus, planned, completed), plan != null ? plan.get("plannedStartDate") : null, prodDetail));

        Map<String, Object> inspection = customerPortalMapper.getLatestInspectionByOrder(orderId);
        steps.add(step("质检", qcStepStatus(inspection), inspection != null ? inspection.get("inspectedAt") : null,
                inspection != null ? str(inspection.get("inspectionNo")) + " " + str(inspection.get("inspectionResult")) : "待质检"));

        Map<String, Object> inbound = customerPortalMapper.getLatestInboundByOrder(orderId);
        steps.add(step("入库", inbound != null ? "done" : (completed.compareTo(BigDecimal.ZERO) > 0 ? "active" : "pending"),
                inbound != null ? inbound.get("handledAt") : null,
                inbound != null ? "入库 " + inbound.get("quantity") + " " + str(inbound.get("remark")) : "待入库"));

        List<Map<String, Object>> deliveries = customerPortalMapper.listDeliveriesByOrder(orderId, customerName);
        Map<String, Object> latestShip = deliveries.stream()
                .filter(d -> "SHIPPED".equals(d.get("deliveryStatus"))).findFirst().orElse(null);
        if (latestShip == null && !deliveries.isEmpty()) {
            latestShip = deliveries.get(0);
        }
        String shipStatus = latestShip == null ? "pending"
                : ("SHIPPED".equals(latestShip.get("deliveryStatus")) ? "done" : "active");
        String shipDetail = latestShip == null ? "待发货"
                : str(latestShip.get("deliveryNo")) + " " + str(latestShip.get("logisticsCompany")) + " " + str(latestShip.get("logisticsNo"));
        steps.add(step("发货", shipStatus, latestShip != null ? latestShip.get("deliveryDate") : null, shipDetail));
        return steps;
    }

    private List<Map<String, Object>> buildFeedbackProgress(Map<String, Object> row) {
        String status = str(row.get("caseStatus"));
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(step("已提交", "done", row.get("openedAt"), "客户提交反馈"));
        steps.add(step("受理中", feedbackStep("OPEN".equals(status) ? "active" : ("PROCESSING".equals(status) || "RESOLVED".equals(status) || "CLOSED".equals(status) ? "done" : "pending")),
                row.get("processingAt"), "售后专员受理"));
        steps.add(step("处理中", feedbackStep("PROCESSING".equals(status) ? "active" : ("RESOLVED".equals(status) || "CLOSED".equals(status) ? "done" : "pending")),
                row.get("processingAt"), str(row.get("handleResult"))));
        steps.add(step("已解决", feedbackStep("RESOLVED".equals(status) ? "active" : ("CLOSED".equals(status) ? "done" : "pending")),
                row.get("resolvedAt"), str(row.get("handleResult"))));
        steps.add(step("已关闭", feedbackStep("CLOSED".equals(status) ? "done" : "pending"),
                row.get("closedAt"), "案例关闭"));
        return steps;
    }

    private Map<String, Object> step(String name, String status, Object time, String detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("status", status);
        m.put("time", time);
        m.put("detail", detail);
        return m;
    }

    private String feedbackStep(String s) { return s; }

    private String auditStepStatus(String auditStatus) {
        if ("APPROVED".equals(auditStatus)) return "done";
        if ("PENDING".equals(auditStatus)) return "active";
        if ("REJECTED".equals(auditStatus)) return "done";
        return "pending";
    }

    private String planStepStatus(String planStatus) {
        if (planStatus == null) return "pending";
        if ("DRAFT".equals(planStatus)) return "active";
        return "done";
    }

    private String prodStepStatus(String planStatus, BigDecimal planned, BigDecimal completed) {
        if (planned.compareTo(BigDecimal.ZERO) <= 0) return "pending";
        if (completed.compareTo(planned) >= 0) return "done";
        if ("RUNNING".equals(planStatus) || completed.compareTo(BigDecimal.ZERO) > 0) return "active";
        return "pending";
    }

    private String qcStepStatus(Map<String, Object> inspection) {
        if (inspection == null || inspection.isEmpty()) return "pending";
        Object result = inspection.get("inspectionResult");
        if ("QUALIFIED".equals(result) || "PASSED".equals(inspection.get("inspectionStatus"))) return "done";
        return "active";
    }

    private String nextOrderNo() {
        String prefix = "CO" + DAY_FMT.format(LocalDate.now());
        Integer max = customerPortalMapper.maxOrderSeqByPrefix(prefix);
        int seq = (max == null ? 0 : max) + 1;
        return prefix + String.format("%03d", seq);
    }

    private String nextCaseNo() {
        String prefix = "AS" + DAY_FMT.format(LocalDate.now());
        Integer max = customerPortalMapper.maxCaseSeqByPrefix(prefix);
        int seq = (max == null ? 0 : max) + 1;
        return prefix + String.format("%03d", seq);
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private BigDecimal toDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(v.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private String str(Object v) {
        return v != null ? v.toString() : "";
    }
}
