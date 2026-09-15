package cn.ecom.task;

import cn.ecom.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * T+1 汇总任务：每天 1:30 重算昨天（GMV/订单/商品销量），看板只读汇总表不扫大表
 */
@Slf4j
@Component
public class StatDailyTask {

    private final DashboardService dashboardService;

    public StatDailyTask(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Scheduled(cron = "0 30 1 * * ?")
    public void aggregateYesterday() {
        try {
            dashboardService.aggregateDate(LocalDate.now().minusDays(1));
            log.info("T+1 汇总完成：{}", LocalDate.now().minusDays(1));
        } catch (Exception e) {
            log.error("T+1 汇总异常", e);
        }
    }
}
