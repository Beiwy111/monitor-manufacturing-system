package com.upc.computer.service.impl;

import com.upc.computer.mapper.FinanceMapper;
import com.upc.computer.service.CostService;
import com.upc.computer.service.FinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinanceServiceImpl implements FinanceService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired private FinanceMapper financeMapper;
    @Autowired private CostService costService;

    @Override
    public List<Map<String, Object>> listWorkOrderCostOverview() {
        List<Map<String, Object>> list = financeMapper.listWorkOrderCostOverview();
        list.forEach(this::enrichCostRow);
        return list;
    }

    @Override
    public Map<String, Object> costBreakdownSummary() {
        Map<String, Object> kpi = costService.costKpi();
        List<Map<String, Object>> settlements = costService.listSettlementViews();
        BigDecimal rework = sumBySource(settlements, "NONCONFORMING_PRODUCT", true);
        BigDecimal afterSales = sumBySource(settlements, "AFTER_SALES", true);
        BigDecimal warehouse = settlements.stream()
                .filter(s -> List.of("PURCHASE_RETURN", "WORK_ORDER").contains(str(s, "sourceType")))
                .map(s -> dec(s.get("otherCost"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal material = dec(kpi.get("totalMaterialCost"));
        BigDecimal labor = dec(kpi.get("totalLaborCost"));
        BigDecimal equipment = dec(kpi.get("totalEquipmentCost"));
        BigDecimal quality = dec(kpi.get("totalQualityCost"));
        BigDecimal other = dec(kpi.get("totalAmount"))
                .subtract(material).subtract(labor).subtract(equipment).subtract(quality)
                .subtract(rework).subtract(afterSales).subtract(warehouse);
        if (other.compareTo(BigDecimal.ZERO) < 0) other = BigDecimal.ZERO;

        BigDecimal total = material.add(labor).add(equipment).add(quality)
                .add(rework).add(warehouse).add(afterSales).add(other);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("materialCost", material);
        result.put("laborCost", labor);
        result.put("equipmentCost", equipment);
        result.put("qualityCost", quality);
        result.put("reworkScrapCost", rework);
        result.put("warehouseLogisticsCost", warehouse);
        result.put("afterSalesCost", afterSales);
        result.put("otherCost", other);
        result.put("totalCost", total);
        result.put("formula", "材料+人工+设备+质检+返工报废+仓储物流+售后+其他");
        return result;
    }

    @Override
    public List<Map<String, Object>> listOrderRevenue() {
        List<Map<String, Object>> orders = financeMapper.listOrderRevenueBase();
        Map<Long, Map<String, Object>> costByOrder = financeMapper.costSumByOrder().stream()
                .collect(Collectors.toMap(r -> longVal(r.get("orderId")), r -> r, (a, b) -> a));
        Map<Long, Map<String, Object>> paymentByOrder = financeMapper.listPayments().stream()
                .collect(Collectors.toMap(r -> longVal(r.get("orderId")), r -> r, (a, b) -> a));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> o : orders) {
            Long orderId = longVal(o.get("orderId"));
            BigDecimal deliveredQty = dec(o.get("deliveredQty"));
            BigDecimal unitPrice = dec(o.get("unitPrice"));
            BigDecimal salesRevenue = deliveredQty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            if (salesRevenue.compareTo(BigDecimal.ZERO) == 0) {
                salesRevenue = dec(o.get("orderLineAmount"));
            }

            Map<String, Object> pay = paymentByOrder.getOrDefault(orderId, Map.of());
            BigDecimal discount = dec(pay.get("discountAmount"));
            BigDecimal refund = dec(pay.get("refundAmount"));
            BigDecimal tax = dec(pay.get("taxAmount"));
            BigDecimal actualIncome = salesRevenue.subtract(discount).subtract(refund).max(BigDecimal.ZERO);

            Map<String, Object> cost = costByOrder.getOrDefault(orderId, Map.of());
            BigDecimal totalCost = dec(cost.get("totalCost"));

            BigDecimal profit = actualIncome.subtract(totalCost);
            BigDecimal margin = actualIncome.compareTo(BigDecimal.ZERO) > 0
                    ? profit.multiply(BigDecimal.valueOf(100)).divide(actualIncome, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderId", orderId);
            row.put("orderNo", o.get("orderNo"));
            row.put("customerName", o.get("customerName"));
            row.put("productName", o.get("productName"));
            row.put("deliveredQty", deliveredQty);
            row.put("unitPrice", unitPrice);
            row.put("salesRevenue", salesRevenue);
            row.put("discountAmount", discount);
            row.put("refundAmount", refund);
            row.put("taxAmount", tax);
            row.put("actualIncome", actualIncome);
            row.put("totalCost", totalCost);
            row.put("profit", profit);
            row.put("grossMargin", margin);
            row.put("accountingStatus", accountingStatus(o, pay, totalCost));
            row.put("accountingStatusCn", accountingStatusCn(str(row, "accountingStatus")));
            row.put("auditStatus", o.get("auditStatus"));
            row.put("requiredDeliveryDate", o.get("requiredDeliveryDate"));
            result.add(row);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> listPayments() {
        List<Map<String, Object>> list = financeMapper.listPayments();
        list.forEach(row -> {
            row.put("paymentStatusCn", paymentStatusCn(str(row, "paymentStatus")));
            fmtDate(row, "plannedDate");
            fmtDate(row, "actualDate");
            fmtTime(row, "createdAt");
            fmtTime(row, "updatedAt");
            BigDecimal pending = dec(row.get("receivableAmount"));
            row.put("pendingAmount", pending);
        });
        return list;
    }

    @Override
    public List<Map<String, Object>> listReceivables() {
        List<Map<String, Object>> customers = financeMapper.listReceivableByCustomer();
        List<Map<String, Object>> logs = financeMapper.listCollectionLogs();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> c : customers) {
            String customer = str(c, "customerName");
            List<Map<String, Object>> customerLogs = logs.stream()
                    .filter(l -> customer.equals(str(l, "customerName")))
                    .limit(5)
                    .map(l -> {
                        Map<String, Object> m = new LinkedHashMap<>(l);
                        fmtTime(m, "createdAt");
                        return m;
                    })
                    .toList();
            BigDecimal overdue = dec(c.get("overdueAmount"));
            int overdueDays = intVal(c.get("maxOverdueDays"));
            String risk = overdueDays >= 30 || overdue.compareTo(BigDecimal.valueOf(50000)) > 0 ? "HIGH"
                    : overdueDays >= 7 ? "MEDIUM" : "LOW";

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("customerName", customer);
            row.put("totalDebt", dec(c.get("totalDebt")));
            row.put("overdueAmount", overdue);
            row.put("overdueDays", overdueDays);
            row.put("creditRisk", risk);
            row.put("creditRiskCn", riskCn(risk));
            row.put("collectionLogs", customerLogs);
            result.add(row);
        }
        return result;
    }

    @Override
    public Map<String, Object> profitAnalysis() {
        List<Map<String, Object>> orders = listOrderRevenue();
        List<Map<String, Object>> byOrder = orders.stream()
                .sorted((a, b) -> dec(b.get("profit")).compareTo(dec(a.get("profit"))))
                .limit(10)
                .map(this::profitRankRow)
                .collect(Collectors.toList());

        Map<String, BigDecimal[]> byCustomer = new LinkedHashMap<>();
        for (Map<String, Object> o : orders) {
            String key = str(o, "customerName");
            BigDecimal[] acc = byCustomer.computeIfAbsent(key, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            acc[0] = acc[0].add(dec(o.get("actualIncome")));
            acc[1] = acc[1].add(dec(o.get("totalCost")));
            acc[2] = acc[2].add(dec(o.get("profit")));
        }
        List<Map<String, Object>> customerRank = byCustomer.entrySet().stream()
                .map(e -> rankEntry(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .sorted((a, b) -> dec(b.get("profit")).compareTo(dec(a.get("profit"))))
                .limit(10).collect(Collectors.toList());

        Map<String, BigDecimal[]> byProduct = new LinkedHashMap<>();
        for (Map<String, Object> o : orders) {
            String key = str(o, "productName");
            if (key.isBlank()) key = "未命名产品";
            BigDecimal[] acc = byProduct.computeIfAbsent(key, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            acc[0] = acc[0].add(dec(o.get("actualIncome")));
            acc[1] = acc[1].add(dec(o.get("totalCost")));
            acc[2] = acc[2].add(dec(o.get("profit")));
        }
        List<Map<String, Object>> productRank = byProduct.entrySet().stream()
                .map(e -> rankEntry(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .sorted((a, b) -> dec(b.get("profit")).compareTo(dec(a.get("profit"))))
                .limit(10).collect(Collectors.toList());

        List<Map<String, Object>> lossOrders = orders.stream()
                .filter(o -> dec(o.get("profit")).compareTo(BigDecimal.ZERO) < 0
                        || dec(o.get("grossMargin")).compareTo(BigDecimal.valueOf(10)) < 0)
                .map(o -> {
                    Map<String, Object> m = profitRankRow(o);
                    m.put("flag", dec(o.get("profit")).compareTo(BigDecimal.ZERO) < 0 ? "LOSS" : "LOW_MARGIN");
                    m.put("flagCn", dec(o.get("profit")).compareTo(BigDecimal.ZERO) < 0 ? "亏损" : "低毛利");
                    return m;
                })
                .limit(15)
                .collect(Collectors.toList());

        return Map.of(
                "orderRank", byOrder,
                "customerRank", customerRank,
                "productRank", productRank,
                "alerts", lossOrders
        );
    }

    @Override
    public Map<String, Object> financeScreen(int days) {
        List<Map<String, Object>> orders = listOrderRevenue();
        Map<String, Object> costSummary = costBreakdownSummary();
        List<Map<String, Object>> payments = listPayments();

        LocalDate today = LocalDate.now();
        List<String> dayLabels = new ArrayList<>();
        List<BigDecimal> incomeTrend = new ArrayList<>();
        List<BigDecimal> costTrend = new ArrayList<>();
        List<BigDecimal> profitTrend = new ArrayList<>();
        List<BigDecimal> marginTrend = new ArrayList<>();
        List<BigDecimal> collectionTrend = new ArrayList<>();

        List<Map<String, Object>> settlements = costService.listSettlementViews();
        BigDecimal totalIncome = orders.stream().map(o -> dec(o.get("actualIncome"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            dayLabels.add(d.getMonthValue() + "/" + d.getDayOfMonth());
            final LocalDate fd = d;
            BigDecimal dayCost = settlements.stream()
                    .filter(s -> List.of("CONFIRMED", "EXPORTED").contains(str(s, "settlementStatus")))
                    .filter(s -> matchDate(s.get("confirmedAt"), fd))
                    .map(s -> dec(s.get("totalCost")))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal dayIncome = payments.stream()
                    .filter(p -> matchDate(p.get("actualDate"), fd))
                    .map(p -> dec(p.get("receivedAmount")))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (dayIncome.compareTo(BigDecimal.ZERO) == 0 && !orders.isEmpty()) {
                dayIncome = totalIncome.divide(BigDecimal.valueOf(Math.max(days, 1)), 2, RoundingMode.HALF_UP);
            }
            BigDecimal dayProfit = dayIncome.subtract(dayCost);
            BigDecimal dayMargin = dayIncome.compareTo(BigDecimal.ZERO) > 0
                    ? dayProfit.multiply(BigDecimal.valueOf(100)).divide(dayIncome, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            incomeTrend.add(dayIncome);
            costTrend.add(dayCost);
            profitTrend.add(dayProfit);
            marginTrend.add(dayMargin);
            collectionTrend.add(dayIncome);
        }

        BigDecimal totalCost = dec(costSummary.get("totalCost"));
        BigDecimal totalProfit = totalIncome.subtract(totalCost);

        List<Map<String, Object>> costStructure = List.of(
                slice("材料成本", costSummary.get("materialCost")),
                slice("人工成本", costSummary.get("laborCost")),
                slice("设备成本", costSummary.get("equipmentCost")),
                slice("质检成本", costSummary.get("qualityCost")),
                slice("返工报废", costSummary.get("reworkScrapCost")),
                slice("仓储物流", costSummary.get("warehouseLogisticsCost")),
                slice("售后成本", costSummary.get("afterSalesCost")),
                slice("其他成本", costSummary.get("otherCost"))
        );

        List<Map<String, Object>> profitRank = orders.stream()
                .sorted((a, b) -> dec(b.get("profit")).compareTo(dec(a.get("profit"))))
                .limit(8).map(this::profitRankRow).collect(Collectors.toList());

        List<Map<String, Object>> lossAlerts = orders.stream()
                .filter(o -> dec(o.get("profit")).compareTo(BigDecimal.ZERO) < 0)
                .limit(6)
                .map(o -> Map.<String, Object>of(
                        "orderNo", o.get("orderNo"),
                        "customerName", o.get("customerName"),
                        "profit", o.get("profit"),
                        "grossMargin", o.get("grossMargin")
                ))
                .collect(Collectors.toList());

        long profitCount = orders.stream().filter(o -> dec(o.get("profit")).compareTo(BigDecimal.ZERO) >= 0).count();
        long lossCount = orders.size() - profitCount;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", Map.of(
                "totalIncome", totalIncome, "totalCost", totalCost, "totalProfit", totalProfit,
                "avgMargin", totalIncome.compareTo(BigDecimal.ZERO) > 0
                        ? totalProfit.multiply(BigDecimal.valueOf(100)).divide(totalIncome, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO
        ));
        result.put("dayLabels", dayLabels);
        result.put("incomeTrend", incomeTrend);
        result.put("costTrend", costTrend);
        result.put("profitTrend", profitTrend);
        result.put("marginTrend", marginTrend);
        result.put("collectionTrend", collectionTrend);
        result.put("costStructure", costStructure);
        result.put("profitRank", profitRank);
        result.put("profitDistribution", List.of(
                Map.of("name", "盈利订单", "value", profitCount),
                Map.of("name", "亏损订单", "value", lossCount)
        ));
        result.put("lossAlerts", lossAlerts);
        result.put("waterfall", buildWaterfall(totalIncome, costSummary));
        return result;
    }

    @Override
    public Map<String, Object> financeReport(String period) {
        String p = (period == null || period.isBlank())
                ? YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                : period;
        List<Map<String, Object>> orders = listOrderRevenue();
        List<Map<String, Object>> payments = listPayments();
        Map<String, Object> costSummary = costBreakdownSummary();
        Map<String, Object> profit = profitAnalysis();

        BigDecimal monthIncome = orders.stream()
                .map(o -> dec(o.get("actualIncome")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthCost = costService.listSettlementViews().stream()
                .filter(s -> p.equals(String.valueOf(s.get("settlementPeriod"))))
                .map(s -> dec(s.get("totalCost")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (monthCost.compareTo(BigDecimal.ZERO) == 0) {
            monthCost = dec(costSummary.get("totalCost"));
        }

        BigDecimal monthProfit = monthIncome.subtract(monthCost);
        BigDecimal monthMargin = monthIncome.compareTo(BigDecimal.ZERO) > 0
                ? monthProfit.multiply(BigDecimal.valueOf(100)).divide(monthIncome, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal monthReceived = payments.stream()
                .filter(pay -> pay.get("actualDate") != null && String.valueOf(pay.get("actualDate")).startsWith(p))
                .map(pay -> dec(pay.get("receivedAmount")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthReceivable = payments.stream()
                .map(pay -> dec(pay.get("receivableAmount")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("period", p);
        report.put("income", monthIncome);
        report.put("cost", monthCost);
        report.put("profit", monthProfit);
        report.put("grossMargin", monthMargin);
        report.put("received", monthReceived);
        report.put("receivable", monthReceivable);
        report.put("costBreakdown", costSummary);
        report.put("costVariance", Map.of(
                "budgetCost", monthCost.multiply(BigDecimal.valueOf(1.05)).setScale(2, RoundingMode.HALF_UP),
                "actualCost", monthCost,
                "variance", monthCost.multiply(BigDecimal.valueOf(-0.05)).setScale(2, RoundingMode.HALF_UP)
        ));
        report.put("lossOrders", profit.get("alerts"));
        report.put("customerRank", profit.get("customerRank"));
        report.put("productRank", profit.get("productRank"));
        report.put("generatedAt", LocalDate.now().format(DAY));
        return report;
    }

    // ── helpers ───────────────────────────────────────────────

    private void enrichCostRow(Map<String, Object> row) {
        BigDecimal total = dec(row.get("materialCost")).add(dec(row.get("laborCost")))
                .add(dec(row.get("equipmentCost"))).add(dec(row.get("qualityCost")))
                .add(dec(row.get("reworkScrapCost"))).add(dec(row.get("warehouseLogisticsCost")))
                .add(dec(row.get("afterSalesCost"))).add(dec(row.get("otherCost")));
        if (dec(row.get("totalCost")).compareTo(BigDecimal.ZERO) == 0) row.put("totalCost", total);
        row.put("settlementStatusCn", settlementStatusCn(str(row, "settlementStatus")));
    }

    private Map<String, Object> profitRankRow(Map<String, Object> o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", o.get("orderNo"));
        m.put("customerName", o.get("customerName"));
        m.put("productName", o.get("productName"));
        m.put("income", o.get("actualIncome"));
        m.put("cost", o.get("totalCost"));
        m.put("profit", o.get("profit"));
        m.put("grossMargin", o.get("grossMargin"));
        return m;
    }

    private Map<String, Object> rankEntry(String name, BigDecimal income, BigDecimal cost, BigDecimal profit) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("income", income);
        m.put("cost", cost);
        m.put("profit", profit);
        m.put("grossMargin", income.compareTo(BigDecimal.ZERO) > 0
                ? profit.multiply(BigDecimal.valueOf(100)).divide(income, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        return m;
    }

    private List<Map<String, Object>> buildWaterfall(BigDecimal income, Map<String, Object> cost) {
        BigDecimal running = income;
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(Map.of("name", "销售收入", "value", income, "type", "total"));
        String[][] keys = {
                {"materialCost", "材料成本"}, {"laborCost", "人工成本"}, {"equipmentCost", "设备成本"},
                {"qualityCost", "质检成本"}, {"reworkScrapCost", "返工报废"}, {"warehouseLogisticsCost", "仓储物流"},
                {"afterSalesCost", "售后成本"}, {"otherCost", "其他成本"}
        };
        for (String[] k : keys) {
            BigDecimal v = dec(cost.get(k[0]));
            if (v.compareTo(BigDecimal.ZERO) > 0) {
                steps.add(Map.of("name", k[1], "value", v.negate(), "type", "decrease"));
                running = running.subtract(v);
            }
        }
        steps.add(Map.of("name", "净利润", "value", running, "type", "total"));
        return steps;
    }

    private Map<String, Object> slice(String name, Object value) {
        return Map.of("name", name, "value", dec(value));
    }

    private BigDecimal sumBySource(List<Map<String, Object>> list, String type, boolean useTotal) {
        return list.stream()
                .filter(s -> type.equals(str(s, "sourceType")))
                .map(s -> useTotal ? dec(s.get("totalCost")) : dec(s.get("qualityCost")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String accountingStatus(Map<String, Object> order, Map<String, Object> pay, BigDecimal totalCost) {
        if (totalCost.compareTo(BigDecimal.ZERO) == 0) return "PENDING_COST";
        if (pay.isEmpty()) return "PENDING_PAYMENT";
        String ps = str(pay, "paymentStatus");
        if ("RECEIVED".equals(ps)) return "SETTLED";
        if (List.of("PARTIAL", "OVERDUE").contains(ps)) return "PARTIAL";
        return "ACCOUNTED";
    }

    private String accountingStatusCn(String s) {
        return switch (s) {
            case "SETTLED" -> "已结清";
            case "PARTIAL" -> "部分回款";
            case "PENDING_COST" -> "待成本核算";
            case "PENDING_PAYMENT" -> "待回款";
            default -> "已核算";
        };
    }

    private String paymentStatusCn(String s) {
        return switch (s) {
            case "RECEIVED" -> "已回款";
            case "PARTIAL" -> "部分回款";
            case "OVERDUE" -> "已逾期";
            default -> "待回款";
        };
    }

    private String settlementStatusCn(String s) {
        return switch (s) {
            case "CONFIRMED" -> "已确认";
            case "EXPORTED" -> "已导出";
            case "CANCELLED" -> "已取消";
            default -> "草稿";
        };
    }

    private String riskCn(String r) {
        return switch (r) {
            case "HIGH" -> "高风险";
            case "MEDIUM" -> "中风险";
            default -> "低风险";
        };
    }

    private boolean matchDate(Object v, LocalDate d) {
        if (v == null) return false;
        try {
            String s = String.valueOf(v);
            LocalDate ld = s.length() >= 10 ? LocalDate.parse(s.substring(0, 10)) : null;
            return d.equals(ld);
        } catch (Exception e) {
            return false;
        }
    }

    private void fmtTime(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v instanceof java.time.LocalDateTime ldt) row.put(key, ldt.format(FMT));
    }

    private void fmtDate(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v instanceof LocalDate ld) row.put(key, ld.format(DAY));
        else if (v instanceof java.time.LocalDateTime ldt) row.put(key, ldt.toLocalDate().format(DAY));
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? "" : v.toString();
    }

    private BigDecimal dec(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        try { return new BigDecimal(v.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private int intVal(Object v) {
        if (v == null) return 0;
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return 0; }
    }

    private long longVal(Object v) {
        if (v == null) return 0L;
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return 0L; }
    }
}
