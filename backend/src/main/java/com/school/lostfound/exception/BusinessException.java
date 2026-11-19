package com.school.lostfound.exception;

import lombok.Getter;

/**
 * 业务异常类
 *
 * <p>当业务规则不满足时（如「物品已被认领」「信用分不足」「无权操作他人物品」），
 * Service 层直接 throw BusinessException，由 GlobalExceptionHandler 统一捕获
 * 并转成带错误码的 JSON 返回给前端。</p>
 *
 * <p>为什么需要它：如果不定义业务异常，只能靠返回 null 或错误码层层传递，
 * 代码冗长且易漏处理；用「抛异常 + 全局处理器」的方式可以让主流程代码保持简洁。</p>
 *
 * <p>继承 RuntimeException（非受检异常）：业务代码中无需在方法签名上声明 throws，
 * 调用链上的代码也不会被强制 try-catch 包裹。</p>
 */
@Getter
public class BusinessException extends RuntimeException {
    /** 错误码，与前端约定：200 成功、401 未登录、403 无权限、404 不存在、500 通用失败 */
    private final Integer code;

    /**
     * 使用默认错误码 500 构造业务异常
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 指定错误码与提示信息，如 new BusinessException(404, "物品不存在")
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 携带底层原因的构造，用于包装 DAO/IO 等底层异常并附加人话提示
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }
}
