package com.xuetupt.controller;

import com.xuetupt.dto.Result;
import com.xuetupt.service.IMaterialService;
import com.xuetupt.utils.TokenUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 学习资料控制器
 * <p>
 * 提供资料查询、购买、下载等功能接口。
 */
@RestController
@RequestMapping("/api/material")
public class MaterialController {

    @Resource
    private IMaterialService materialService;

    @Resource
    private TokenUtils tokenUtils;

    /**
     * 根据 ID 查询资料详情
     *
     * @param id 资料ID
     * @return 资料信息
     */
    @GetMapping("/{id}")
    public Result queryById(@PathVariable("id") Long id) {
        return materialService.queryMaterialById(id);
    }

    /**
     * 分页查询资料列表
     *
     * @param type    资料类型（可选）
     * @param current 当前页码，默认第1页
     * @return 资料列表（分页）
     */
    @GetMapping("/list")
    public Result list(@RequestParam(value = "type", required = false) Integer type,
                       @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return materialService.queryMaterialList(type, current);
    }

    /**
     * 秒杀购买资料
     *
     * @param materialId 资料ID
     * @param token      用户认证令牌
     * @return 购买结果
     */
    @PostMapping("/seckill/{materialId}")
    public Result seckill(@PathVariable("materialId") Long materialId,
                          @RequestHeader("authorization") String token) {
        Long userId = tokenUtils.getUserIdFromToken(token);
        if (userId == null) {
            return Result.fail("未登录或登录已过期");
        }
        return materialService.seckillMaterial(materialId, userId);
    }

    /**
     * 获取资料下载链接
     *
     * @param orderNo 订单号
     * @param token   用户认证令牌
     * @return 下载地址
     */
    @GetMapping("/download")
    public Result download(@RequestParam("orderNo") String orderNo,
                           @RequestHeader("authorization") String token) {
        Long userId = tokenUtils.getUserIdFromToken(token);
        if (userId == null) {
            return Result.fail("未登录或登录已过期");
        }
        return materialService.getDownloadUrl(orderNo, userId);
    }
}
