package com.example.questionbank.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_student_answer")
public class ExamStudentAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long examId;
    private String studentName;
    private String studentNo;
    private Long questionId;
    private String answerContent;
    private String answerImagePath;
    private BigDecimal autoScore;
    private BigDecimal teacherScore;
    private BigDecimal finalScore;
    private Long gradedBy;
    private LocalDateTime gradedTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime submitTime;
}
