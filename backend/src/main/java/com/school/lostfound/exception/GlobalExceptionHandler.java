package com.school.lostfound.exception;

import com.school.lostfound.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * <p>作用：把散落在各处的 try-catch 收敛到一个类里统一处理。任何 Controller
 * 抛出的异常都会被这里的 @ExceptionHandler 方法按类型匹配捕获，并包装成
 * 统一的 {@link Result} JSON 结构返回，同时写入错误日志。</p>
 *
 * <p>这样做的好处：① Controller 代码保持干净，只写正常流程；
 * ② 前端总能拿到固定格式 {code, message, data}，方便统一弹窗提示；
 * ③ 避免把堆栈信息等敏感内容直接返回给用户（兜底 handler 返回固定文案）。</p>
 *
 * <p>@RestControllerAdvice = @ControllerAdvice + @ResponseBody，
 * 表示对全部 Controller 生效，且返回值直接序列化为 JSON。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常：返回业务方指定的错误码与提示信息（HTTP 状态仍为 200，靠 code 区分）
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理认证异常（未登录、Token 无效、密码错误等）：返回 401，提示重新登录
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<?> handleAuthenticationException(AuthenticationException e) {
        log.error("认证异常: {}", e.getMessage());
        // 密码错误是常见场景，给出更具体的提示；其余统一提示认证失败
        if (e instanceof BadCredentialsException) {
            return Result.unauthorized("用户名或密码错误");
        }
        return Result.unauthorized("认证失败");
    }

    /**
     * 处理权限不足异常（已登录但角色不够，如普通用户访问管理接口）：返回 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<?> handleAccessDeniedException(AccessDeniedException e) {
        log.error("权限不足: {}", e.getMessage());
        return Result.forbidden("权限不足");
    }

    /**
     * 处理 @RequestBody + @Valid 参数校验失败：把所有字段的错误提示拼接返回（400）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        // 收集每个校验失败字段上 @NotNull/@NotBlank 等注解里写的 message，用分号拼接
        String errors = e.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.error("参数校验失败: {}", errors);
        return Result.badRequest("参数校验失败: " + errors);
    }

    /**
     * 处理表单绑定类参数校验失败（非 JSON body 的场景）：逻辑同上（400）
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e) {
        String errors = e.getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.error("参数绑定失败: {}", errors);
        return Result.badRequest("参数绑定失败: " + errors);
    }

    /**
     * 处理非法参数异常（如枚举 code 不存在）：返回 400，把原因透出给前端
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("非法参数: {}", e.getMessage());
        return Result.badRequest(e.getMessage());
    }

    /**
     * 兜底处理所有未捕获异常：记录完整堆栈便于排查，但对用户只返回模糊提示，
     * 避免泄露内部实现细节（安全考虑）
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error("系统异常，请联系管理员");
    }
}
