package cn.ecom.service;

import cn.ecom.domain.vo.DashboardVos;
import cn.ecom.domain.vo.RealtimeVo;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {

    /** 今日实时指标（含环比；Redis 缓存 5 分钟） */
    RealtimeVo getRealtime();

    /** 商品排行（近 30 天热销/滞销 TOP10） */
    DashboardVos.RankResult getProductRank();

    /** 类目销售占比（近 30 天，向上归到一级类目） */
    List<DashboardVos.CategoryShare> getCategoryAnalysis();

    /** 重算指定日期（T+1 任务与手动刷新共用） */
    void aggregateDate(LocalDate date);

    /** 手动重算最近 N 天（含今天），返回写入的汇总行数 */
    int aggregateStat(int days);
}
