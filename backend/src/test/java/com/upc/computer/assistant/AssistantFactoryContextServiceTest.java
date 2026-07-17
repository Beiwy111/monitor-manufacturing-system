package com.upc.computer.assistant;

import com.upc.computer.mapper.AttendanceRecordMapper;
import com.upc.computer.mapper.PermissionMapper;
import com.upc.computer.mapper.RoleMenuMapper;
import com.upc.computer.mapper.SysMenuMapper;
import com.upc.computer.service.MesSnapshotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantFactoryContextServiceTest {

    @Mock private MesSnapshotService snapshotService;
    @Mock private PermissionMapper permissionMapper;
    @Mock private SysMenuMapper menuMapper;
    @Mock private RoleMenuMapper roleMenuMapper;
    @Mock private AttendanceRecordMapper attendanceMapper;

    @Test
    void buildsFullFactoryDatasetsAndRemovesAuthenticationSecrets() {
        when(snapshotService.buildSnapshot()).thenReturn(Map.of(
                "orders", List.of(Map.of("id", "ORD-1", "status", "生产中")),
                "sysUsers", List.of(Map.of(
                        "username", "admin",
                        "passwordHash", "must-not-leave-server",
                        "sessionToken", "must-not-leave-server"
                ))
        ));
        when(permissionMapper.permissionList()).thenReturn(new ArrayList<>());
        when(menuMapper.menuList()).thenReturn(new ArrayList<>());
        when(roleMenuMapper.listAll()).thenReturn(List.of(Map.of("roleId", 1, "menuId", 2)));
        when(attendanceMapper.listAll()).thenReturn(new ArrayList<>());

        AssistantFactoryContextService service = new AssistantFactoryContextService(
                snapshotService, permissionMapper, menuMapper, roleMenuMapper, attendanceMapper);

        Map<String, Object> result = service.buildCurrentFactory();

        assertTrue(result.containsKey("orders"));
        assertTrue(result.containsKey("permissions"));
        assertTrue(result.containsKey("menus"));
        assertTrue(result.containsKey("roleMenus"));
        assertTrue(result.containsKey("attendanceRecords"));
        assertEquals(1, ((List<?>) result.get("orders")).size());

        Map<?, ?> user = (Map<?, ?>) ((List<?>) result.get("sysUsers")).get(0);
        assertEquals("admin", user.get("username"));
        assertFalse(user.containsKey("passwordHash"));
        assertFalse(user.containsKey("sessionToken"));
    }
}
