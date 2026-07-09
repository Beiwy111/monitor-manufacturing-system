package com.upc.computer.service;

import com.upc.computer.entity.AfterSalesCase;
import com.upc.computer.entity.CustomerOrder;
import com.upc.computer.entity.DeliveryOrder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户主数据：默认种子 + 从业务单据合并，供快照与下单共用。
 */
public final class CustomerCatalog {

    private static final List<String[]> SEED_CUSTOMERS = List.of(
            new String[]{"深圳华创科技有限公司", "林经理", "13900001001"},
            new String[]{"北京星辰电竞俱乐部", "王总", "13900001002"},
            new String[]{"上海视觉设计工作室", "陈设计师", "13900001003"},
            new String[]{"广州教育设备有限公司", "黄主任", "13900001004"},
            new String[]{"杭州电商运营中心", "张运营", "13900001005"}
    );

    private CustomerCatalog() {
    }

    public static List<Map<String, Object>> buildList(
            List<CustomerOrder> orders,
            List<DeliveryOrder> deliveries,
            List<AfterSalesCase> afterSalesCases) {
        Map<String, Map<String, Object>> byName = new LinkedHashMap<>();
        int nextId = 1;
        for (String[] seed : SEED_CUSTOMERS) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("id", nextId++);
            c.put("name", seed[0]);
            c.put("contact", seed[1]);
            c.put("phone", seed[2]);
            byName.put(seed[0], c);
        }
        if (orders != null) {
            for (CustomerOrder o : orders) {
                nextId = merge(byName, o.getCustomerName(), o.getCustomerContact(), o.getCustomerPhone(), nextId);
            }
        }
        if (deliveries != null) {
            for (DeliveryOrder d : deliveries) {
                nextId = merge(byName, d.getCustomerName(), d.getReceiverName(), d.getReceiverPhone(), nextId);
            }
        }
        if (afterSalesCases != null) {
            for (AfterSalesCase a : afterSalesCases) {
                nextId = merge(byName, a.getCustomerName(), a.getContactName(), a.getContactPhone(), nextId);
            }
        }
        return new ArrayList<>(byName.values());
    }

    public static String resolveName(
            int customerId,
            List<CustomerOrder> orders,
            List<DeliveryOrder> deliveries,
            List<AfterSalesCase> afterSalesCases) {
        if (customerId <= 0) {
            return "";
        }
        for (Map<String, Object> c : buildList(orders, deliveries, afterSalesCases)) {
            if (customerId == toInt(c.get("id"))) {
                return String.valueOf(c.get("name"));
            }
        }
        return "";
    }

    private static int merge(
            Map<String, Map<String, Object>> byName,
            String name,
            String contact,
            String phone,
            int nextId) {
        if (name == null || name.isBlank()) {
            return nextId;
        }
        Map<String, Object> existing = byName.get(name);
        if (existing != null) {
            if (isBlank(existing.get("contact")) && !isBlank(contact)) {
                existing.put("contact", contact);
            }
            if (isBlank(existing.get("phone")) && !isBlank(phone)) {
                existing.put("phone", phone);
            }
            return nextId;
        }
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("id", nextId);
        c.put("name", name);
        c.put("contact", contact != null ? contact : "");
        c.put("phone", phone != null ? phone : "");
        byName.put(name, c);
        return nextId + 1;
    }

    private static boolean isBlank(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }

    private static int toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
