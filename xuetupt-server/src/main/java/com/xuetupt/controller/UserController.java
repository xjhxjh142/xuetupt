package com.xuetupt.controller;

import com.xuetupt.dto.Result;
import com.xuetupt.service.IUserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户管理控制器
 * <p>
 * 提供发送验证码、登录、测试登录、获取用户信息等功能接口。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private IUserService userService;

    /**
     * 发送短信验证码
     * <p>
     * 验证码会打印在控制台日志中，方便开发调试。
     *
     * @param phone 手机号
     * @return 验证码发送结果
     */
    @PostMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone) {
        return userService.sendCode(phone);
    }

    /**
     * 手机号 + 验证码登录
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 登录结果（含 token）
     */
    @PostMapping("/login")
    public Result login(@RequestParam("phone") String phone, @RequestParam("code") String code) {
        return userService.login(phone, code);
    }

    /**
     * 测试登录（免验证码）
     * <p>
     * 直接使用测试账号 13800138000 登录，返回 token。
     *
     * @return 登录结果（含 token）
     */
    @PostMapping("/login/test")
    public Result loginForTest() {
        return userService.loginForTest();
    }

    /**
     * 获取当前登录用户信息
     * <p>
     * 根据请求头中的 authorization token 查询用户信息。
     *
     * @param token 用户认证令牌
     * @return 用户信息（id、nickName、phone、icon）
     */
    @GetMapping("/info")
    public Result getUserInfo(@RequestHeader("authorization") String token) {
        return userService.getUserInfo(token);
    }
}
