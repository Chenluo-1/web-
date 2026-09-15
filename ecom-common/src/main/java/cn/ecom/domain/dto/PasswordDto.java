package cn.ecom.domain.dto;

import lombok.Data;

/**
 * 密码 DTO：管理员重置（id+newPassword）；本人修改（oldPassword+newPassword）
 */
@Data
public class PasswordDto {
    private Long id;
    private String oldPassword;
    private String newPassword;
}
