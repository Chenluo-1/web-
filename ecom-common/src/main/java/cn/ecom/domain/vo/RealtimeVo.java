package cn.ecom.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 今日实时指标（含环比；无对比数据时为 null → 前端显示 "-"）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeVo {
    private BigDecimal gmv;
    private Long paidCount;
    /** 客单价 = GMV / 支付单数（无单为 0） */
    private BigDecimal avgAmount;
    private Long visitorCount;
    /** 转化率 = 支付单数 / 访客数（访客为 0 时为 null） */
    private BigDecimal conversion;

    /** 环比（与昨日对比，百分比数值，如 12.5 表示 +12.5%；无法计算为 null） */
    private BigDecimal gmvMoM;
    private BigDecimal paidCountMoM;
    private BigDecimal visitorMoM;
}
