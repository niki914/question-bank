package com.example.questionbank.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PaperValidationResult {
    private boolean passed;
    private BigDecimal knowledgePointCoverage;
    private BigDecimal requiredCoverage;
    private List<String> uncoveredKnowledgePoints;
    private boolean difficultyMatched;
    private String difficultyDetail;
    private BigDecimal actualTotalScore;
    private BigDecimal expectedTotalScore;
    private boolean totalScoreMatched;
}
