package cn.ecom.service.impl;

import cn.ecom.domain.pojo.Coupon;
import cn.ecom.domain.pojo.Product;
import cn.ecom.domain.pojo.SalesForecast;
import cn.ecom.domain.pojo.Sku;
import cn.ecom.domain.vo.ForecastVo;
import cn.ecom.domain.vo.PointVo;
import cn.ecom.mapper.CouponMapper;
import cn.ecom.mapper.OrderItemMapper;
import cn.ecom.mapper.ProductMapper;
import cn.ecom.mapper.SalesForecastMapper;
import cn.ecom.mapper.SkuMapper;
import cn.ecom.service.SalesForecastService;
import cn.ecom.service.SysConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 销量预测服务（规则版，非机器学习）：
 * 加权移动平均（近 7 天 0.5 / 8-30 天 0.3 / 31-90 天 0.2，权重走 ec_sys_config）
 * × 季节系数（周末 1.1）× 促销系数（预测日在任一有效券期内 1.2），输出未来 28 天逐日预测。
 * 新品无历史销量：用同类目其他 SKU 的日均销量兜底，仍无则默认 1 件/天。
 */
@Slf4j
@Service("salesForecastService")
public class SalesForecastServiceImpl extends ServiceImpl<SalesForecastMapper, SalesForecast>
        implements SalesForecastService {

    private static final int HISTORY_DAYS = 90;
    private static final int FORECAST_DAYS = 28;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final CouponMapper couponMapper;
    private final OrderItemMapper orderItemMapper;
    private final SysConfigService sysConfigService;

    public SalesForecastServiceImpl(SkuMapper skuMapper, ProductMapper productMapper, CouponMapper couponMapper,
                                    OrderItemMapper orderItemMapper, SysConfigService sysConfigService) {
        this.skuMapper = skuMapper;
        this.productMapper = productMapper;
        this.couponMapper = couponMapper;
        this.orderItemMapper = orderItemMapper;
        this.sysConfigService = sysConfigService;
    }

    @Override
    @Transactional
    public int generateForecasts() {
        double wRecent = parseDouble(sysConfigService.getString("forecast_weight_recent", "0.5"), 0.5);
        double wMid = parseDouble(sysConfigService.getString("forecast_weight_mid", "0.3"), 0.3);
        double wOld = parseDouble(sysConfigService.getString("forecast_weight_old", "0.2"), 0.2);

        LocalDate today = LocalDate.now();
        Date since90 = toDate(today.minusDays(HISTORY_DAYS));
        // 促销窗口只查一次（预测日在任一有效券期内 → 促销系数 1.2）
        List<Coupon> coupons = couponMapper.selectList(new LambdaQueryWrapper<Coupon>().eq(Coupon::getStatus, 0));

        getBaseMapper().deleteAll();
        List<Sku> skus = skuMapper.selectList(null);
        int rows = 0;
        for (Sku sku : skus) {
            double base = baselineDailySales(sku, since90, wRecent, wMid, wOld);
            for (int d = 1; d <= FORECAST_DAYS; d++) {
                LocalDate day = today.plusDays(d);
                double coeff = weekdayCoeff(day) * promoCoeff(day, coupons);
                int qty = (int) Math.round(base * coeff);
                getBaseMapper().insert(new SalesForecast()
                        .setSkuId(sku.getId())
                        .setForecastDate(toDate(day))
                        .setForecastQty(Math.max(qty, 0)));
                rows++;
            }
        }
        return rows;
    }

    /** 基线日销量：加权移动平均；新品走同类目兜底 */
    private double baselineDailySales(Sku sku, Date since90, double wRecent, double wMid, double wOld) {
        List<Map<String, Object>> daily = orderItemMapper.selectDailySales(sku.getId(), since90);
        if (daily == null || daily.isEmpty()) {
            return categoryFallback(sku, since90);
        }
        Map<LocalDate, Integer> qtyByDate = new HashMap<>();
        for (Map<String, Object> row : daily) {
            LocalDate date = toLocalDate(row.get("sales_date"));
            if (date != null) {
                qtyByDate.put(date, ((Number) row.get("qty")).intValue());
            }
        }
        LocalDate today = LocalDate.now();
        LocalDate firstSale = qtyByDate.keySet().stream().min(LocalDate::compareTo).orElse(today);
        long daysSinceFirst = ChronoUnit.DAYS.between(firstSale, today) + 1;

        double recent = windowAvg(qtyByDate, today, 1, 7, daysSinceFirst);
        double mid = windowAvg(qtyByDate, today, 8, 30, daysSinceFirst);
        double old = windowAvg(qtyByDate, today, 31, 90, daysSinceFirst);
        return recent * wRecent + mid * wMid + old * wOld;
    }

    /** 窗口日均：offset=Ago..toAgo 天前；分母按"上架至今"封顶，避免新品被历史拉低 */
    private double windowAvg(Map<LocalDate, Integer> qtyByDate, LocalDate today, int fromAgo, int toAgo, long daysSinceFirst) {
        int windowDays = toAgo - fromAgo + 1;
        long effectiveDays = Math.min(windowDays, Math.max(daysSinceFirst - (fromAgo - 1), 0));
        if (effectiveDays <= 0) {
            return 0;
        }
        int total = 0;
        for (int offset = fromAgo; offset <= toAgo; offset++) {
            total += qtyByDate.getOrDefault(today.minusDays(offset), 0);
        }
        return (double) total / effectiveDays;
    }

    /** 新品兜底：同类目其他 SKU 日均销量；仍无则 1 件/天 */
    private double categoryFallback(Sku sku, Date since90) {
        Product product = productMapper.selectById(sku.getProductId());
        if (product != null) {
            List<Map<String, Object>> cat = orderItemMapper.selectCategoryDailySales(
                    product.getCategoryId(), sku.getId(), since90);
            if (cat != null && !cat.isEmpty()) {
                double total = 0;
                int days = 0;
                for (Map<String, Object> row : cat) {
                    total += ((Number) row.get("qty")).doubleValue();
                    days += ((Number) row.get("days")).intValue();
                }
                if (days > 0) {
                    return total / days;
                }
            }
        }
        return 1.0;
    }

    /** 季节系数（简化）：周末 1.1，工作日 1.0 */
    private double weekdayCoeff(LocalDate date) {
        int dow = date.getDayOfWeek().getValue();
        return (dow == 6 || dow == 7) ? 1.1 : 1.0;
    }

    /** 促销系数：预测日落在任一有效券期内 → 1.2 */
    private double promoCoeff(LocalDate date, List<Coupon> coupons) {
        Date day = toDate(date);
        for (Coupon coupon : coupons) {
            if (coupon.getStartTime() != null && coupon.getEndTime() != null
                    && !day.before(coupon.getStartTime()) && !day.after(coupon.getEndTime())) {
                return 1.2;
            }
        }
        return 1.0;
    }

    @Override
    public List<Map<String, Object>> listForecastSkus() {
        return getBaseMapper().selectForecastSkuList();
    }

    @Override
    public ForecastVo getForecastData(Long skuId) {
        Sku sku = skuMapper.selectById(skuId);
        Product product = sku == null ? null : productMapper.selectById(sku.getProductId());
        // 近 30 天实际（补零成连续序列）
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> daily = orderItemMapper.selectDailySales(skuId, toDate(today.minusDays(29)));
        Map<LocalDate, Integer> qtyByDate = new HashMap<>();
        if (daily != null) {
            for (Map<String, Object> row : daily) {
                LocalDate date = toLocalDate(row.get("sales_date"));
                if (date != null) {
                    qtyByDate.put(date, ((Number) row.get("qty")).intValue());
                }
            }
        }
        List<PointVo> history = new ArrayList<>();
        for (int offset = 29; offset >= 0; offset--) {
            LocalDate day = today.minusDays(offset);
            history.add(new PointVo(day.format(DATE_FMT), qtyByDate.getOrDefault(day, 0)));
        }
        // 未来 28 天预测
        List<SalesForecast> forecasts = list(new LambdaQueryWrapper<SalesForecast>()
                .eq(SalesForecast::getSkuId, skuId).orderByAsc(SalesForecast::getForecastDate));
        List<PointVo> forecast = new ArrayList<>();
        for (SalesForecast f : forecasts) {
            forecast.add(new PointVo(toLocalDate(f.getForecastDate()).format(DATE_FMT), f.getForecastQty()));
        }
        ForecastVo vo = new ForecastVo();
        vo.setSkuId(skuId);
        vo.setProductTitle(product == null ? null : product.getTitle());
        vo.setSpecs(sku == null ? null : sku.getSpecs());
        vo.setHistory(history);
        vo.setForecast(forecast);
        vo.setFallback(qtyByDate.isEmpty());
        return vo;
    }

    private double parseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        if (value instanceof Date) {
            return ((Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.parse(value.toString().substring(0, 10));
    }
}
