package cn.ecom.service.impl;

import cn.ecom.domain.pojo.Category;
import cn.ecom.domain.pojo.Product;
import cn.ecom.domain.pojo.SearchLog;
import cn.ecom.domain.pojo.SearchSynonym;
import cn.ecom.domain.vo.SearchResultVo;
import cn.ecom.handler.exception.BusinessException;
import cn.ecom.mapper.CategoryMapper;
import cn.ecom.mapper.ProductMapper;
import cn.ecom.mapper.SearchLogMapper;
import cn.ecom.mapper.SearchSynonymMapper;
import cn.ecom.service.SearchService;
import cn.ecom.utils.AppHttpCodeEnum;
import cn.ecom.utils.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 智能搜索（规则版）：
 * 1) 同义词扩展（ec_search_synonym，双向命中）
 * 2) 标题多词 OR 匹配（上架商品）
 * 3) 零结果 → 字符集 Dice 容错模糊匹配（错别字/漏字可命中）
 * 4) 仍无结果 → 相近类目商品推荐（类目名相似），兜底热销商品
 * 5) 每次搜索记录 ec_search_log（热词榜/联想数据源）
 */
@Service("searchService")
public class SearchServiceImpl implements SearchService {

    /** 容错模糊命中阈值 */
    private static final double FUZZY_THRESHOLD = 0.5;
    /** 类目名相似阈值 */
    private static final double CATEGORY_THRESHOLD = 0.34;

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final SearchSynonymMapper synonymMapper;
    private final SearchLogMapper searchLogMapper;

    public SearchServiceImpl(ProductMapper productMapper, CategoryMapper categoryMapper,
                             SearchSynonymMapper synonymMapper, SearchLogMapper searchLogMapper) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
        this.synonymMapper = synonymMapper;
        this.searchLogMapper = searchLogMapper;
    }

    @Override
    public SearchResultVo search(String keyword, Integer pageNum, Integer pageSize) {
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(AppHttpCodeEnum.PARAMETER_ERROR, "请输入搜索关键词");
        }
        String kw = keyword.trim();
        List<String> terms = expandSynonyms(kw);

        // 1) 标题多词 OR 精确匹配
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        wrapper.and(q -> {
            for (int i = 0; i < terms.size(); i++) {
                if (i == 0) {
                    q.like(Product::getTitle, terms.get(i));
                } else {
                    q.or().like(Product::getTitle, terms.get(i));
                }
            }
        });
        wrapper.orderByDesc(Product::getSales);
        Page<Product> page = productMapper.selectPage(
                new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize), wrapper);

        SearchResultVo vo = new SearchResultVo();
        vo.setFuzzy(false);
        vo.setRecommend(new ArrayList<>());

        if (page.getTotal() > 0) {
            vo.setTotal(page.getTotal());
            vo.setRecords(page.getRecords());
            if (terms.size() > 1) {
                vo.setHint("已按同义词扩展搜索：" + String.join(" / ", terms));
            }
        } else {
            // 2) 容错模糊（错别字/漏字）
            List<Product> fuzzy = fuzzyMatch(kw);
            if (!fuzzy.isEmpty()) {
                vo.setFuzzy(true);
                vo.setTotal((long) fuzzy.size());
                vo.setRecords(fuzzy);
                vo.setHint("未找到「" + kw + "」的精确结果，已为您展示相近的商品");
            } else {
                // 3) 无结果 → 推荐相近类目商品，兜底热销
                vo.setTotal(0L);
                vo.setRecords(new ArrayList<>());
                List<Product> recommend = recommendByCategory(kw);
                vo.setRecommend(recommend);
                vo.setHint(recommend.isEmpty() ? "没有找到相关商品" : "没有找到相关商品，为您推荐同类好物");
            }
        }

        logSearch(kw, vo.getTotal() == null ? 0 : vo.getTotal().intValue());
        return vo;
    }

    /** 同义词扩展：命中组内任意词 → 整组展开 */
    private List<String> expandSynonyms(String keyword) {
        Set<String> terms = new LinkedHashSet<>();
        terms.add(keyword);
        for (SearchSynonym synonym : synonymMapper.selectList(null)) {
            String word = synonym.getWord();
            List<String> group = new ArrayList<>();
            group.add(word);
            if (StringUtils.hasText(synonym.getSynonyms())) {
                group.addAll(Arrays.stream(synonym.getSynonyms().split("[,，]"))
                        .map(String::trim).filter(StringUtils::hasText).collect(Collectors.toList()));
            }
            boolean hit = group.stream().anyMatch(g -> keyword.contains(g) || g.contains(keyword));
            if (hit) {
                terms.addAll(group);
            }
        }
        return terms.stream().filter(StringUtils::hasText).limit(8).collect(Collectors.toList());
    }

    /** 容错模糊：字符集 Dice 相似度 ≥ 阈值（对错别字/漏字有效） */
    private List<Product> fuzzyMatch(String keyword) {
        Set<Character> kwSet = charSet(keyword);
        if (kwSet.isEmpty()) {
            return new ArrayList<>();
        }
        List<Product> onShelf = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1));
        return onShelf.stream()
                .map(p -> new Object[]{p, dice(kwSet, charSet(p.getTitle()))})
                .filter(t -> ((double) t[1]) >= FUZZY_THRESHOLD)
                .sorted((a, b) -> Double.compare((double) b[1], (double) a[1]))
                .limit(10)
                .map(t -> (Product) t[0])
                .collect(Collectors.toList());
    }

    /** 相近类目推荐 → 兜底热销 */
    private List<Product> recommendByCategory(String keyword) {
        Set<Character> kwSet = charSet(keyword);
        List<Category> categories = categoryMapper.selectList(null);
        List<Long> hitCategoryIds = categories.stream()
                .filter(c -> dice(kwSet, charSet(c.getName())) >= CATEGORY_THRESHOLD)
                .map(Category::getId)
                .collect(Collectors.toList());
        if (!hitCategoryIds.isEmpty()) {
            // 含子孙类目
            Set<Long> all = new HashSet<>(hitCategoryIds);
            boolean changed = true;
            while (changed) {
                changed = false;
                for (Category category : categories) {
                    if (all.contains(category.getParentId()) && all.add(category.getId())) {
                        changed = true;
                    }
                }
            }
            List<Product> list = productMapper.selectList(new LambdaQueryWrapper<Product>()
                    .eq(Product::getStatus, 1)
                    .in(Product::getCategoryId, all)
                    .orderByDesc(Product::getSales)
                    .last("LIMIT 8"));
            if (!list.isEmpty()) {
                return list;
            }
        }
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getSales)
                .last("LIMIT 8"));
    }

    private Set<Character> charSet(String text) {
        if (text == null) {
            return new HashSet<>();
        }
        Set<Character> set = new HashSet<>();
        for (char c : text.replaceAll("[^\\p{IsHan}\\p{Alnum}]", "").toLowerCase().toCharArray()) {
            set.add(c);
        }
        return set;
    }

    private double dice(Set<Character> a, Set<Character> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        long common = a.stream().filter(b::contains).count();
        return 2.0 * common / (a.size() + b.size());
    }

    private void logSearch(String keyword, int resultCount) {
        Long userId = null;
        try {
            userId = SecurityUtils.getUserId();
        } catch (Exception ignored) {
            // 游客
        }
        searchLogMapper.insert(new SearchLog()
                .setUserId(userId)
                .setKeyword(keyword)
                .setResultCount(resultCount));
    }

    @Override
    public List<Map<String, Object>> hotWords(int limit) {
        return searchLogMapper.selectHotWords(Math.max(1, Math.min(limit, 20)));
    }

    @Override
    public List<String> suggest(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return new ArrayList<>();
        }
        return searchLogMapper.selectSuggest(prefix.trim());
    }
}
