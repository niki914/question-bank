package com.example.questionbank.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExamRoomVO {
    private Long id;
    private String roomName;
    private String location;
    private Integer capacity;
    private Long teacherId;
    private String teacherName;
    private LocalDateTime createTime;
}
