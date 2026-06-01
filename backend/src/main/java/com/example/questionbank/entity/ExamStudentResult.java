package com.example.questionbank.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_student_result")
public class ExamStudentResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long examId;
    private String studentName;
    private String studentNo;
    private BigDecimal totalScore;
    /** SUBMITTED / GRADED */
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime submitTime;
}
