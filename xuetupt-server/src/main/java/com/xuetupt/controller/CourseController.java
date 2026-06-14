package com.xuetupt.controller;

import com.xuetupt.dto.Result;
import com.xuetupt.service.ICourseService;
import com.xuetupt.utils.TokenUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 课程管理控制器
 * <p>
 * 提供课程查询、秒杀预约、支付等功能接口。
 */
@RestController
@RequestMapping("/api/course")
public class CourseController {

    @Resource
    private ICourseService courseService;

    @Resource
    private TokenUtils tokenUtils;

    /**
     * 根据 ID 查询课程详情
     *
     * @param id 课程ID
     * @return 课程信息
     */
    @GetMapping("/{id}")
    public Result queryCourseById(@PathVariable("id") Long id) {
        return courseService.queryCourseById(id);
    }

    /**
     * 分页查询课程列表
     *
     * @param type    课程类型（可选）
     * @param current 当前页码，默认第1页
     * @return 课程列表（分页）
     */
    @GetMapping("/list")
    public Result queryCourseList(@RequestParam(value = "type", required = false) Integer type,
                                  @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return courseService.queryCourseList(type, current);
    }

    /**
     * 获取热门课程列表（首页展示）
     *
     * @return 热门课程列表（前8名）
     */
    @GetMapping("/hot")
    public Result hotCourseList() {
        return courseService.getHotCourseList();
    }


    /**
     * 秒杀预约课程
     *
     * @param courseId 课程ID
     * @param token    用户认证令牌
     * @return 预约结果
     */
    @PostMapping("/seckill/{courseId}")
    public Result seckill(@PathVariable("courseId") Long courseId,
                          @RequestHeader("authorization") String token) {
        Long userId = tokenUtils.getUserIdFromToken(token);
        if (userId == null) {
            return Result.fail("未登录或登录已过期");
        }
        return courseService.seckillCourse(courseId, userId);
    }

    /**
     * 支付订单
     *
     * @param orderNo 订单号
     * @param token   用户认证令牌
     * @return 支付结果
     */
    @PostMapping("/pay")
    public Result pay(@RequestParam("orderNo") String orderNo,
                      @RequestHeader("authorization") String token) {
        Long userId = tokenUtils.getUserIdFromToken(token);
        if (userId == null) {
            return Result.fail("未登录或登录已过期");
        }
        return courseService.payCourse(orderNo, userId);
    }
}
