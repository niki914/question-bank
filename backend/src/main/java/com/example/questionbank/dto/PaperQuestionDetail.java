package com.example.questionbank.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaperQuestionDetail {
    private Long id;
    private Long questionId;
    private BigDecimal questionScore;
    private Integer questionOrder;
    private String questionTitle;
    private String questionType;
    private String questionDifficulty;
    private String questionChapter;
    private String knowledgePoints;
    /** 题目 content_json，供前端渲染选项/填空/主观 */
    private String contentJson;
}
