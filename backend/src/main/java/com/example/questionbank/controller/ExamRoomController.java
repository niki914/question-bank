package com.example.questionbank.controller;

import com.example.questionbank.dto.ExamRoomDTO;
import com.example.questionbank.dto.ExamRoomVO;
import com.example.questionbank.service.ExamRoomService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exam-rooms")
public class ExamRoomController {

    private final ExamRoomService service;

    public ExamRoomController(ExamRoomService service) {
        this.service = service;
    }

    @PostMapping
    public ExamRoomVO create(@Valid @RequestBody ExamRoomDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public ExamRoomVO update(@PathVariable Long id, @Valid @RequestBody ExamRoomDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        service.delete(id);
        return Map.of("success", true);
    }

    @GetMapping("/{id}")
    public ExamRoomVO getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public List<ExamRoomVO> listAll() {
        return service.listAll();
    }
}
