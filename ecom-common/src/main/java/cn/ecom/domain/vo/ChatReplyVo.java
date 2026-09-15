package cn.ecom.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 客服应答 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatReplyVo {
    private String reply;
    /** 匹配置信度(0-1) */
    private BigDecimal confidence;
    /** 是否转人工 */
    private Boolean transfer;
    /** 命中的知识（可空） */
    private String matchedQuestion;
}
