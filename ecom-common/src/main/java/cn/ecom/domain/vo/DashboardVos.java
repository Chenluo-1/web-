package cn.ecom.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 看板配套小 VO 集合
 */
public class DashboardVos {

    /** 商品排行项 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankItem {
        private Long productId;
        private String title;
        private Integer qty;
        private BigDecimal amount;
    }

    /** 类目占比项（一级类目汇总） */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryShare {
        private String name;
        private BigDecimal amount;
    }

    /** 订单漏斗 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Funnel {
        private Long expose;
        private Long click;
        private Long cart;
        private Long order;
        /** 统计区间说明 */
        private String range;
    }

    /** 排行榜响应 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankResult {
        private List<RankItem> hot;
        private List<RankItem> cold;
        private String range;
    }
}
