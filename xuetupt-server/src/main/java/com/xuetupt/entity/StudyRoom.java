package com.xuetupt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_study_room")
public class StudyRoom {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String name;
    private String subject;
    private Integer maxCapacity;
    private Integer currentCount;
    private Integer status;
    private LocalDateTime createTime;
}
