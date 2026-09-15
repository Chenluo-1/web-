package cn.ecom.service.impl;

import cn.ecom.domain.pojo.Category;
import cn.ecom.domain.pojo.Product;
import cn.ecom.domain.pojo.ProductSalesDaily;
import cn.ecom.domain.pojo.StatDaily;
import cn.ecom.domain.vo.DashboardVos;
import cn.ecom.domain.vo.RealtimeVo;
import cn.ecom.domain.vo.TrendVo;
import cn.ecom.mapper.BehaviorMapper;
import cn.ecom.mapper.CategoryMapper;
import cn.ecom.mapper.OrderItemMapper;
import cn.ecom.mapper.OrderMapper;
import cn.ecom.mapper.ProductMapper;
import cn.ecom.mapper.ProductSalesDailyMapper;
import cn.ecom.mapper.StatDailyMapper;
import cn.ecom.service.DashboardService;
import cn.ecom.utils.RedisCache;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 数据看板服务：
 * - 实时指标：Redis 缓存 5 分钟（GMV 支付成功口径，不含未支付/已退款）
 * - 趋势/排行/占比：读 T+1 汇总表（ec_stat_daily / ec_product_sales_daily），不实时扫大表
 * - 同比数据不足时返回 null（前端显示 "-"）
 */
@Slf4j
@Service("dashboardService")
public class DashboardServiceImpl implements DashboardService {

    private static final String CACHE_KEY_REALTIME = "dashboard:realtime";

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final BehaviorMapper behaviorMapper;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final StatDailyMapper statDailyMapper;
    private final ProductSalesDailyMapper productSalesDailyMapper;
    private final RedisCache redisCache;

    public DashboardServiceImpl(OrderMapper orderMapper, OrderItemMapper orderItemMapper, BehaviorMapper behaviorMapper,
                                ProductMapper productMapper, CategoryMapper categoryMapper, StatDailyMapper statDailyMapper,
                                ProductSalesDailyMapper productSalesDailyMapper, RedisCache redisCache) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.behaviorMapper = behaviorMapper;
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.statDailyMapper = statDailyMapper;
        this.productSalesDailyMapper = productSalesDailyMapper;
        this.redisCache = redisCache;
    }

    // ---------- 实时指标 ----------

    @Override
    public RealtimeVo getRealtime() {
        Object cached = redisCache.getCacheObject(CACHE_KEY_REALTIME);
        if (cached instanceof RealtimeVo) {
            return (RealtimeVo) cached;
        }
        LocalDate today = LocalDate.now();
        Map<String, Object> todaySummary = orderMapper.selectPaidSummary(toDate(today), toDate(today.plusDays(1)));
        Map<String, Object> yestSummary = orderMapper.selectPaidSummary(toDate(today.minusDays(1)), toDate(today));

        BigDecimal gmv = toDecimal(todaySummary.get("gmv"));
        long paidCount = toLong(todaySummary.get("cnt"));
        long visitors = nullSafe(behaviorMapper.countDistinctUser(toDate(today), toDate(today.plusDays(1))));

        BigDecimal avgAmount = paidCount == 0 ? BigDecimal.ZERO
                : gmv.divide(BigDecimal.valueOf(paidCount), 2, RoundingMode.HALF_UP);
        BigDecimal conversion = visitors == 0 ? null
                : BigDecimal.valueOf(paidCount).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(visitors), 1, RoundingMode.HALF_UP);

        BigDecimal yestGmv = toDecimal(yestSummary.get("gmv"));
        long yestPaid = toLong(yestSummary.get("cnt"));
        long yestVisitors = nullSafe(behaviorMapper.countDistinctUser(toDate(today.minusDays(1)), toDate(today)));

        RealtimeVo vo = new RealtimeVo();
        vo.setGmv(gmv);
        vo.setPaidCount(paidCount);
        vo.setAvgAmount(avgAmount);
        vo.setVisitorCount(visitors);
        vo.setConversion(conversion);
        vo.setGmvMoM(percentChange(gmv, yestGmv));
        vo.setPaidCountMoM(percentChange(BigDecimal.valueOf(paidCount), BigDecimal.valueOf(yestPaid)));
        vo.setVisitorMoM(percentChange(BigDecimal.valueOf(visitors), BigDecimal.valueOf(yestVisitors)));
        redisCache.setCacheObject(CACHE_KEY_REALTIME, vo, 300, TimeUnit.SECONDS);
        return vo;
    }

    // ---------- 销售趋势 ----------

    @Override
    public TrendVo getSalesTrend(String dimension) {
        String dim = dimension == null || dimension.isEmpty() ? "day" : dimension;
        LocalDate today = LocalDate.now();
        // 多拉一年用于同比查找（数据不足时同比为 null）
        Date from = toDate(today.minusDays("month".equals(dim) ? 365 + 400 : 200));
        List<StatDaily> rows = statDailyMapper.selectList(new LambdaQueryWrapper<StatDaily>()
                .ge(StatDaily::getStatDate, from).orderByAsc(StatDaily::getStatDate));

        Map<LocalDate, BigDecimal> dayGmv = new HashMap<>();
        Map<LocalDate, Long> dayCnt = new HashMap<>();
        Map<LocalDate, BigDecimal> weekGmv = new HashMap<>();
        Map<LocalDate, Long> weekCnt = new HashMap<>();
        Map<YearMonth, BigDecimal> monthGmv = new HashMap<>();
        Map<YearMonth, Long> monthCnt = new HashMap<>();
        for (StatDaily row : rows) {
            LocalDate d = toLocalDate(row.getStatDate());
            BigDecimal gmv = row.getGmv() == null ? BigDecimal.ZERO : row.getGmv();
            long cnt = row.getPaidCount() == null ? 0 : row.getPaidCount();
            dayGmv.merge(d, gmv, BigDecimal::add);
            dayCnt.merge(d, cnt, Long::sum);
            LocalDate weekKey = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            weekGmv.merge(weekKey, gmv, BigDecimal::add);
            weekCnt.merge(weekKey, cnt, Long::sum);
            YearMonth monthKey = YearMonth.from(d);
            monthGmv.merge(monthKey, gmv, BigDecimal::add);
            monthCnt.merge(monthKey, cnt, Long::sum);
        }

        List<TrendVo.TrendPoint> points = new ArrayList<>();
        if ("week".equals(dim)) {
            for (int i = 11; i >= 0; i--) {
                LocalDate key = today.minusWeeks(i).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                points.add(buildPoint(key.toString(), weekGmv.get(key), weekCnt.get(key),
                        weekGmv.get(key.minusWeeks(1)), weekGmv.get(key.minusYears(1))));
            }
        } else if ("month".equals(dim)) {
            for (int i = 11; i >= 0; i--) {
                YearMonth key = YearMonth.from(today).minusMonths(i);
                points.add(buildPoint(key.toString(), monthGmv.get(key), monthCnt.get(key),
                        monthGmv.get(key.minusMonths(1)), monthGmv.get(key.minusYears(1))));
            }
        } else {
            for (int i = 29; i >= 0; i--) {
                LocalDate key = today.minusDays(i);
                points.add(buildPoint(key.toString(), dayGmv.get(key), dayCnt.get(key),
                        dayGmv.get(key.minusDays(1)), dayGmv.get(key.minusYears(1))));
            }
        }
        return new TrendVo(dim, points);
    }

    private TrendVo.TrendPoint buildPoint(String label, BigDecimal gmv, Long cnt,
                                          BigDecimal prev, BigDecimal prevYear) {
        TrendVo.TrendPoint point = new TrendVo.TrendPoint();
        point.setLabel(label);
        point.setGmv(gmv == null ? BigDecimal.ZERO : gmv);
        point.setOrderCount(cnt == null ? 0 : cnt);
        point.setMom(percentChange(point.getGmv(), prev));
        // 同比：去年同期无数据（或为 0）→ null，前端显示 "-"
        point.setYoy(prevYear == null ? null : percentChange(point.getGmv(), prevYear));
        return point;
    }

    // ---------- 排行 / 类目占比 / 漏斗 ----------

    @Override
    public DashboardVos.RankResult getProductRank() {
        LocalDate today = LocalDate.now();
        Date from = toDate(today.minusDays(29));
        Date to = toDate(today);
        List<DashboardVos.RankItem> hot = productSalesDailyMapper.selectHotTop(from, to).stream()
                .map(row -> new DashboardVos.RankItem(
                        toLongObj(row.get("productId")), String.valueOf(row.get("title")),
                        (int) toLong(row.get("qty")), toDecimal(row.get("amount"))))
                .collect(Collectors.toList());
        List<DashboardVos.RankItem> cold = productSalesDailyMapper.selectColdTop(from, to).stream()
                .map(row -> new DashboardVos.RankItem(
                        toLongObj(row.get("productId")), String.valueOf(row.get("title")),
                        (int) toLong(row.get("qty")), BigDecimal.ZERO))
                .collect(Collectors.toList());
        return new DashboardVos.RankResult(hot, cold, "近 30 天");
    }

    @Override
    public List<DashboardVos.CategoryShare> getCategoryAnalysis() {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> amounts = productSalesDailyMapper.selectProductAmount(
                toDate(today.minusDays(29)), toDate(today));
        if (amounts.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> productIds = amounts.stream().map(m -> toLongObj(m.get("productId"))).collect(Collectors.toList());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, Category> categoryMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        Map<String, BigDecimal> share = new HashMap<>();
        for (Map<String, Object> row : amounts) {
            Product product = productMap.get(toLongObj(row.get("productId")));
            if (product == null) {
                continue;
            }
            // 向上归到一级类目
            Category category = categoryMap.get(product.getCategoryId());
            while (category != null && category.getParentId() != null && category.getParentId() != 0L) {
                Category parent = categoryMap.get(category.getParentId());
                if (parent == null) {
                    break;
                }
                category = parent;
            }
            String name = category == null ? "未分类" : category.getName();
            share.merge(name, toDecimal(row.get("amount")), BigDecimal::add);
        }
        return share.entrySet().stream()
                .map(e -> new DashboardVos.CategoryShare(e.getKey(), e.getValue()))
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .collect(Collectors.toList());
    }

    @Override
    public DashboardVos.Funnel getOrderFunnel() {
        LocalDate today = LocalDate.now();
        Date from = toDate(today.minusDays(6));
        Date to = toDate(today.plusDays(1));
        long expose = nullSafe(behaviorMapper.countEvent("expose", from, to));
        long click = nullSafe(behaviorMapper.countEvent("click", from, to));
        long cart = nullSafe(behaviorMapper.countEvent("cart", from, to));
        long order = toLong(orderMapper.selectPaidSummary(from, to).get("cnt"));
        return new DashboardVos.Funnel(expose, click, cart, order, "近 7 天");
    }

    // ---------- T+1 汇总 ----------

    @Override
    public void aggregateDate(LocalDate date) {
        Date from = toDate(date);
        Date to = toDate(date.plusDays(1));
        Map<String, Object> paid = orderMapper.selectPaidSummary(from, to);
        BigDecimal gmv = toDecimal(paid.get("gmv"));
        long paidCount = toLong(paid.get("cnt"));
        int orderCount = (int) nullSafe(orderMapper.countCreated(from, to));
        int refundCount = (int) nullSafe(orderMapper.countRefunded(from, to));
        int visitors = (int) nullSafe(behaviorMapper.countDistinctUser(from, to));
        long pv = nullSafe(behaviorMapper.countEvent("expose", from, to))
                + nullSafe(behaviorMapper.countEvent("click", from, to));
        int cartCount = (int) nullSafe(behaviorMapper.countEvent("cart", from, to));

        statDailyMapper.deleteByDate(from);
        statDailyMapper.insert(new StatDaily()
                .setStatDate(from)
                .setGmv(gmv)
                .setOrderCount(orderCount)
                .setPaidCount((int) paidCount)
                .setRefundCount(refundCount)
                .setVisitorCount(visitors)
                .setPv((int) pv)
                .setCartCount(cartCount));

        productSalesDailyMapper.deleteByDate(from);
        for (Map<String, Object> row : orderItemMapper.selectPaidItemSummary(from, to)) {
            productSalesDailyMapper.insert(new ProductSalesDaily()
                    .setStatDate(from)
                    .setProductId(toLongObj(row.get("productId")))
                    .setSkuId(toLongObj(row.get("skuId")))
                    .setSalesQty((int) toLong(row.get("qty")))
                    .setSalesAmount(toDecimal(row.get("amount"))));
        }
    }

    @Override
    public int aggregateStat(int days) {
        int n = Math.max(days, 1);
        int rows = 0;
        LocalDate today = LocalDate.now();
        for (int i = n - 1; i >= 0; i--) {
            aggregateDate(today.minusDays(i));
            rows++;
        }
        return rows;
    }

    // ---------- 工具 ----------

    /** 环比：前一值为 0 或为空 → null（前端 "-"） */
    private BigDecimal percentChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        if (value instanceof Date) {
            return ((Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.parse(value.toString().substring(0, 10));
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString());
    }

    private long toLong(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private Long toLongObj(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private long nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}
