package com.example.questionbank.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaperQuestionItem {
    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    @NotNull(message = "题目分值不能为空")
    private BigDecimal questionScore;
}
