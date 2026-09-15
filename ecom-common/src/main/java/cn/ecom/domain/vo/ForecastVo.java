package cn.ecom.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 销量预测 VO：SKU 信息 + 近 30 天实际 + 未来 28 天预测
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForecastVo {
    private Long skuId;
    private String productTitle;
    private String specs;
    private List<PointVo> history;
    private List<PointVo> forecast;
    /** 是否使用了兜底值（新品无历史销量） */
    private Boolean fallback;
}
