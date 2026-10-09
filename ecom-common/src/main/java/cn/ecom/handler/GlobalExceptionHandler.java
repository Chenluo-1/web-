package cn.ecom.handler;

import cn.ecom.handler.exception.BusinessException;
import cn.ecom.handler.exception.LoginRequiredException;
import cn.ecom.utils.AppHttpCodeEnum;
import cn.ecom.utils.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseResult<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return ResponseResult.errorResult(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(LoginRequiredException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseResult<Void> handleLoginRequired(LoginRequiredException e) {
        return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseResult<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("权限不足：{}", e.getMessage());
        return ResponseResult.errorResult(AppHttpCodeEnum.NO_OPERATION_AUTHORITY);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseResult<Void> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("数据完整性异常：{}", e.getMessage());
        return ResponseResult.errorResult(AppHttpCodeEnum.SYSTEM_ERROR, "数据关联约束导致操作失败（可能已存在同名数据）");
    }

    @ExceptionHandler(Exception.class)
    public ResponseResult<Void> handleException(Exception e) {
        // 原始异常信息可能含 SQL、表名、类名等内部细节，只进日志，不外传
        log.error("系统异常", e);
        return ResponseResult.errorResult(AppHttpCodeEnum.SYSTEM_ERROR, "系统繁忙，请稍后重试");
    }
}
