package cn.ecom.controller;

import cn.ecom.domain.pojo.Logistics;
import cn.ecom.service.LogisticsService;
import cn.ecom.utils.ResponseResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物流轨迹查询（读取时按时间虚拟推进：已揽收→运输中→派送中→已签收）
 */
@RestController
@RequestMapping("/logistics")
public class LogisticsController {

    private final LogisticsService logisticsService;

    public LogisticsController(LogisticsService logisticsService) {
        this.logisticsService = logisticsService;
    }

    /** 按订单号查询物流轨迹 */
    @GetMapping("/{orderNo}")
    public ResponseResult<Logistics> trace(@PathVariable String orderNo) {
        return ResponseResult.okResult(logisticsService.getTrace(orderNo));
    }
}
