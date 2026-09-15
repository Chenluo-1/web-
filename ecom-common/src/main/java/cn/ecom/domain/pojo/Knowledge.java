package cn.ecom.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 客服知识库实体
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_knowledge")
public class Knowledge implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标准问法 */
    private String question;

    /** 相似问法（JSON 数组字符串） */
    private String similarQuestions;

    private String answer;

    private String category;

    /** 生效状态(0停用 1生效) */
    private Integer status;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
