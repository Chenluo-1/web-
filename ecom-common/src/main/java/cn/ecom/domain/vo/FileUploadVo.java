package cn.ecom.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传返回：访问 URL + 原文件名
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadVo {
    private String url;
    private String name;
}
