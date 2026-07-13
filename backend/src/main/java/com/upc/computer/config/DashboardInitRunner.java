package com.upc.computer.config;

import com.upc.computer.service.MesDashboardSeedService;
import com.upc.computer.task.DispatchProgressRepairTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 应用启动后初始化生产大屏模拟数据
 */
@Component
public class DashboardInitRunner {

    @Autowired
    private MesDashboardSeedService seedService;
    @Autowired
    private DispatchProgressRepairTask dispatchProgressRepairTask;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            seedService.seedIfEmpty();
            dispatchProgressRepairTask.repairFromWorkReports();
        } catch (Exception ignored) {
            // 表未创建时不阻断启动，首次访问接口时会再次尝试
        }
    }
}
