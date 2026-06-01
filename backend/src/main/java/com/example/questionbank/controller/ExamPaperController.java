package com.example.questionbank.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.questionbank.dto.*;
import com.example.questionbank.service.ExamPaperService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/papers")
public class ExamPaperController {

    private final ExamPaperService examPaperService;

    public ExamPaperController(ExamPaperService examPaperService) {
        this.examPaperService = examPaperService;
    }

    @PostMapping
    public PaperVO create(@Valid @RequestBody PaperCreateDTO dto) {
        return examPaperService.create(dto);
    }

    @PutMapping("/{id}")
    public PaperVO update(@PathVariable Long id, @Valid @RequestBody PaperCreateDTO dto) {
        return examPaperService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        examPaperService.delete(id);
        return Map.of("success", true);
    }

    @GetMapping("/{id}")
    public PaperVO getById(@PathVariable Long id) {
        return examPaperService.getById(id);
    }

    @GetMapping
    public Page<PaperVO> page(
            @RequestParam Long teacherId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status) {
        return examPaperService.page(teacherId, page, size, status);
    }

    @PostMapping("/{id}/validate")
    public PaperValidationResult validate(@PathVariable Long id) {
        return examPaperService.validate(id);
    }

    @PostMapping("/{id}/submit")
    public PaperVO submit(@PathVariable Long id) {
        PaperValidationResult result = examPaperService.validate(id);
        if (!result.isPassed()) {
            throw new IllegalArgumentException("试卷校验未通过，请检查校验结果");
        }
        return examPaperService.submit(id);
    }
}
