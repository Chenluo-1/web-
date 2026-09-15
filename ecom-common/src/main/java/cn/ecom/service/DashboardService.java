package cn.ecom.service;

import cn.ecom.domain.vo.DashboardVos;
import cn.ecom.domain.vo.RealtimeVo;
import cn.ecom.domain.vo.TrendVo;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {

    /** 今日实时指标（含环比；Redis 缓存 5 分钟） */
    RealtimeVo getRealtime();

    /** 销售趋势（dimension：day/week/month；基于 T+1 汇总表） */
    TrendVo getSalesTrend(String dimension);

    /** 商品排行（近 30 天热销/滞销 TOP10） */
    DashboardVos.RankResult getProductRank();

    /** 类目销售占比（近 30 天，向上归到一级类目） */
    List<DashboardVos.CategoryShare> getCategoryAnalysis();

    /** 订单漏斗（近 7 天：曝光→点击→加购→成交） */
    DashboardVos.Funnel getOrderFunnel();

    /** 重算指定日期（T+1 任务与手动刷新共用） */
    void aggregateDate(LocalDate date);

    /** 手动重算最近 N 天（含今天），返回写入的汇总行数 */
    int aggregateStat(int days);
}
