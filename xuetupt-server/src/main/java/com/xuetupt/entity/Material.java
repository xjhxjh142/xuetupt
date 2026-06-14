package com.xuetupt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_material")
public class Material {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String coverImage;
    private BigDecimal price;
    private Integer stock;
    private Integer type;
    private String subject;
    private String grade;
    private String fileUrl;
    private String previewImages;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
