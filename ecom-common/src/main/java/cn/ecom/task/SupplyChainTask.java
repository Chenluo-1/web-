package cn.ecom.task;

import cn.ecom.service.SalesForecastService;
import cn.ecom.service.StockAlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 智能供应链定时任务：库存预警（6 小时）+ 销量预测（每天凌晨 2:00）
 */
@Slf4j
@Component
public class SupplyChainTask {

    private final StockAlertService stockAlertService;
    private final SalesForecastService salesForecastService;

    public SupplyChainTask(StockAlertService stockAlertService, SalesForecastService salesForecastService) {
        this.stockAlertService = stockAlertService;
        this.salesForecastService = salesForecastService;
    }

    /** 每 6 小时扫描一次库存预警 */
    @Scheduled(cron = "0 0 */6 * * ?")
    public void scanStockAlerts() {
        try {
            int created = stockAlertService.scanAndCreateAlerts();
            log.info("库存预警扫描完成，新生成 {} 条", created);
        } catch (Exception e) {
            log.error("库存预警扫描异常", e);
        }
    }

    /** 每天凌晨 2:00 生成未来 28 天销量预测 */
    @Scheduled(cron = "0 0 2 * * ?")
    public void generateForecasts() {
        try {
            int rows = salesForecastService.generateForecasts();
            log.info("销量预测生成完成，写入 {} 行", rows);
        } catch (Exception e) {
            log.error("销量预测生成异常", e);
        }
    }
}
