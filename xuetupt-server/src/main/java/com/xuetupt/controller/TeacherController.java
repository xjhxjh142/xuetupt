package com.xuetupt.controller;

import com.xuetupt.dto.Result;
import com.xuetupt.service.ITeacherService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 教师管理控制器
 * <p>
 * 提供教师信息查询、列表展示等功能接口。
 */
@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    @Resource
    private ITeacherService teacherService;

    /**
     * 根据 ID 查询教师详情
     *
     * @param id 教师ID
     * @return 教师信息
     */
    @GetMapping("/{id}")
    public Result queryById(@PathVariable("id") Long id) {
        return teacherService.queryTeacherById(id);
    }

    /**
     * 分页查询教师列表（按评分排序）
     *
     * @param current 当前页码，默认第1页
     * @return 教师列表（分页）
     */
    @GetMapping("/list")
    public Result list(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return teacherService.queryTeacherList(current);
    }
}
