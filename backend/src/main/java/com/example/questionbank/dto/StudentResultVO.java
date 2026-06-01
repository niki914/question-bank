package com.example.questionbank.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 考生成绩汇总（含各题详情） */
@Data
public class StudentResultVO {
    private Long id;
    private Long examId;
    private String studentName;
    private String studentNo;
    private BigDecimal totalScore;
    private String status;
    private LocalDateTime submitTime;
    private List<StudentAnswerVO> answers;
}
