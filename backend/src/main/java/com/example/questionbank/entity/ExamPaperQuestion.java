package com.example.questionbank.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_paper_question")
public class ExamPaperQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long paperId;
    private Long questionId;
    private BigDecimal questionScore;
    private Integer questionOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
