package cn.ecom.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 商品评价展示 VO（含评价人昵称，隐藏用户敏感信息）
 */
@SuppressWarnings("serial")
@Data
public class ReviewVo implements Serializable {

    private Long id;

    private Integer rating;

    private String content;

    /** 评价人昵称（脱敏展示：只给昵称） */
    private String nickName;

    private Date createTime;
}
