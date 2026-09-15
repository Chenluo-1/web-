package cn.ecom.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码。编号段：1xxx 系统/用户，2xxx 商品，3xxx 交易，4xxx 文件
 */
@Getter
@AllArgsConstructor
public enum AppHttpCodeEnum {

    SUCCESS(200, "操作成功"),
    PARAMETER_ERROR(400, "参数错误"),
    NEED_LOGIN(401, "需要登录后操作"),
    NO_OPERATION_AUTHORITY(403, "无操作权限"),
    NOT_FOUND(404, "请求的资源不存在"),
    SYSTEM_ERROR(500, "系统异常"),

    USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码错误"),
    USERNAME_EXIST(1002, "用户名已存在"),
    USER_NOT_EXIST(1003, "用户不存在"),
    ROLE_NOT_EXIST(1004, "角色不存在"),
    ACCOUNT_DISABLED(1005, "账号已停用"),
    OLD_PASSWORD_ERROR(1006, "原密码错误"),

    CATEGORY_HAS_PRODUCT(2002, "类目下存在商品，不允许删除"),
    CATEGORY_HAS_CHILD(2003, "类目下存在子类目，不允许删除"),
    PRODUCT_NOT_EXIST(2004, "商品不存在"),
    PRODUCT_OFF_SHELF(2005, "商品已下架"),
    SKU_NOT_EXIST(2006, "规格不存在"),

    STOCK_NOT_ENOUGH(3001, "库存不足"),
    CART_EMPTY(3002, "购物车没有选中商品"),
    ORDER_NOT_EXIST(3003, "订单不存在"),
    ORDER_STATUS_ERROR(3004, "当前订单状态不允许该操作"),
    REPEAT_SUBMIT(3005, "请勿重复提交"),
    COUPON_INVALID(3006, "优惠券不可用"),
    COUPON_RECEIVED_LIMIT(3007, "已达到领取上限"),
    REVIEW_REPEAT(3008, "该商品已评价，不能重复评价"),

    FILE_UPLOAD_ERROR(4001, "文件上传失败");

    private final Integer code;
    private final String msg;
}
