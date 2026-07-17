package com.upc.computer.assistant;

import com.upc.computer.mapper.AttendanceRecordMapper;
import com.upc.computer.mapper.PermissionMapper;
import com.upc.computer.mapper.RoleMenuMapper;
import com.upc.computer.mapper.SysMenuMapper;
import com.upc.computer.service.MesSnapshotService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 为全局智能对话构建当前工厂上下文。
 *
 * <p>MES 业务数据复用 {@link MesSnapshotService} 的统一快照，再补齐此前不在快照中的
 * 权限、菜单、角色菜单关系和考勤。密码、Token、API Key、Cookie 等认证秘密始终剔除，
 * 其余用户、日志及业务字段按当前数据库内容发送给模型。</p>
 */
@Service
public class AssistantFactoryContextService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> SECRET_KEY_PARTS = List.of(
            "password", "passwd", "token", "secret", "apikey", "api_key",
            "authorization", "cookie", "credential"
    );

    private final MesSnapshotService snapshotService;
    private final PermissionMapper permissionMapper;
    private final SysMenuMapper menuMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final AttendanceRecordMapper attendanceMapper;

    public AssistantFactoryContextService(MesSnapshotService snapshotService,
                                          PermissionMapper permissionMapper,
                                          SysMenuMapper menuMapper,
                                          RoleMenuMapper roleMenuMapper,
                                          AttendanceRecordMapper attendanceMapper) {
        this.snapshotService = snapshotService;
        this.permissionMapper = permissionMapper;
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.attendanceMapper = attendanceMapper;
    }

    public Map<String, Object> buildCurrentFactory() {
        Map<String, Object> factory = new LinkedHashMap<>();
        factory.put("capturedAt", LocalDateTime.now().format(TIME_FORMAT));
        factory.put("scope", "当前 MES 工厂全量业务快照；认证秘密已剔除");
        factory.putAll(snapshotService.buildSnapshot());
        factory.put("permissions", permissionMapper.permissionList());
        factory.put("menus", menuMapper.menuList());
        factory.put("roleMenus", roleMenuMapper.listAll());
        factory.put("attendanceRecords", attendanceMapper.listAll());
        return mapValue(sanitize(factory));
    }

    private Object sanitize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> cleaned = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (!isSecretKey(key)) cleaned.put(key, sanitize(entry.getValue()));
            }
            return cleaned;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> cleaned = new ArrayList<>();
            for (Object item : iterable) cleaned.add(sanitize(item));
            return cleaned;
        }
        if (value != null && value.getClass().isArray()) {
            List<Object> cleaned = new ArrayList<>();
            int length = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < length; i++) cleaned.add(sanitize(java.lang.reflect.Array.get(value, i)));
            return cleaned;
        }
        return value;
    }

    private boolean isSecretKey(String key) {
        String normalized = key == null ? "" : key.replace("-", "_").toLowerCase(Locale.ROOT);
        return SECRET_KEY_PARTS.stream().anyMatch(normalized::contains);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : new LinkedHashMap<>();
    }
}
