package cn.ecom.domain.vo;

import cn.ecom.domain.pojo.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索结果 VO：精确结果 + 容错标记/提示 + 无结果推荐
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultVo {
    private Long total;
    private List<Product> records;
    /** 是否走了容错模糊匹配 */
    private Boolean fuzzy;
    /** 给用户的提示（纠错/推荐说明） */
    private String hint;
    /** 无结果时的推荐商品（相近类目或热销兜底） */
    private List<Product> recommend;
}
