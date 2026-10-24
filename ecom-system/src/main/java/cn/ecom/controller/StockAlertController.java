package cn.ecom.controller;

import cn.ecom.annotation.OperLog;
import cn.ecom.domain.pojo.StockAlert;
import cn.ecom.service.StockAlertService;
import cn.ecom.utils.ResponseResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存预警：列表 / 标记已处理 / 手动扫描 / 未处理计数
 */
@RestController
@RequestMapping("/stock/alert")
public class StockAlertController {

    private final StockAlertService stockAlertService;

    public StockAlertController(StockAlertService stockAlertService) {
        this.stockAlertService = stockAlertService;
    }

    /** 预警分页（按类型/状态筛选） */
    @GetMapping
    @PreAuthorize("@ss.hasPermission('stock:alert')")
    public ResponseResult<Page<StockAlert>> selectByPage(Integer pageNum, Integer pageSize,
                                                         Integer alertType, Integer status) {
        return ResponseResult.okResult(stockAlertService.selectByPage(pageNum, pageSize, alertType, status));
    }

    /** 未处理预警数（首页卡片；无权限用户会收到 403，前端静默处理） */
    @GetMapping("/unhandledCount")
    public ResponseResult<Long> unhandledCount() {
        return ResponseResult.okResult(stockAlertService.unhandledCount());
    }

    /** 手动执行一次扫描（应急/演示用） */
    @OperLog(module = "库存预警", type = "手动扫描")
    @PreAuthorize("@ss.hasPermission('stock:alert:scan')")
    @PostMapping("/scan")
    public ResponseResult<Integer> scan() {
        return ResponseResult.okResult(stockAlertService.scanAndCreateAlerts());
    }

    /** 标记已处理 */
    @OperLog(module = "库存预警", type = "标记已处理")
    @PreAuthorize("@ss.hasPermission('stock:alert:handle')")
    @PutMapping("/{id}/handle")
    public ResponseResult<Void> handle(@PathVariable Long id) {
        stockAlertService.handle(id);
        return ResponseResult.okResult();
    }
}
