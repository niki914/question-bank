package com.example.questionbank.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 考生某道题的答题+评分详情 */
@Data
public class StudentAnswerVO {
    private Long id;
    private Long questionId;
    private String questionTitle;
    private String questionType;
    private BigDecimal questionScore;   // 该题分值
    private String answerContent;
    private String answerImagePath;
    private BigDecimal autoScore;
    private BigDecimal teacherScore;
    private BigDecimal finalScore;
    private Long gradedBy;
    private LocalDateTime gradedTime;
}
