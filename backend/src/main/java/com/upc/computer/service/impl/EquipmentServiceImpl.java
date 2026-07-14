package com.upc.computer.service.impl;

import com.upc.computer.common.BusinessException;
import com.upc.computer.entity.AndonAlarm;
import com.upc.computer.entity.Equipment;
import com.upc.computer.entity.EquipmentMaintenanceRecord;
import com.upc.computer.entity.User;
import com.upc.computer.mapper.AndonAlarmMapper;
import com.upc.computer.mapper.EquipmentMaintenanceRecordMapper;
import com.upc.computer.mapper.EquipmentMapper;
import com.upc.computer.mapper.UserMapper;
import com.upc.computer.service.EquipmentService;
import com.upc.computer.service.MesPlannerAgentService;
import com.upc.computer.service.ProductionWorkshopCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EquipmentServiceImpl implements EquipmentService {

    // 设备状态（对应 chk_equipment_status）
    private static final String EQ_IDLE = "IDLE";
    private static final String EQ_RUNNING = "RUNNING";
    private static final String EQ_FAULT = "FAULT";
    private static final String EQ_MAINTAINING = "MAINTAINING";
    private static final String EQ_SCRAPPED = "SCRAPPED";

    // 报警状态（对应 chk_andon_alarm_status）
    private static final String AL_OPEN = "OPEN";
    private static final String AL_RECEIVED = "RECEIVED";
    private static final String AL_PROCESSING = "PROCESSING";
    private static final String AL_CLOSED = "CLOSED";
    private static final String AL_CANCELLED = "CANCELLED";

    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired private EquipmentMapper equipmentMapper;
    @Autowired private AndonAlarmMapper andonAlarmMapper;
    @Autowired private EquipmentMaintenanceRecordMapper equipmentMaintenanceRecordMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private MesPlannerAgentService plannerAgentService;

    // ===== 基础 CRUD（保留） =====
    @Override public ArrayList<Equipment> equipmentList() { return equipmentMapper.equipmentList(); }
    @Override public Equipment getEquipmentById(Long equipmentId) { return equipmentMapper.getEquipmentById(equipmentId); }
    @Override public void insertEquipment(Equipment equipment) {
        if (equipment.getStatus() == null || equipment.getStatus().isBlank()) equipment.setStatus(EQ_IDLE);
        equipmentMapper.insertEquipment(equipment);
    }
    @Override public void updateEquipment(Equipment equipment) { equipmentMapper.updateEquipment(equipment); }
    @Override public void deleteEquipment(Long equipmentId) { equipmentMapper.deleteEquipment(equipmentId); }
    @Override public ArrayList<AndonAlarm> alarmList() { return andonAlarmMapper.alarmList(); }
    @Override public AndonAlarm getAlarmById(Long alarmId) { return andonAlarmMapper.getAlarmById(alarmId); }
    @Override public void insertAlarm(AndonAlarm alarm) { andonAlarmMapper.insertAlarm(alarm); }
    @Override public void updateAlarm(AndonAlarm alarm) { andonAlarmMapper.updateAlarm(alarm); }
    @Override public void deleteAlarm(Long alarmId) { andonAlarmMapper.deleteAlarm(alarmId); }
    @Override public ArrayList<EquipmentMaintenanceRecord> maintenanceList() { return equipmentMaintenanceRecordMapper.maintenanceList(); }
    @Override public EquipmentMaintenanceRecord getMaintenanceById(Long maintenanceId) { return equipmentMaintenanceRecordMapper.getMaintenanceById(maintenanceId); }
    @Override public void insertMaintenance(EquipmentMaintenanceRecord maintenance) { equipmentMaintenanceRecordMapper.insertMaintenance(maintenance); }
    @Override public void updateMaintenance(EquipmentMaintenanceRecord maintenance) { equipmentMaintenanceRecordMapper.updateMaintenance(maintenance); }
    @Override public void deleteMaintenance(Long maintenanceId) { equipmentMaintenanceRecordMapper.deleteMaintenance(maintenanceId); }

    // ===== 视图 & KPI =====
    @Override
    public List<Map<String, Object>> equipmentViews() {
        List<AndonAlarm> alarms = andonAlarmMapper.alarmList();
        List<EquipmentMaintenanceRecord> records = equipmentMaintenanceRecordMapper.maintenanceList();
        // 每台设备的未闭环报警数
        Map<Long, Long> openAlarmCount = alarms.stream()
                .filter(a -> a.getEquipmentId() != null && isAlarmOpen(a.getAlarmStatus()))
                .collect(Collectors.groupingBy(AndonAlarm::getEquipmentId, Collectors.counting()));
        // 每台设备进行中的维保记录
        Map<Long, EquipmentMaintenanceRecord> activeMaintenance = new HashMap<>();
        for (EquipmentMaintenanceRecord r : records) {
            if (r.getEquipmentId() != null && r.getEndTime() == null) {
                activeMaintenance.putIfAbsent(r.getEquipmentId(), r);
            }
        }
        return equipmentMapper.equipmentList().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("equipmentId", e.getEquipmentId());
            m.put("equipmentCode", e.getEquipmentCode());
            m.put("equipmentName", e.getEquipmentName());
            m.put("equipmentType", e.getEquipmentType());
            enrichWorkshopCatalog(m, e);
            m.put("workstation", e.getWorkstation());
            m.put("status", safe(e.getStatus()));
            m.put("statusCn", equipmentStatusCn(e.getStatus()));
            m.put("openAlarmCount", openAlarmCount.getOrDefault(e.getEquipmentId(), 0L));
            EquipmentMaintenanceRecord am = activeMaintenance.get(e.getEquipmentId());
            m.put("activeMaintenanceId", am != null ? am.getMaintenanceId() : null);
            m.put("lastMaintenanceAt", fmt(e.getLastMaintenanceAt()));
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> equipmentKpi() {
        List<Equipment> all = equipmentMapper.equipmentList();
        List<Equipment> production = all.stream().filter(this::isProductionEquipment).toList();
        List<AndonAlarm> alarms = andonAlarmMapper.alarmList();
        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("total", production.size());
        kpi.put("workshopCount", ProductionWorkshopCatalog.allWorkshops().size());
        kpi.put("stageCount", ProductionWorkshopCatalog.PRODUCTION_STAGES.size());
        kpi.put("normal", production.stream().filter(e -> EQ_IDLE.equals(safe(e.getStatus())) || EQ_RUNNING.equals(safe(e.getStatus()))).count());
        kpi.put("fault", production.stream().filter(e -> EQ_FAULT.equals(safe(e.getStatus()))).count());
        kpi.put("maintaining", production.stream().filter(e -> EQ_MAINTAINING.equals(safe(e.getStatus()))).count());
        kpi.put("scrapped", production.stream().filter(e -> EQ_SCRAPPED.equals(safe(e.getStatus()))).count());
        kpi.put("openAlarms", alarms.stream().filter(a -> isAlarmOpen(a.getAlarmStatus())).count());
        kpi.put("allEquipmentTotal", all.size());
        kpi.put("postProductionTotal", all.size() - production.size());
        return kpi;
    }

    @Override
    public Map<String, Object> workshopOverview() {
        return plannerAgentService.buildProductionOverview();
    }

    @Override
    public List<Map<String, Object>> alarmViews() {
        Map<Long, Equipment> eqMap = equipmentMap();
        Map<Long, User> userMap = userMap();
        return andonAlarmMapper.alarmList().stream()
                .map(a -> alarmView(a, eqMap, userMap))
                .sorted((x, y) -> String.valueOf(y.get("reportedAt")).compareTo(String.valueOf(x.get("reportedAt"))))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> maintenanceViews() {
        Map<Long, Equipment> eqMap = equipmentMap();
        Map<Long, User> userMap = userMap();
        return equipmentMaintenanceRecordMapper.maintenanceList().stream()
                .map(r -> maintenanceView(r, eqMap, userMap))
                .sorted((x, y) -> String.valueOf(y.get("startTime")).compareTo(String.valueOf(x.get("startTime"))))
                .collect(Collectors.toList());
    }

    // ===== 闭环动作 =====
    @Override
    @Transactional
    public AndonAlarm triggerAlarm(Long equipmentId, String alarmType, String alarmLevel, String description, String operator) {
        Equipment eq = requireEquipment(equipmentId);
        if (EQ_SCRAPPED.equals(safe(eq.getStatus()))) {
            throw new BusinessException("设备已报废，无法触发报警");
        }
        LocalDateTime now = LocalDateTime.now();
        Long reporter = resolveUserId(operator);

        AndonAlarm alarm = new AndonAlarm();
        alarm.setAlarmNo(nextNo("AL", andonAlarmMapper.alarmList(), AndonAlarm::getAlarmNo));
        alarm.setEquipmentId(equipmentId);
        alarm.setAlarmType(normalizeAlarmType(alarmType));
        alarm.setAlarmLevel(normalizeAlarmLevel(alarmLevel));
        alarm.setAlarmDescription((description == null || description.isBlank()) ? "设备异常报警" : description.trim());
        alarm.setAlarmStatus(AL_OPEN);
        alarm.setReportedBy(reporter);
        alarm.setReportedAt(now);
        alarm.setCreatedAt(now);
        alarm.setUpdatedAt(now);
        andonAlarmMapper.insertAlarm(alarm);

        // 设备状态：正常/运行 -> 故障（维保中则保持维保中）
        if (!EQ_MAINTAINING.equals(safe(eq.getStatus()))) {
            eq.setStatus(EQ_FAULT);
            eq.setUpdatedAt(now);
            equipmentMapper.updateEquipment(eq);
        }
        return alarm;
    }

    @Override
    @Transactional
    public AndonAlarm receiveAlarm(Long alarmId, String operator) {
        AndonAlarm alarm = requireAlarm(alarmId);
        if (!AL_OPEN.equals(safe(alarm.getAlarmStatus()))) {
            throw new BusinessException("仅待接收(OPEN)状态可接收，当前：" + alarmStatusCn(alarm.getAlarmStatus()));
        }
        LocalDateTime now = LocalDateTime.now();
        alarm.setAlarmStatus(AL_RECEIVED);
        alarm.setReceivedBy(resolveUserId(operator));
        alarm.setReceivedAt(now);
        alarm.setUpdatedAt(now);
        andonAlarmMapper.updateAlarm(alarm);
        return alarm;
    }

    @Override
    @Transactional
    public AndonAlarm resolveAlarm(Long alarmId, String closeResult, String operator) {
        AndonAlarm alarm = requireAlarm(alarmId);
        String st = safe(alarm.getAlarmStatus());
        if (AL_CLOSED.equals(st) || AL_CANCELLED.equals(st)) {
            throw new BusinessException("报警已" + alarmStatusCn(st) + "，无需重复处理");
        }
        LocalDateTime now = LocalDateTime.now();
        alarm.setAlarmStatus(AL_CLOSED);
        alarm.setClosedBy(resolveUserId(operator));
        alarm.setClosedAt(now);
        alarm.setCloseResult((closeResult == null || closeResult.isBlank()) ? "已解除" : closeResult.trim());
        alarm.setUpdatedAt(now);
        andonAlarmMapper.updateAlarm(alarm);

        // 若设备处于故障、且无其它未闭环报警、无进行中维保 -> 恢复空闲
        recoverEquipmentIfClear(alarm.getEquipmentId(), now);
        return alarm;
    }

    @Override
    @Transactional
    public EquipmentMaintenanceRecord startMaintenance(Long equipmentId, Long alarmId, String maintenanceType,
                                                       String faultDescription, String maintenanceContent, String operator) {
        Equipment eq = requireEquipment(equipmentId);
        String st = safe(eq.getStatus());
        if (EQ_MAINTAINING.equals(st)) {
            throw new BusinessException("设备已在维保中，请勿重复开始");
        }
        if (!EQ_FAULT.equals(st)) {
            throw new BusinessException("仅故障状态设备可开始维保（当前：" + equipmentStatusCn(st) + "）");
        }
        LocalDateTime now = LocalDateTime.now();
        Long maintainer = resolveUserId(operator);

        EquipmentMaintenanceRecord r = new EquipmentMaintenanceRecord();
        r.setMaintenanceNo(nextNo("MR", equipmentMaintenanceRecordMapper.maintenanceList(), EquipmentMaintenanceRecord::getMaintenanceNo));
        r.setEquipmentId(equipmentId);
        r.setAlarmId(alarmId);
        r.setMaintenanceType(normalizeMaintenanceType(maintenanceType));
        r.setFaultDescription(faultDescription);
        r.setMaintenanceContent((maintenanceContent == null || maintenanceContent.isBlank()) ? "设备维修处理" : maintenanceContent.trim());
        r.setStartTime(now);
        r.setDowntimeMinutes(0);
        r.setMaintenanceResult("COMPLETED"); // 占位，完成时改写；进行中以 end_time IS NULL 判定
        r.setMaintainerId(maintainer);
        r.setCostAmount(BigDecimal.ZERO);
        r.setCreatedAt(now);
        r.setUpdatedAt(now);
        equipmentMaintenanceRecordMapper.insertMaintenance(r);

        // 设备 -> 维保中
        eq.setStatus(EQ_MAINTAINING);
        eq.setUpdatedAt(now);
        equipmentMapper.updateEquipment(eq);

        // 关联报警推进为处理中
        if (alarmId != null) {
            AndonAlarm alarm = andonAlarmMapper.getAlarmById(alarmId);
            if (alarm != null && isAlarmOpen(alarm.getAlarmStatus())) {
                if (alarm.getReceivedBy() == null) { alarm.setReceivedBy(maintainer); alarm.setReceivedAt(now); }
                alarm.setAlarmStatus(AL_PROCESSING);
                alarm.setUpdatedAt(now);
                andonAlarmMapper.updateAlarm(alarm);
            }
        }
        return r;
    }

    @Override
    @Transactional
    public EquipmentMaintenanceRecord finishMaintenance(Long maintenanceId, String result, BigDecimal costAmount,
                                                        String maintenanceContent, String operator) {
        EquipmentMaintenanceRecord r = equipmentMaintenanceRecordMapper.getMaintenanceById(maintenanceId);
        if (r == null) throw new BusinessException("维保记录不存在");
        if (r.getEndTime() != null) throw new BusinessException("该维保记录已完成，请勿重复操作");
        LocalDateTime now = LocalDateTime.now();

        r.setEndTime(now);
        LocalDateTime start = r.getStartTime() != null ? r.getStartTime() : now;
        long minutes = Math.max(0, Duration.between(start, now).toMinutes());
        r.setDowntimeMinutes((int) minutes);
        r.setMaintenanceResult(normalizeMaintenanceResult(result));
        if (costAmount != null && costAmount.compareTo(BigDecimal.ZERO) >= 0) r.setCostAmount(costAmount);
        if (maintenanceContent != null && !maintenanceContent.isBlank()) r.setMaintenanceContent(maintenanceContent.trim());
        r.setUpdatedAt(now);
        equipmentMaintenanceRecordMapper.updateMaintenance(r);

        // 设备维保中 -> 恢复空闲，并记录最近维护时间
        Equipment eq = equipmentMapper.getEquipmentById(r.getEquipmentId());
        if (eq != null) {
            if (EQ_MAINTAINING.equals(safe(eq.getStatus()))) eq.setStatus(EQ_IDLE);
            eq.setLastMaintenanceAt(now);
            eq.setUpdatedAt(now);
            equipmentMapper.updateEquipment(eq);
        }

        // 关闭关联报警，形成闭环
        if (r.getAlarmId() != null) {
            AndonAlarm alarm = andonAlarmMapper.getAlarmById(r.getAlarmId());
            if (alarm != null && isAlarmOpen(alarm.getAlarmStatus())) {
                alarm.setAlarmStatus(AL_CLOSED);
                alarm.setClosedBy(resolveUserId(operator));
                alarm.setClosedAt(now);
                alarm.setCloseResult("维保完成：" + r.getMaintenanceNo());
                alarm.setUpdatedAt(now);
                andonAlarmMapper.updateAlarm(alarm);
            }
        }
        return r;
    }

    @Override
    public List<Map<String, Object>> calcHealthList() {
        List<Equipment> equipments = equipmentMapper.equipmentList();
        List<AndonAlarm> alarms = andonAlarmMapper.alarmList();
        List<EquipmentMaintenanceRecord> records = equipmentMaintenanceRecordMapper.maintenanceList();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since7d = now.minusDays(7);
        LocalDateTime since30d = now.minusDays(30);
        Map<Long, Equipment> eqMap = equipmentMap();
        Map<Long, User> userMap = userMap();

        Map<Long, Long> alarm7dCount = alarms.stream()
                .filter(a -> a.getEquipmentId() != null && a.getReportedAt() != null && a.getReportedAt().isAfter(since7d))
                .collect(Collectors.groupingBy(AndonAlarm::getEquipmentId, Collectors.counting()));
        Map<Long, List<AndonAlarm>> alarm7dByEq = alarms.stream()
                .filter(a -> a.getEquipmentId() != null && a.getReportedAt() != null && a.getReportedAt().isAfter(since7d))
                .collect(Collectors.groupingBy(AndonAlarm::getEquipmentId));
        Map<Long, Long> faultCount30d = records.stream()
                .filter(r -> r.getEquipmentId() != null && r.getStartTime() != null && r.getStartTime().isAfter(since30d))
                .filter(r -> "REPAIR".equals(r.getMaintenanceType()) || "OVERHAUL".equals(r.getMaintenanceType()))
                .collect(Collectors.groupingBy(EquipmentMaintenanceRecord::getEquipmentId, Collectors.counting()));
        Map<Long, List<EquipmentMaintenanceRecord>> recordsByEq = records.stream()
                .filter(r -> r.getEquipmentId() != null)
                .collect(Collectors.groupingBy(EquipmentMaintenanceRecord::getEquipmentId));

        return equipments.stream().map(eq -> {
            Long eqId = eq.getEquipmentId();
            String code = safe(eq.getEquipmentCode());
            int runHours = simulateRunHours(code);
            int deductRun = runHours > 800 ? 20 : runHours > 500 ? 10 : 0;

            long alarm7d = alarm7dCount.getOrDefault(eqId, 0L);
            int deductAlarm = alarm7d >= 10 ? 30 : alarm7d >= 6 ? 20 : alarm7d >= 3 ? 10 : 0;

            int daysSinceMaint;
            if (eq.getLastMaintenanceAt() != null) {
                daysSinceMaint = (int) Duration.between(eq.getLastMaintenanceAt(), now).toDays();
            } else {
                daysSinceMaint = 999;
            }
            int deductMaint = daysSinceMaint > 90 ? 25 : daysSinceMaint > 60 ? 18 : daysSinceMaint > 30 ? 10 : 0;

            long fault30d = faultCount30d.getOrDefault(eqId, 0L);
            int deductNc = fault30d >= 10 ? 25 : fault30d >= 5 ? 15 : fault30d >= 2 ? 5 : 0;

            int score = Math.max(0, 100 - deductRun - deductAlarm - deductMaint - deductNc);
            String level = scoreToLevel(score);

            List<Map<String, Object>> alarmList7d = alarm7dByEq.getOrDefault(eqId, List.of()).stream()
                    .sorted((a, b) -> b.getReportedAt().compareTo(a.getReportedAt()))
                    .map(a -> alarmView(a, eqMap, userMap))
                    .collect(Collectors.toList());
            List<Map<String, Object>> maintList = recordsByEq.getOrDefault(eqId, List.of()).stream()
                    .sorted((a, b) -> {
                        LocalDateTime ta = a.getStartTime() != null ? a.getStartTime() : LocalDateTime.MIN;
                        LocalDateTime tb = b.getStartTime() != null ? b.getStartTime() : LocalDateTime.MIN;
                        return tb.compareTo(ta);
                    })
                    .limit(5)
                    .map(r -> maintenanceView(r, eqMap, userMap))
                    .collect(Collectors.toList());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("equipmentId", eqId);
            result.put("equipmentCode", code);
            result.put("equipmentName", safe(eq.getEquipmentName()));
            result.put("equipmentType", safe(eq.getEquipmentType()));
            result.put("workshop", safe(eq.getWorkshop()));
            result.put("status", safe(eq.getStatus()));
            result.put("statusCn", equipmentStatusCn(eq.getStatus()));
            result.put("lastMaintenanceAt", fmt(eq.getLastMaintenanceAt()));
            result.put("healthScore", score);
            result.put("healthLevel", level);
            result.put("runHours", runHours);
            result.put("alarm7d", alarm7d);
            result.put("daysSinceMaint", daysSinceMaint);
            result.put("faultCount30d", fault30d);
            result.put("deductRun", deductRun);
            result.put("deductAlarm", deductAlarm);
            result.put("deductMaint", deductMaint);
            result.put("deductNc", deductNc);
            result.put("advice", genAdvice(score, code, alarm7d, daysSinceMaint, fault30d));
            result.put("alarmList7d", alarmList7d);
            result.put("maintList", maintList);
            return result;
        }).sorted(Comparator.comparingInt(a -> (Integer) a.get("healthScore"))).collect(Collectors.toList());
    }

    // ===== 内部辅助 =====
    private boolean isProductionEquipment(Equipment equipment) {
        if (equipment == null) {
            return false;
        }
        return ProductionWorkshopCatalog.allWorkshops().stream()
                .anyMatch(ws -> ProductionWorkshopCatalog.equipmentBelongsToWorkshop(equipment, ws));
    }

    private void enrichWorkshopCatalog(Map<String, Object> m, Equipment e) {
        for (ProductionWorkshopCatalog.WorkshopDef ws : ProductionWorkshopCatalog.allWorkshops()) {
            if (ProductionWorkshopCatalog.equipmentBelongsToWorkshop(e, ws)) {
                ProductionWorkshopCatalog.ProcessStageDef stage = ProductionWorkshopCatalog.stageByKey(ws.parentStepKey());
                m.put("workshopKey", ws.key());
                m.put("workshop", ws.workshopName());
                m.put("parentStepKey", ws.parentStepKey());
                m.put("parentStepName", stage != null ? stage.stepName() : "");
                m.put("isProductionWorkshop", true);
                return;
            }
        }
        m.put("workshop", e.getWorkshop());
        m.put("workshopKey", "");
        m.put("parentStepKey", "");
        m.put("parentStepName", "后置工序");
        m.put("isProductionWorkshop", false);
    }

    private void recoverEquipmentIfClear(Long equipmentId, LocalDateTime now) {
        if (equipmentId == null) return;
        Equipment eq = equipmentMapper.getEquipmentById(equipmentId);
        if (eq == null || !EQ_FAULT.equals(safe(eq.getStatus()))) return;
        boolean hasOpenAlarm = andonAlarmMapper.alarmList().stream()
                .anyMatch(a -> equipmentId.equals(a.getEquipmentId()) && isAlarmOpen(a.getAlarmStatus()));
        boolean hasActiveMaintenance = equipmentMaintenanceRecordMapper.maintenanceList().stream()
                .anyMatch(r -> equipmentId.equals(r.getEquipmentId()) && r.getEndTime() == null);
        if (!hasOpenAlarm && !hasActiveMaintenance) {
            eq.setStatus(EQ_IDLE);
            eq.setUpdatedAt(now);
            equipmentMapper.updateEquipment(eq);
        }
    }

    private Map<String, Object> alarmView(AndonAlarm a, Map<Long, Equipment> eqMap, Map<Long, User> userMap) {
        Equipment eq = a.getEquipmentId() != null ? eqMap.get(a.getEquipmentId()) : null;
        User reporter = a.getReportedBy() != null ? userMap.get(a.getReportedBy()) : null;
        User receiver = a.getReceivedBy() != null ? userMap.get(a.getReceivedBy()) : null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("alarmId", a.getAlarmId());
        m.put("alarmNo", a.getAlarmNo());
        m.put("equipmentId", a.getEquipmentId());
        m.put("equipmentName", eq != null ? eq.getEquipmentName() : "");
        m.put("equipmentCode", eq != null ? eq.getEquipmentCode() : "");
        if (eq != null) {
            Map<String, Object> wsMeta = new LinkedHashMap<>();
            enrichWorkshopCatalog(wsMeta, eq);
            m.put("workshop", wsMeta.get("workshop"));
            m.put("parentStepName", wsMeta.get("parentStepName"));
        } else {
            m.put("workshop", "");
            m.put("parentStepName", "");
        }
        m.put("alarmType", a.getAlarmType());
        m.put("alarmTypeCn", alarmTypeCn(a.getAlarmType()));
        m.put("alarmLevel", a.getAlarmLevel());
        m.put("alarmLevelCn", alarmLevelCn(a.getAlarmLevel()));
        m.put("alarmDescription", a.getAlarmDescription());
        m.put("alarmStatus", safe(a.getAlarmStatus()));
        m.put("alarmStatusCn", alarmStatusCn(a.getAlarmStatus()));
        m.put("reporterName", reporter != null ? reporter.getRealName() : "");
        m.put("reportedAt", fmt(a.getReportedAt()));
        m.put("receiverName", receiver != null ? receiver.getRealName() : "");
        m.put("receivedAt", fmt(a.getReceivedAt()));
        m.put("closeResult", a.getCloseResult());
        m.put("closedAt", fmt(a.getClosedAt()));
        return m;
    }

    private Map<String, Object> maintenanceView(EquipmentMaintenanceRecord r, Map<Long, Equipment> eqMap, Map<Long, User> userMap) {
        Equipment eq = r.getEquipmentId() != null ? eqMap.get(r.getEquipmentId()) : null;
        User maintainer = r.getMaintainerId() != null ? userMap.get(r.getMaintainerId()) : null;
        boolean inProgress = r.getEndTime() == null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("maintenanceId", r.getMaintenanceId());
        m.put("maintenanceNo", r.getMaintenanceNo());
        m.put("equipmentId", r.getEquipmentId());
        m.put("equipmentName", eq != null ? eq.getEquipmentName() : "");
        m.put("equipmentCode", eq != null ? eq.getEquipmentCode() : "");
        m.put("alarmId", r.getAlarmId());
        m.put("maintenanceType", r.getMaintenanceType());
        m.put("maintenanceTypeCn", maintenanceTypeCn(r.getMaintenanceType()));
        m.put("faultDescription", r.getFaultDescription());
        m.put("maintenanceContent", r.getMaintenanceContent());
        m.put("startTime", fmt(r.getStartTime()));
        m.put("endTime", fmt(r.getEndTime()));
        m.put("downtimeMinutes", r.getDowntimeMinutes());
        m.put("inProgress", inProgress);
        m.put("maintenanceResult", inProgress ? "IN_PROGRESS" : safe(r.getMaintenanceResult()));
        m.put("maintenanceResultCn", inProgress ? "维修中" : maintenanceResultCn(r.getMaintenanceResult()));
        m.put("maintainerName", maintainer != null ? maintainer.getRealName() : "");
        m.put("costAmount", r.getCostAmount());
        m.put("remark", r.getRemark());
        return m;
    }

    private Equipment requireEquipment(Long id) {
        if (id == null) throw new BusinessException("设备ID不能为空");
        Equipment eq = equipmentMapper.getEquipmentById(id);
        if (eq == null) throw new BusinessException("设备不存在");
        return eq;
    }

    private AndonAlarm requireAlarm(Long id) {
        if (id == null) throw new BusinessException("报警ID不能为空");
        AndonAlarm alarm = andonAlarmMapper.getAlarmById(id);
        if (alarm == null) throw new BusinessException("报警记录不存在");
        return alarm;
    }

    /** 解析操作人用户ID：优先按用户名，找不到则回退第一个用户，保证外键非空 */
    private Long resolveUserId(String username) {
        if (username != null && !username.isBlank()) {
            User u = userMapper.getUserByUsername(username);
            if (u != null) return u.getUserId();
        }
        List<User> users = userMapper.userList();
        if (users == null || users.isEmpty()) throw new BusinessException("系统无可用操作人");
        return users.get(0).getUserId();
    }

    private boolean isAlarmOpen(String status) {
        String s = safe(status);
        return AL_OPEN.equals(s) || AL_RECEIVED.equals(s) || AL_PROCESSING.equals(s);
    }

    private Map<Long, Equipment> equipmentMap() {
        return equipmentMapper.equipmentList().stream()
                .collect(Collectors.toMap(Equipment::getEquipmentId, e -> e, (a, b) -> a));
    }
    private Map<Long, User> userMap() {
        return userMapper.userList().stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));
    }

    private <T> String nextNo(String prefix, List<T> list, Function<T, String> fn) {
        String fp = prefix + LocalDate.now().format(YM_FMT);
        int max = list.stream().map(fn).filter(Objects::nonNull).filter(n -> n.startsWith(fp))
                .mapToInt(n -> { try { return Integer.parseInt(n.substring(fp.length())); } catch (Exception e) { return 0; } })
                .max().orElse(0);
        return fp + String.format("%03d", max + 1);
    }

    private String normalizeAlarmType(String t) {
        if (t == null || t.isBlank()) return "EQUIPMENT";
        List<String> allow = List.of("EQUIPMENT", "QUALITY", "MATERIAL", "PROCESS", "SAFETY", "OTHER");
        return allow.contains(t) ? t : "EQUIPMENT";
    }
    private String normalizeAlarmLevel(String l) {
        if (l == null || l.isBlank()) return "GENERAL";
        List<String> allow = List.of("GENERAL", "IMPORTANT", "URGENT");
        return allow.contains(l) ? l : "GENERAL";
    }
    private String normalizeMaintenanceType(String t) {
        if (t == null || t.isBlank()) return "REPAIR";
        List<String> allow = List.of("INSPECTION", "REPAIR", "MAINTENANCE", "OVERHAUL");
        return allow.contains(t) ? t : "REPAIR";
    }
    private String normalizeMaintenanceResult(String r) {
        if (r == null || r.isBlank()) return "COMPLETED";
        List<String> allow = List.of("COMPLETED", "UNRESOLVED", "TEMPORARY_FIXED");
        return allow.contains(r) ? r : "COMPLETED";
    }

    private String equipmentStatusCn(String db) {
        if (db == null) return "空闲";
        return switch (db) {
            case "IDLE" -> "空闲"; case "RUNNING" -> "运行中"; case "FAULT" -> "故障";
            case "MAINTAINING" -> "维保中"; case "SCRAPPED" -> "已报废"; default -> db;
        };
    }
    private String alarmStatusCn(String db) {
        if (db == null) return "";
        return switch (db) {
            case "OPEN" -> "待接收"; case "RECEIVED" -> "已接收"; case "PROCESSING" -> "处理中";
            case "CLOSED" -> "已关闭"; case "CANCELLED" -> "已取消"; default -> db;
        };
    }
    private String alarmLevelCn(String db) {
        if (db == null) return "";
        return switch (db) { case "GENERAL" -> "一般"; case "IMPORTANT" -> "重要"; case "URGENT" -> "紧急"; default -> db; };
    }
    private String alarmTypeCn(String db) {
        if (db == null) return "";
        return switch (db) {
            case "EQUIPMENT" -> "设备"; case "QUALITY" -> "质量"; case "MATERIAL" -> "物料";
            case "PROCESS" -> "工艺"; case "SAFETY" -> "安全"; case "OTHER" -> "其它"; default -> db;
        };
    }
    private String maintenanceTypeCn(String db) {
        if (db == null) return "";
        return switch (db) {
            case "INSPECTION" -> "点检"; case "REPAIR" -> "维修"; case "MAINTENANCE" -> "保养"; case "OVERHAUL" -> "大修"; default -> db;
        };
    }
    private String maintenanceResultCn(String db) {
        if (db == null) return "";
        return switch (db) {
            case "COMPLETED" -> "已完成"; case "UNRESOLVED" -> "未解决"; case "TEMPORARY_FIXED" -> "临时修复"; default -> db;
        };
    }

    private String scoreToLevel(int s) {
        return s >= 85 ? "GOOD" : s >= 65 ? "WARN" : s >= 40 ? "ALERT" : "DANGER";
    }

    private int simulateRunHours(String code) {
        return 100 + (Math.abs(code.hashCode()) % 900);
    }

    private String genAdvice(int score, String code, long alarm7d, int daysSinceMaint, long faultCount30d) {
        if (score >= 85) return code + " 运行状态优良，无需干预。";
        List<String> parts = new ArrayList<>();
        if (alarm7d >= 6) parts.add("近7天报警 " + alarm7d + " 次，频率异常");
        if (daysSinceMaint > 60) parts.add("已 " + daysSinceMaint + " 天未维保，超出保养周期");
        if (faultCount30d >= 5) parts.add("近30天故障维修 " + faultCount30d + " 次，故障率偏高");
        if (parts.isEmpty()) parts.add("健康度有所下降，建议关注");
        String urgency = score < 40 ? "立即" : score < 65 ? "8小时内" : "本周内";
        return code + " 健康度 " + score + "，" + String.join("；", parts) + "，建议 " + urgency + " 安排维护。";
    }

    private String safe(String v) { return v == null ? "" : v; }
    private String fmt(LocalDateTime dt) { return dt != null ? dt.format(DT_FMT) : ""; }
}
