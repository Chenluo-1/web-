package cn.ecom.controller;

import cn.ecom.annotation.OperLog;
import cn.ecom.domain.vo.DashboardVos;
import cn.ecom.domain.vo.RealtimeVo;
import cn.ecom.domain.vo.TrendVo;
import cn.ecom.service.DashboardService;
import cn.ecom.utils.ResponseResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据看板（接口层按角色做数据范围控制）：
 * - 营销数据（实时/趋势/漏斗）：dashboard:market
 * - 商品数据（排行/类目占比）：dashboard:product
 * - 手动重算汇总：dashboard:refresh
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** 今日实时指标（Redis 缓存 5 分钟；GMV 支付成功口径） */
    @GetMapping("/realtime")
    @PreAuthorize("@ss.hasPermission('dashboard:market')")
    public ResponseResult<RealtimeVo> realtime() {
        return ResponseResult.okResult(dashboardService.getRealtime());
    }

    /** 销售趋势（dimension=day/week/month，含环比同比） */
    @GetMapping("/salesTrend")
    @PreAuthorize("@ss.hasPermission('dashboard:market')")
    public ResponseResult<TrendVo> salesTrend(@RequestParam(defaultValue = "day") String dimension) {
        return ResponseResult.okResult(dashboardService.getSalesTrend(dimension));
    }

    /** 商品排行榜（近 30 天热销/滞销 TOP10） */
    @GetMapping("/productRank")
    @PreAuthorize("@ss.hasPermission('dashboard:product')")
    public ResponseResult<DashboardVos.RankResult> productRank() {
        return ResponseResult.okResult(dashboardService.getProductRank());
    }

    /** 类目销售占比（近 30 天，一级类目） */
    @GetMapping("/categoryAnalysis")
    @PreAuthorize("@ss.hasPermission('dashboard:product')")
    public ResponseResult<List<DashboardVos.CategoryShare>> categoryAnalysis() {
        return ResponseResult.okResult(dashboardService.getCategoryAnalysis());
    }

    /** 订单漏斗（近 7 天：曝光→点击→加购→成交） */
    @GetMapping("/orderFunnel")
    @PreAuthorize("@ss.hasPermission('dashboard:market')")
    public ResponseResult<DashboardVos.Funnel> orderFunnel() {
        return ResponseResult.okResult(dashboardService.getOrderFunnel());
    }

    /** 手动重算最近 N 天汇总（演示/应急用；每天 1:30 自动算昨天） */
    @OperLog(module = "数据看板", type = "重算汇总")
    @PreAuthorize("@ss.hasPermission('dashboard:refresh')")
    @PostMapping("/aggregate")
    public ResponseResult<Integer> aggregate(@RequestParam(defaultValue = "7") Integer days) {
        return ResponseResult.okResult(dashboardService.aggregateStat(days));
    }
}
