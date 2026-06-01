package com.example.questionbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExamRoomDTO {
    @NotBlank(message = "考场名称不能为空")
    private String roomName;
    private String location;
    @NotNull(message = "容纳人数不能为空")
    private Integer capacity;
    @NotNull(message = "教师ID不能为空")
    private Long teacherId;
}
