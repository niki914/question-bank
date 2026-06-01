package com.example.questionbank.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AutoGeneratePaperResponse {
    private Boolean success;
    private String message;
    private Integer totalScore;
    private Integer totalQuestionCount;
    private BigDecimal questionScore;
    private BigDecimal actualTotalScore;
    private String difficulty;
    private Integer totalKnowledgePointCount;
    private Integer coveredKnowledgePointCount;
    private BigDecimal coverageRate;
    private List<String> uncoveredKnowledgePoints;
    private List<PaperQuestionVO> questions;
}
