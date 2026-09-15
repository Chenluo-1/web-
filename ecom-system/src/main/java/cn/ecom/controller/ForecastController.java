package cn.ecom.controller;

import cn.ecom.annotation.OperLog;
import cn.ecom.domain.vo.ForecastVo;
import cn.ecom.service.SalesForecastService;
import cn.ecom.utils.ResponseResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 销量预测：SKU 列表 / 图表数据 / 手动生成
 */
@RestController
@RequestMapping("/stock/forecast")
public class ForecastController {

    private final SalesForecastService salesForecastService;

    public ForecastController(SalesForecastService salesForecastService) {
        this.salesForecastService = salesForecastService;
    }

    /** 有预测数据的 SKU 列表 */
    @GetMapping("/skus")
    @PreAuthorize("@ps.hasPermission('stock:forecast')")
    public ResponseResult<List<Map<String, Object>>> skus() {
        return ResponseResult.okResult(salesForecastService.listForecastSkus());
    }

    /** 单个 SKU：近 30 天实际 + 未来 28 天预测 */
    @GetMapping("/{skuId}")
    @PreAuthorize("@ps.hasPermission('stock:forecast')")
    public ResponseResult<ForecastVo> data(@PathVariable Long skuId) {
        return ResponseResult.okResult(salesForecastService.getForecastData(skuId));
    }

    /** 手动重跑预测（演示用；每日 2:00 也会自动跑） */
    @OperLog(module = "销量预测", type = "手动生成")
    @PreAuthorize("@ps.hasPermission('stock:forecast:generate')")
    @PostMapping("/generate")
    public ResponseResult<Integer> generate() {
        return ResponseResult.okResult(salesForecastService.generateForecasts());
    }
}
