package com.xuetupt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_teacher")
public class Teacher {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String name;
    private String avatar;
    private String title;
    private String subject;
    private String description;
    private BigDecimal score;
    private Integer courseCount;
    private Integer studentCount;
    private LocalDateTime createTime;
}
