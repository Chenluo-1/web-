package cn.ecom.domain.dto;

import lombok.Data;

/**
 * 退款审核 DTO
 */
@Data
public class RefundAuditDto {
    /** true 通过 / false 驳回 */
    private Boolean approve;
    private String remark;
}
