package com.xuetupt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuetupt.dto.Result;
import com.xuetupt.entity.Teacher;

/**
 * 教师服务接口
 * <p>
 * 提供教师信息查询、列表展示等功能。
 */
public interface ITeacherService extends IService<Teacher> {
    /** 根据 ID 查询教师详情 */
    Result queryTeacherById(Long id);

    /** 分页查询教师列表（按评分排序） */
    Result queryTeacherList(Integer current);
}
