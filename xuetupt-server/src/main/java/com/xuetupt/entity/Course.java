package com.xuetupt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("tb_course")
public class Course {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long teacherId;
    private String title;
    private String subtitle;
    private BigDecimal price;
    private Integer stock;
    private Integer type;
    private String subject;
    private String grade;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
