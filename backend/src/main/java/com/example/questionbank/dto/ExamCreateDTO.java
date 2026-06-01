package com.example.questionbank.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExamCreateDTO {
    @NotBlank(message = "考试名称不能为空")
    private String examName;
    @NotNull(message = "试卷ID不能为空")
    private Long paperId;
    @NotNull(message = "考场ID不能为空")
    private Long roomId;
    @NotNull(message = "教师ID不能为空")
    private Long teacherId;
    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @NotNull(message = "考试时长不能为空")
    private Integer durationMinutes;
}
