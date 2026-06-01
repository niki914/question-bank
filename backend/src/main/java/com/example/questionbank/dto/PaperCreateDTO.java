package com.example.questionbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PaperCreateDTO {
    @NotNull(message = "教师ID不能为空")
    private Long teacherId;

    @NotBlank(message = "试卷名称不能为空")
    private String paperName;

    @NotNull(message = "总分不能为空")
    private BigDecimal totalScore;

    @NotBlank(message = "目标难度不能为空")
    private String difficulty;

    private String questionTypeDistribution;

    @NotNull(message = "试卷题目列表不能为空")
    private List<PaperQuestionItem> questions;
}
