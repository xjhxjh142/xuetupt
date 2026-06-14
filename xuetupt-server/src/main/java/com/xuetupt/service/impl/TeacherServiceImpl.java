package com.xuetupt.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuetupt.dto.Result;
import com.xuetupt.entity.Teacher;
import com.xuetupt.mapper.TeacherMapper;
import com.xuetupt.service.ITeacherService;
import com.xuetupt.utils.CacheClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.xuetupt.utils.RedisConstants.*;

/**
 * 教师服务实现
 * <p>
 * 实现教师信息查询、列表展示等功能。
 * 使用缓存穿透防护策略查询教师详情。
 */
@Service
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements ITeacherService {

    @Resource
    private CacheClient cacheClient;

    /**
     * 根据 ID 查询教师详情
     * <p>
     * 使用缓存穿透防护策略查询。
     *
     * @param id 教师ID
     * @return 教师信息
     */
    @Override
    public Result queryTeacherById(Long id) {
        Teacher teacher = cacheClient.queryWithPassThrough(
                CACHE_TEACHER_KEY, id, Teacher.class,
                this::getById,
                CACHE_TEACHER_TTL, TimeUnit.MINUTES
        );
        if (teacher == null) {
            return Result.fail("教师不存在");
        }
        return Result.ok(teacher);
    }

    /**
     * 分页查询教师列表（按评分排序）
     *
     * @param current 当前页码
     * @return 教师列表（分页）
     */
    @Override
    public Result queryTeacherList(Integer current) {
        Page<Teacher> page = query()
                .orderByDesc("score")
                .page(new Page<>(current, 10));
        return Result.ok(page.getRecords(), page.getTotal());
    }
}
