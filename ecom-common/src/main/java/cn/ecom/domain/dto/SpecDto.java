package cn.ecom.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 规格维度 DTO：如 {specName: "颜色", values: ["红","蓝"]}
 */
@Data
public class SpecDto {
    private String specName;
    private List<String> values;
}
