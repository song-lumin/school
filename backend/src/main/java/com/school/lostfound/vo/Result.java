package com.school.lostfound.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应封装（Global Response Wrapper）—— 整个后端所有接口的返回值外壳
 *
 * 功能：所有 Controller 接口统一返回 Result&lt;T&gt;，前端以固定格式解析：
 * { code, message, data, timestamp }。
 * - code = 200 表示成功，其余（400/401/403/404/409/429/500）为各类失败；
 * - data 为泛型 T，成功时携带业务数据（可为任意类型或 null），
 *   失败时为 null。
 *
 * 为什么需要：
 * 1. 统一前后端契约——前端拦截器只需按 code 判断，不用解析各种异常结构；
 * 2. 全局异常处理器（GlobalExceptionHandler）捕获异常后也用本类的
 *    error 系列方法包装返回，保证"成功与失败走同一响应格式"；
 * 3. 泛型 T 提供编译期类型安全：接口声明 Result&lt;List&lt;FoundItemVO&gt;&gt;
 *    即可明确 data 的实际类型，便于前端生成 TypeScript 类型。
 *
 * 静态工厂方法设计：构造器私有化程度低（保留无参构造用于反序列化），
 * 外部统一通过语义化工厂方法创建实例——success()/error()/
 * badRequest()/unauthorized()/forbidden()/notFound()/conflict()/
 * tooManyRequests()，调用处可读性高且避免到处硬编码状态码。
 */
@Data
public class Result<T> implements Serializable {
    /** 业务状态码：200=成功，其余见各 error 工厂方法 */
    private Integer code;
    /** 提示信息（成功为 "success"，失败为具体错误描述） */
    private String message;
    /** 业务数据，泛型 T；失败时为 null */
    private T data;
    /** 响应时间戳（毫秒），构造时自动生成，便于前端调试与日志排查 */
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /** 成功，无数据（data = null） */
    public static <T> Result<T> success() {
        return success(null);
    }

    /** 成功，携带业务数据 */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    /** 成功，携带自定义提示信息与数据 */
    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    /** 失败，自定义状态码与信息（主要供全局异常处理器使用） */
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    /** 失败，默认 500 服务器内部错误 */
    public static <T> Result<T> error(String message) {
        return error(500, message);
    }

    /** 400 参数错误 / 请求不合法 */
    public static <T> Result<T> badRequest(String message) {
        return error(400, message);
    }

    /** 401 未登录或 token 失效 */
    public static <T> Result<T> unauthorized(String message) {
        return error(401, message);
    }

    /** 403 已登录但无权限 */
    public static <T> Result<T> forbidden(String message) {
        return error(403, message);
    }

    /** 404 资源不存在 */
    public static <T> Result<T> notFound(String message) {
        return error(404, message);
    }

    /** 409 资源冲突（如重复申请、状态不允许该操作） */
    public static <T> Result<T> conflict(String message) {
        return error(409, message);
    }

    /** 429 请求过于频繁（限流） */
    public static <T> Result<T> tooManyRequests(String message) {
        return error(429, message);
    }
}
