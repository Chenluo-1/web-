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
 * 搜索同义词实体（word 为同义词组首词，synonyms 逗号分隔）
 */
@SuppressWarnings("serial")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName("ec_search_synonym")
public class SearchSynonym implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String word;

    private String synonyms;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
