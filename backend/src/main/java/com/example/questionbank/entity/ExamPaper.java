package com.example.questionbank.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_paper")
public class ExamPaper {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teacherId;
    private String paperName;
    private BigDecimal totalScore;
    private String difficulty;
    private String status;
    private String questionTypeDistribution;
    private BigDecimal knowledgePointCoverage;
    private BigDecimal actualTotalScore;
    private String validationResult;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
