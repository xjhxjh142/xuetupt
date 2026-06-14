package com.xuetupt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xuetupt.dto.Result;
import com.xuetupt.entity.Material;

/**
 * 学习资料服务接口
 * <p>
 * 提供资料查询、购买、下载等功能。
 */
public interface IMaterialService extends IService<Material> {
    /** 根据 ID 查询资料详情 */
    Result queryMaterialById(Long id);

    /** 分页查询资料列表 */
    Result queryMaterialList(Integer type, Integer current);

    /** 秒杀购买资料 */
    Result seckillMaterial(Long materialId, Long userId);

    /** 获取资料下载链接 */
    Result getDownloadUrl(String orderNo, Long userId);
}
