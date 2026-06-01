package com.example.questionbank.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ExamVO {
    private Long id;
    private String examName;
    private Long paperId;
    private String paperName;
    private Long roomId;
    private String roomName;
    private Long teacherId;
    private String teacherName;
    private LocalDateTime startTime;
    private Integer durationMinutes;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 考场详情中附带试卷题目列表 */
    private List<PaperQuestionDetail> questions;
}
