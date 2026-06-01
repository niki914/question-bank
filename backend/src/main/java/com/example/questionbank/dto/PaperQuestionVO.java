package com.example.questionbank.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaperQuestionVO {
    private Long id;
    private String type;
    private String title;
    private String difficulty;
    private String chapter;
    private String knowledgePoints;
    private BigDecimal score;
}
