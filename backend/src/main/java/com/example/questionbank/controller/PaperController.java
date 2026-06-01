package com.example.questionbank.controller;

import com.example.questionbank.dto.AutoGeneratePaperRequest;
import com.example.questionbank.dto.AutoGeneratePaperResponse;
import com.example.questionbank.service.PaperService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/papers")
public class PaperController {

    private final PaperService paperService;

    public PaperController(PaperService paperService) {
        this.paperService = paperService;
    }

    @PostMapping("/auto-generate")
    public AutoGeneratePaperResponse autoGenerate(@RequestBody AutoGeneratePaperRequest request) {
        return paperService.autoGenerate(request);
    }
}
