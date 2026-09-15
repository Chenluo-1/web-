package cn.ecom.service;

import cn.ecom.domain.pojo.SalesForecast;
import cn.ecom.domain.vo.ForecastVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface SalesForecastService extends IService<SalesForecast> {

    /** 全量重跑预测（加权移动平均 + 系数），返回写入行数 */
    int generateForecasts();

    /** 有预测的 SKU 列表 */
    List<Map<String, Object>> listForecastSkus();

    /** 单个 SKU 的图表数据：近 30 天实际 + 未来 28 天预测 */
    ForecastVo getForecastData(Long skuId);
}
