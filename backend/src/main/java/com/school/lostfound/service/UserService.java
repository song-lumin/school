package com.school.lostfound.service;

import com.school.lostfound.dto.LoginRequest;
import com.school.lostfound.dto.RegisterRequest;
import com.school.lostfound.dto.UpdatePasswordRequest;
import com.school.lostfound.dto.UpdateProfileRequest;
import com.school.lostfound.entity.User;
import com.school.lostfound.vo.LoginVO;
import com.school.lostfound.vo.UserVO;

/**
 * 用户服务接口 —— 定义账号体系的全部业务能力。
 * <p>
 * 作用：Controller 层只依赖本接口而不依赖实现类，符合"面向接口编程"原则，
 * 便于在 impl 包中的 UserServiceImpl 独立维护具体逻辑（密码加密、JWT 签发、
 * 学号/邮箱唯一性校验等），也方便单测时用 Mock 替换。
 * <p>
 * 覆盖的业务场景：注册、登录（返回 JWT 令牌）、查看/修改个人资料、
 * 修改密码、按 ID 查询用户实体（供其他 Service 内部调用）。
 * DTO（XXXRequest）负责入参校验，VO（XXXVO）负责出参脱敏——
 * VO 中不会暴露密码哈希等敏感字段。
 */
public interface UserService {
    /**
     * 用户注册：校验学号/用户名唯一性、加密存储密码、创建账号并直接登录（返回令牌）。
     *
     * @param request 注册信息（学号、用户名、密码等，已通过 DTO 注解做格式校验）
     * @return 登录结果（含 JWT 令牌与用户信息），注册即登录，减少一次操作
     */
    LoginVO register(RegisterRequest request);

    /**
     * 用户登录：校验账号密码后签发 JWT。
     *
     * @param request 登录请求（用户名 + 明文密码）
     * @return 登录结果（含 JWT 令牌、角色等前端路由所需信息）
     */
    LoginVO login(LoginRequest request);

    /**
     * 查询当前登录用户的个人资料。
     *
     * @param userId 当前用户 ID（由 JWT 过滤器解析得到，非前端传入，防越权）
     * @return 脱敏后的用户信息 VO
     */
    UserVO getCurrentUser(Long userId);

    /**
     * 修改个人资料（昵称、头像、联系方式等）。
     *
     * @param userId  当前用户 ID，保证只能改自己的资料
     * @param request 待修改的字段集
     * @return 修改后的最新资料 VO
     */
    UserVO updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 修改密码：需验证旧密码正确后才允许更新。
     *
     * @param userId  当前用户 ID
     * @param request 旧密码 + 新密码
     */
    void updatePassword(Long userId, UpdatePasswordRequest request);

    /**
     * 按主键查询用户实体（完整字段，含密码哈希）。
     * 仅供服务端内部各 Service 之间调用，不直接暴露给 Controller/前端。
     *
     * @param userId 用户主键
     * @return 用户实体；不存在时由实现类决定抛异常或返回 null
     */
    User getUserById(Long userId);
}
