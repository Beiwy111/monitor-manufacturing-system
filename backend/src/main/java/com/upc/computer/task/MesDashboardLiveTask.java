package com.upc.computer.task;

import com.upc.computer.entity.*;
import com.upc.computer.mapper.*;
import com.upc.computer.service.MesDashboardSeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 定期刷新生产大屏数据，模拟现场实时变化
 */
@Component
public class MesDashboardLiveTask {

    private static final int STEP_SECONDS = 30;
    private static final String[] ALARM_TYPES = {"EQUIPMENT", "QUALITY", "MATERIAL"};
    private static final String[] ALARM_LEVELS = {"GENERAL", "URGENT"};
    private static final String[] ALARM_DESC = {
            "贴附机真空吸附压力偏低",
            "老化测试治具接触不良",
            "LCD面板批次色差偏大",
            "包装线贴标机卡纸",
            "组装线扭矩枪校准偏差"
    };

    @Autowired
    private MesDashboardSeedService seedService;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private ProdLineStationMapper stationMapper;
    @Autowired
    private ProdHourlyMetricMapper hourlyMetricMapper;
    @Autowired
    private ProdShiftCapacityMapper shiftCapacityMapper;
    @Autowired
    private ProdDowntimeReasonMapper downtimeReasonMapper;
    @Autowired
    private EquipmentMapper equipmentMapper;
    @Autowired
    private AndonAlarmMapper alarmMapper;
    @Autowired
    private DispatchTaskMapper dispatchTaskMapper;

    @Scheduled(fixedRate = STEP_SECONDS * 1000L, initialDelay = 5000)
    @Transactional
    public void refreshLiveData() {
        seedService.seedIfEmpty();
        LocalDate today = LocalDate.now();
        int hour = LocalDateTime.now().getHour();
        boolean hasActiveProduction = tickProductionStep();
        tickHourlyMetric(today, hour);
        syncEquipmentFromDispatches(hasActiveProduction);
        tickStationsFromEquipment(hasActiveProduction);
        tickShiftCapacity(today);
        tickEquipmentAlarms(hasActiveProduction);
        tickDowntimeReasons(today);
        purgeOldClosedAlarms();
    }

    /** 清理历史已关闭安灯，避免全表扫描拖慢接口 */
    private void purgeOldClosedAlarms() {
        if (ThreadLocalRandom.current().nextInt(100) >= 20) {
            return;
        }
        alarmMapper.deleteClosedBefore(LocalDateTime.now().minusDays(2));
    }

    private boolean tickProductionStep() {
        ArrayList<WorkOrder> activeOrders = workOrderMapper.listActiveWorkOrders();
        if (activeOrders.isEmpty()) {
            return false;
        }
        Set<Long> activeOrderIds = new HashSet<>();
        for (WorkOrder wo : activeOrders) {
            activeOrderIds.add(wo.getWorkOrderId());
        }
        return dispatchTaskMapper.dispatchList().stream()
                .anyMatch(d -> activeOrderIds.contains(d.getWorkOrderId())
                        && List.of("ASSIGNED", "ACCEPTED", "PRODUCING", "RUNNING").contains(d.getStatus()));
    }

    private void tickHourlyMetric(LocalDate today, int hour) {
        if (hour < 6) {
            return;
        }
        ProdHourlyMetric metric = hourlyMetricMapper.getByDateHour(today, hour);
        if (metric == null) {
            metric = new ProdHourlyMetric();
            metric.setStatDate(today);
            metric.setStatHour(hour);
            metric.setPlannedOutput(BigDecimal.valueOf(20));
            metric.setActualOutput(BigDecimal.ZERO);
            metric.setQualifiedQty(BigDecimal.ZERO);
            metric.setUnqualifiedQty(BigDecimal.ZERO);
            metric.setAlarmCount(0);
            hourlyMetricMapper.insert(metric);
        }
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        BigDecimal actual = metric.getActualOutput().add(BigDecimal.valueOf(rnd.nextInt(1, 4)));
        BigDecimal planned = metric.getPlannedOutput();
        if (actual.compareTo(planned.multiply(BigDecimal.valueOf(1.05))) > 0) {
            actual = planned;
        }
        BigDecimal unqualified = metric.getUnqualifiedQty();
        if (rnd.nextInt(100) < 6) {
            unqualified = unqualified.add(BigDecimal.ONE);
        }
        metric.setActualOutput(actual);
        metric.setQualifiedQty(actual.subtract(unqualified).max(BigDecimal.ZERO));
        metric.setUnqualifiedQty(unqualified);
        if (rnd.nextInt(100) < 5) {
            metric.setAlarmCount(metric.getAlarmCount() + 1);
        }
        hourlyMetricMapper.update(metric);
    }

    private void syncEquipmentFromDispatches(boolean hasActiveProduction) {
        ArrayList<Equipment> equipmentList = equipmentMapper.equipmentList();
        ArrayList<DispatchTask> dispatches = dispatchTaskMapper.dispatchList();
        Set<Long> activeOrderIds = new HashSet<>();
        for (WorkOrder wo : workOrderMapper.listActiveWorkOrders()) {
            activeOrderIds.add(wo.getWorkOrderId());
        }
        Set<Long> activeEquipmentIds = new HashSet<>();
        for (DispatchTask dispatch : dispatches) {
            if (dispatch.getEquipmentId() == null) {
                continue;
            }
            String status = dispatch.getStatus();
            if (activeOrderIds.contains(dispatch.getWorkOrderId())
                    && ("RUNNING".equals(status) || "ACCEPTED".equals(status) || "ASSIGNED".equals(status))) {
                activeEquipmentIds.add(dispatch.getEquipmentId());
            }
        }
        for (Equipment eq : equipmentList) {
            if ("MAINTENANCE".equals(eq.getStatus()) || "FAULT".equals(eq.getStatus())) {
                continue;
            }
            String nextStatus = hasActiveProduction && activeEquipmentIds.contains(eq.getEquipmentId()) ? "RUNNING" : "IDLE";
            if (!nextStatus.equals(eq.getStatus())) {
                eq.setStatus(nextStatus);
                eq.setUpdatedAt(LocalDateTime.now());
                equipmentMapper.updateEquipment(eq);
            }
        }
    }

    private void tickStationsFromEquipment(boolean hasActiveProduction) {
        ArrayList<ProdLineStation> stations = stationMapper.listAll();
        ArrayList<Equipment> equipmentList = equipmentMapper.equipmentList();
        long faultCount = equipmentList.stream().filter(e -> "FAULT".equals(e.getStatus())).count();
        long runningCount = equipmentList.stream().filter(e -> "RUNNING".equals(e.getStatus())).count();
        ArrayList<WorkOrder> workOrders = workOrderMapper.listActiveWorkOrders();
        for (ProdLineStation station : stations) {
            if (!hasActiveProduction) {
                station.setStationStatus("IDLE");
                station.setAlarmFlag(0);
            } else if (faultCount > 0) {
                station.setStationStatus("FAULT");
                station.setAlarmFlag(1);
            } else if (runningCount > 0) {
                station.setStationStatus("RUNNING");
                station.setAlarmFlag(0);
            } else {
                station.setStationStatus("IDLE");
                station.setAlarmFlag(0);
            }
            station.setCurrentQty(station.getCurrentQty().add(BigDecimal.valueOf(hasActiveProduction && runningCount > 0 ? 1 : 0)));
            station.setThroughputPerHour(BigDecimal.valueOf(6 + Math.min(8, runningCount * 2)));
            if (!workOrders.isEmpty()) {
                WorkOrder wo = workOrders.get(0);
                station.setWorkOrderId(wo.getWorkOrderId());
                station.setWorkOrderNo(wo.getWorkOrderNo());
            }
            stationMapper.update(station);
        }
    }

    private void tickShiftCapacity(LocalDate today) {
        ArrayList<ProdShiftCapacity> list = shiftCapacityMapper.listByDate(today);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (ProdShiftCapacity cap : list) {
            cap.setActualQty(cap.getActualQty().add(BigDecimal.valueOf(rnd.nextInt(0, 3))));
            if (cap.getActualQty().compareTo(cap.getPlannedQty()) > 0) {
                cap.setActualQty(cap.getPlannedQty());
            }
            BigDecimal unq = cap.getActualQty().multiply(BigDecimal.valueOf(0.012));
            BigDecimal yield = cap.getActualQty().compareTo(BigDecimal.ZERO) > 0
                    ? cap.getActualQty().subtract(unq).divide(cap.getActualQty(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.valueOf(98);
            cap.setYieldRate(yield.setScale(2, RoundingMode.HALF_UP));
            shiftCapacityMapper.update(cap);
        }
        list.sort((a, b) -> b.getActualQty().compareTo(a.getActualQty()));
        for (int i = 0; i < list.size(); i++) {
            ProdShiftCapacity cap = list.get(i);
            cap.setRankNo(i + 1);
            shiftCapacityMapper.update(cap);
        }
    }

    private void tickEquipmentAlarms(boolean hasActiveProduction) {
        if (!hasActiveProduction) {
            return;
        }
        ArrayList<Equipment> equipmentList = equipmentMapper.equipmentList();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int openCount = alarmMapper.countOpenAlarms();
        if (openCount < 5 && rnd.nextInt(100) < 8) {
            AndonAlarm alarm = new AndonAlarm();
            alarm.setAlarmNo("AL" + System.currentTimeMillis());
            ArrayList<WorkOrder> wos = workOrderMapper.listActiveWorkOrders();
            if (!wos.isEmpty()) {
                alarm.setWorkOrderId(wos.get(0).getWorkOrderId());
            }
            if (!equipmentList.isEmpty()) {
                alarm.setEquipmentId(equipmentList.get(rnd.nextInt(equipmentList.size())).getEquipmentId());
            }
            alarm.setAlarmType(ALARM_TYPES[rnd.nextInt(ALARM_TYPES.length)]);
            alarm.setAlarmLevel(ALARM_LEVELS[rnd.nextInt(ALARM_LEVELS.length)]);
            alarm.setAlarmDescription(ALARM_DESC[rnd.nextInt(ALARM_DESC.length)]);
            alarm.setAlarmStatus("OPEN");
            alarm.setReportedBy(3L);
            alarm.setReportedAt(LocalDateTime.now());
            LocalDateTime now = LocalDateTime.now();
            alarm.setCreatedAt(now);
            alarm.setUpdatedAt(now);
            alarmMapper.insertAlarm(alarm);
        } else if (openCount > 0 && rnd.nextInt(100) < 20) {
            alarmMapper.listOpenAlarms(1).stream()
                    .findFirst()
                    .ifPresent(a -> {
                        a.setAlarmStatus("CLOSED");
                        a.setClosedBy(1L);
                        a.setClosedAt(LocalDateTime.now());
                        a.setCloseResult("现场处理后恢复生产");
                        alarmMapper.updateAlarm(a);
                    });
        }
    }

    private void tickDowntimeReasons(LocalDate today) {
        ArrayList<ProdDowntimeReason> reasons = downtimeReasonMapper.listByDate(today);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        if (reasons.isEmpty()) {
            return;
        }
        ProdDowntimeReason reason = reasons.get(rnd.nextInt(reasons.size()));
        reason.setDowntimeMinutes(reason.getDowntimeMinutes() + rnd.nextInt(1, 4));
        reason.setOccurrenceCount(reason.getOccurrenceCount() + (rnd.nextInt(100) < 30 ? 1 : 0));
        downtimeReasonMapper.update(reason);
    }
}
