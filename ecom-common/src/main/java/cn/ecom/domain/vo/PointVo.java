package cn.ecom.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图表数据点：日期 + 数量
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointVo {
    private String date;
    private Integer qty;
}
