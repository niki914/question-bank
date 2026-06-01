package com.example.questionbank.service;

import com.example.questionbank.dto.AutoGeneratePaperRequest;
import com.example.questionbank.dto.AutoGeneratePaperResponse;

public interface PaperService {
    AutoGeneratePaperResponse autoGenerate(AutoGeneratePaperRequest request);
}
