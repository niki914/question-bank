package com.example.questionbank.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exam_room")
public class ExamRoom {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roomName;
    private String location;
    private Integer capacity;
    private Long teacherId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
