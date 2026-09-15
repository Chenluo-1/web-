package cn.ecom.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售趋势：日/周/月维度，含环比（与前一周期比）与同比（去年同期，无数据为 null）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendVo {
    private String dimension;

    private List<TrendPoint> points;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        /** 日期或周期标签（如 2026-09-15 / 2026-09 第3周 / 2026-09） */
        private String label;
        private BigDecimal gmv;
        private Long orderCount;
        /** 环比（前一周期，%） */
        private BigDecimal mom;
        /** 同比（去年同期，%；不足一年数据为 null → 前端 "-") */
        private BigDecimal yoy;
    }
}
