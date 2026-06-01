package com.example.questionbank.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PaperVO {
    private Long id;
    private Long teacherId;
    private String teacherName;
    private String paperName;
    private BigDecimal totalScore;
    private String difficulty;
    private String status;
    private String questionTypeDistribution;
    private BigDecimal knowledgePointCoverage;
    private BigDecimal actualTotalScore;
    private String validationResult;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<PaperQuestionDetail> questions;
}
