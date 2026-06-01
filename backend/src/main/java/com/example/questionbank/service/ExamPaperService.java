package com.example.questionbank.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.questionbank.dto.*;

public interface ExamPaperService {
    PaperVO create(PaperCreateDTO dto);
    PaperVO update(Long id, PaperCreateDTO dto);
    void delete(Long id);
    PaperVO getById(Long id);
    Page<PaperVO> page(Long teacherId, Integer pageNum, Integer pageSize, String status);
    PaperValidationResult validate(Long id);
    PaperVO submit(Long id);
}
