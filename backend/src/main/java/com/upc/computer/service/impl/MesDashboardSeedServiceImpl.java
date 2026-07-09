package com.upc.computer.service.impl;

import com.upc.computer.entity.*;
import com.upc.computer.mapper.*;
import com.upc.computer.service.MesDashboardSeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MesDashboardSeedServiceImpl implements MesDashboardSeedService {

    private static final String[] STATION_CODES = {
            "MATERIAL_IN", "PANEL_QC", "MB_ASSEMBLY", "UNIT_ASSEMBLY", "AGING_TEST", "QC", "PACK_IN"
    };
    private static final String[] STATION_NAMES = {
            "原材料入库", "面板检测", "主板装配", "整机组装", "老化测试", "质检", "包装入库"
    };
    private static final String[] STATION_STATUSES = {"RUNNING", "RUNNING", "RUNNING", "WAIT_MATERIAL", "RUNNING", "QC_ABNORMAL", "RUNNING"};

    @Autowired
    private ProdLineStationMapper stationMapper;
    @Autowired
    private ProdHourlyMetricMapper hourlyMetricMapper;
    @Autowired
    private ProdShiftCapacityMapper shiftCapacityMapper;
    @Autowired
    private ProdDowntimeReasonMapper downtimeReasonMapper;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private EquipmentMapper equipmentMapper;

    private final Random random = new Random();

    @Override
    @Transactional
    public void seedIfEmpty() {
        if (stationMapper.countAll() == 0) {
            seedStations();
        }
        LocalDate today = LocalDate.now();
        if (hourlyMetricMapper.countByDate(today) == 0) {
            seedHourlyMetrics(today);
        }
        if (shiftCapacityMapper.countByDate(today) == 0) {
            seedShiftCapacity(today);
        }
        if (downtimeReasonMapper.countByDate(today) == 0) {
            seedDowntimeReasons(today);
        }
    }

    private void seedStations() {
        ArrayList<WorkOrder> workOrders = workOrderMapper.listActiveWorkOrders();
        if (workOrders.isEmpty()) {
            workOrders = workOrderMapper.workOrderList();
        }
        ArrayList<Equipment> equipmentList = equipmentMapper.equipmentList();
        WorkOrder primaryWo = workOrders.isEmpty() ? null : workOrders.get(0);
        WorkOrder secondaryWo = workOrders.size() > 1 ? workOrders.get(1) : primaryWo;

        for (int i = 0; i < STATION_CODES.length; i++) {
            ProdLineStation station = new ProdLineStation();
            station.setStationCode(STATION_CODES[i]);
            station.setStationName(STATION_NAMES[i]);
            station.setLineCode("LINE-A");
            station.setSortNo((i + 1) * 10);
            station.setStationStatus(STATION_STATUSES[i]);
            station.setCurrentQty(BigDecimal.valueOf(12 + i * 3L));
            station.setThroughputPerHour(BigDecimal.valueOf(8 + i));
            if (i < equipmentList.size()) {
                station.setEquipmentId(equipmentList.get(i % equipmentList.size()).getEquipmentId());
            }
            if (i <= 3 && primaryWo != null) {
                station.setWorkOrderId(primaryWo.getWorkOrderId());
                station.setWorkOrderNo(primaryWo.getWorkOrderNo());
            } else if (secondaryWo != null) {
                station.setWorkOrderId(secondaryWo.getWorkOrderId());
                station.setWorkOrderNo(secondaryWo.getWorkOrderNo());
            }
            station.setAlarmFlag("QC_ABNORMAL".equals(STATION_STATUSES[i]) ? 1 : 0);
            station.setRemark("显示器产线工位");
            stationMapper.insert(station);
        }
    }

    private void seedHourlyMetrics(LocalDate today) {
        int currentHour = LocalDateTime.now().getHour();
        int startHour = Math.max(8, currentHour - 8);
        int endHour = Math.max(startHour, currentHour);
        BigDecimal cumulativeActual = BigDecimal.ZERO;
        for (int hour = startHour; hour <= endHour; hour++) {
            ProdHourlyMetric metric = new ProdHourlyMetric();
            metric.setStatDate(today);
            metric.setStatHour(hour);
            BigDecimal planned = BigDecimal.valueOf(18 + (hour - 8) * 2L);
            BigDecimal actual = BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(14, 20));
            if (hour == endHour) {
                actual = actual.add(BigDecimal.valueOf(random.nextInt(3)));
            }
            cumulativeActual = cumulativeActual.add(actual);
            if (cumulativeActual.compareTo(planned.multiply(BigDecimal.valueOf(hour - startHour + 1))) > 0) {
                actual = actual.subtract(BigDecimal.ONE).max(BigDecimal.valueOf(10));
            }
            BigDecimal unqualified = actual.multiply(BigDecimal.valueOf(random.nextDouble() * 0.025 + 0.005))
                    .setScale(0, RoundingMode.HALF_UP);
            BigDecimal qualified = actual.subtract(unqualified).max(BigDecimal.ZERO);
            metric.setPlannedOutput(planned);
            metric.setActualOutput(actual);
            metric.setQualifiedQty(qualified);
            metric.setUnqualifiedQty(unqualified);
            metric.setAlarmCount(hour >= 14 && hour <= 16 ? random.nextInt(2) + 1 : random.nextInt(2));
            hourlyMetricMapper.insert(metric);
        }
    }

    private void seedShiftCapacity(LocalDate today) {
        List<String[]> teams = List.of(
                new String[]{"TEAM-A", "A班贴附组", "王操作"},
                new String[]{"TEAM-B", "B班组装组", "陈操作"},
                new String[]{"TEAM-C", "C班包装组", "周操作"}
        );
        BigDecimal[] planned = {BigDecimal.valueOf(120), BigDecimal.valueOf(110), BigDecimal.valueOf(95)};
        BigDecimal[] actual = {BigDecimal.valueOf(86), BigDecimal.valueOf(72), BigDecimal.valueOf(58)};
        for (int i = 0; i < teams.size(); i++) {
            ProdShiftCapacity cap = new ProdShiftCapacity();
            cap.setStatDate(today);
            cap.setTeamCode(teams.get(i)[0]);
            cap.setTeamName(teams.get(i)[1]);
            cap.setLeaderName(teams.get(i)[2]);
            cap.setPlannedQty(planned[i]);
            cap.setActualQty(actual[i]);
            BigDecimal yield = actual[i].compareTo(BigDecimal.ZERO) > 0
                    ? actual[i].subtract(BigDecimal.valueOf(i + 1))
                            .divide(actual[i], 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                    : BigDecimal.valueOf(98);
            cap.setYieldRate(yield.setScale(2, RoundingMode.HALF_UP));
            cap.setRankNo(i + 1);
            shiftCapacityMapper.insert(cap);
        }
    }

    private void seedDowntimeReasons(LocalDate today) {
        String[][] reasons = {
                {"WAIT_MATERIAL", "待料停机", "35", "4"},
                {"EQ_FAULT", "设备故障", "28", "2"},
                {"QC_HOLD", "质检冻结", "18", "3"},
                {"CHANGEOVER", "换型调试", "12", "2"},
                {"POWER", "电力波动", "8", "1"}
        };
        for (String[] row : reasons) {
            ProdDowntimeReason reason = new ProdDowntimeReason();
            reason.setStatDate(today);
            reason.setReasonCode(row[0]);
            reason.setReasonName(row[1]);
            reason.setDowntimeMinutes(Integer.parseInt(row[2]));
            reason.setOccurrenceCount(Integer.parseInt(row[3]));
            downtimeReasonMapper.insert(reason);
        }
    }
}
