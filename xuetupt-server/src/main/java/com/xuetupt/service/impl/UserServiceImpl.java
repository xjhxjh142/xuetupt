package com.xuetupt.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuetupt.dto.Result;
import com.xuetupt.entity.User;
import com.xuetupt.mapper.UserMapper;
import com.xuetupt.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.xuetupt.utils.RedisConstants.*;

/**
 * 用户服务实现
 * <p>
 * 实现发送验证码、登录、测试登录等功能。
 * 使用 Redis 存储验证码和登录 token。
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送短信验证码
     * <p>
     * 验证码会打印在控制台日志中，方便开发调试。
     * 生产环境应对接短信服务商。
     *
     * @param phone 手机号
     * @return 验证码发送结果
     */
    @Override
    public Result sendCode(String phone) {
        // 生成6位随机验证码
        String code = RandomUtil.randomNumbers(6);
        // 保存到 Redis，有效期5分钟
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        // 打印验证码到日志（开发调试用）
        log.info("验证码发送成功，手机号：{}，验证码：{}", phone, code);
        return Result.ok();
    }

    /**
     * 手机号 + 验证码登录
     * <p>
     * 验证码验证通过后，生成 token 并保存用户信息到 Redis。
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 登录结果（含 token）
     */
    @Override
    public Result login(String phone, String code) {
        // 从 Redis 获取验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if (StrUtil.isBlank(cacheCode)) {
            return Result.fail("验证码已过期");
        }
        if (!cacheCode.equals(code)) {
            return Result.fail("验证码错误");
        }

        // 查询或创建用户
        User user = query().eq("phone", phone).one();
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setNickName("用户" + RandomUtil.randomString(6));
            save(user);
        }

        // 生成 token
        String token = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(LOGIN_TOKEN_KEY + token, String.valueOf(user.getId()), LOGIN_TOKEN_TTL, TimeUnit.DAYS);

        // 删除验证码
        stringRedisTemplate.delete(LOGIN_CODE_KEY + phone);

        return Result.ok(token);
    }

    /**
     * 测试登录（免验证码）
     * <p>
     * 直接使用测试账号 13800138000 登录，返回 token。
     * 方便前端开发调试。
     *
     * @return 登录结果（含 token）
     */
    @Override
    public Result loginForTest() {
        String testPhone = "13800138000";
        User user = query().eq("phone", testPhone).one();
        if (user == null) {
            user = new User();
            user.setPhone(testPhone);
            user.setNickName("测试用户");
            save(user);
        }

        String token = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(LOGIN_TOKEN_KEY + token, String.valueOf(user.getId()), LOGIN_TOKEN_TTL, TimeUnit.DAYS);

        return Result.ok(token);
    }

    /**
     * 根据 token 获取当前登录用户信息
     * <p>
     * 从 Redis 中解析 userId，再查询数据库返回用户信息。
     *
     * @param token 用户认证令牌
     * @return 用户信息（含 id、nickName、phone、icon）
     */
    @Override
    public Result getUserInfo(String token) {
        if (StrUtil.isBlank(token)) {
            return Result.fail("未登录");
        }
        // 兼容 "Bearer " 前缀
        String actualToken = token;
        if (token.startsWith("Bearer ")) {
            actualToken = token.substring(7);
        }
        String userIdStr = stringRedisTemplate.opsForValue().get(LOGIN_TOKEN_KEY + actualToken);
        if (StrUtil.isBlank(userIdStr)) {
            return Result.fail("登录已过期，请重新登录");
        }
        Long userId = Long.parseLong(userIdStr);
        User user = getById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        // 只返回必要信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("nickName", user.getNickName());
        userInfo.put("phone", user.getPhone());
        userInfo.put("icon", user.getIcon());
        return Result.ok(userInfo);
    }
}
