package com.xuetupt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuetupt.dto.Result;
import com.xuetupt.entity.Course;

/**
 * 课程服务接口
 * <p>
 * 提供课程查询、秒杀预约、支付等功能。
 */
public interface ICourseService extends IService<Course> {
    /** 根据 ID 查询课程详情 */
    Result queryCourseById(Long id);

    /** 分页查询课程列表 */
    Result queryCourseList(Integer typeId, Integer current);

    /** 获取热门课程列表（按秒杀热度排序） */
    Result getHotCourseList();

    /** 秒杀预约课程 */

    Result seckillCourse(Long courseId, Long userId);

    /** 支付订单 */
    Result payCourse(String orderNo, Long userId);
}
