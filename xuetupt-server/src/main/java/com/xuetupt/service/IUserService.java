package com.xuetupt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuetupt.dto.Result;
import com.xuetupt.entity.User;

/**
 * 用户服务接口
 * <p>
 * 提供发送验证码、登录、测试登录、获取用户信息等功能。
 */
public interface IUserService extends IService<User> {
    /** 发送短信验证码 */
    Result sendCode(String phone);

    /** 手机号 + 验证码登录 */
    Result login(String phone, String code);

    /** 测试登录（免验证码） */
    Result loginForTest();

    /** 根据 token 获取当前登录用户信息 */
    Result getUserInfo(String token);
}
