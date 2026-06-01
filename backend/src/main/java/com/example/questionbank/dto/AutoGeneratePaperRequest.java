package com.example.questionbank.dto;

import lombok.Data;

import java.util.List;

@Data
public class AutoGeneratePaperRequest {
    private Long teacherId;
    private Integer totalScore;
    private String difficulty;
    private Long chapterId;
    private String chapter;
    private List<PaperTypeRuleDTO> typeRules;
}
