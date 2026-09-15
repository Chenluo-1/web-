package cn.ecom.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 待补充问题 VO（转人工且当前仍匹配不到知识的问题）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingVo {
    private String question;
    private Long count;
    private Date lastTime;
}
