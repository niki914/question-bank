package com.example.questionbank.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 教师对单道主观题评分请求
 */
@Data
public class TeacherGradeDTO {
    @NotNull
    private Long answerId;      // exam_student_answer.id
    @NotNull
    private BigDecimal score;   // 教师给分
    @NotNull
    private Long gradedBy;      // 教师ID
}
