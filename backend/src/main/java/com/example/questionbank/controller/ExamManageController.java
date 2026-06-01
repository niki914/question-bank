package com.example.questionbank.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.questionbank.dto.*;
import com.example.questionbank.service.ExamManageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exams")
public class ExamManageController {

    private final ExamManageService service;

    public ExamManageController(ExamManageService service) {
        this.service = service;
    }

    // ---------- 考试 CRUD ----------

    @PostMapping
    public ExamVO create(@Valid @RequestBody ExamCreateDTO dto) {
        return service.createExam(dto);
    }

    @PutMapping("/{id}")
    public ExamVO update(@PathVariable Long id, @Valid @RequestBody ExamCreateDTO dto) {
        return service.updateExam(id, dto);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        service.deleteExam(id);
        return Map.of("success", true);
    }

    @GetMapping("/{id}")
    public ExamVO getById(@PathVariable Long id) {
        return service.getExamById(id);
    }

    @GetMapping
    public Page<ExamVO> page(
            @RequestParam Long teacherId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status) {
        return service.pageExams(teacherId, page, size, status);
    }

    // ---------- 状态流转 ----------

    @PostMapping("/{id}/start")
    public ExamVO start(@PathVariable Long id) {
        return service.startExam(id);
    }

    @PostMapping("/{id}/end")
    public ExamVO end(@PathVariable Long id) {
        return service.endExam(id);
    }

    @PostMapping("/{id}/finish-grading")
    public ExamVO finishGrading(@PathVariable Long id) {
        return service.finishGrading(id);
    }

    // ---------- 考生作答 ----------

    @PostMapping("/submit")
    public StudentResultVO submitAnswer(@Valid @RequestBody StudentSubmitDTO dto) {
        return service.submitAnswer(dto);
    }

    @GetMapping("/{examId}/results")
    public List<StudentResultVO> listResults(@PathVariable Long examId) {
        return service.listResults(examId);
    }

    @GetMapping("/{examId}/results/{studentNo}")
    public StudentResultVO getStudentResult(@PathVariable Long examId,
                                            @PathVariable String studentNo) {
        return service.getStudentResult(examId, studentNo);
    }

    // ---------- 教师评分 ----------

    @PostMapping("/grade")
    public StudentAnswerVO gradeSubjective(@Valid @RequestBody TeacherGradeDTO dto) {
        return service.gradeSubjective(dto);
    }

    @PostMapping("/{examId}/results/{studentNo}/calc-total")
    public StudentResultVO calcTotal(@PathVariable Long examId,
                                     @PathVariable String studentNo) {
        return service.calcTotalScore(examId, studentNo);
    }
}
