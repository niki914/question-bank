package com.example.questionbank.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exam")
public class Exam {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String examName;
    private Long paperId;
    private Long roomId;
    private Long teacherId;
    private LocalDateTime startTime;
    private Integer durationMinutes;
    /** PENDING / ONGOING / GRADING / FINISHED */
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
