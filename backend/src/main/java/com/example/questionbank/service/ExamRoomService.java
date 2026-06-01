package com.example.questionbank.service;

import com.example.questionbank.dto.ExamRoomDTO;
import com.example.questionbank.dto.ExamRoomVO;
import java.util.List;

public interface ExamRoomService {
    ExamRoomVO create(ExamRoomDTO dto);
    ExamRoomVO update(Long id, ExamRoomDTO dto);
    void delete(Long id);
    ExamRoomVO getById(Long id);
    List<ExamRoomVO> listAll();
}
