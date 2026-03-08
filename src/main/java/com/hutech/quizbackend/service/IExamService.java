package com.hutech.quizbackend.service;

import com.hutech.quizbackend.model.dto.*;
import com.hutech.quizbackend.model.request.SaveExamRequestDTO;

import java.util.List;

public interface IExamService {

    List<ExamPublicDTO> getPublicExams();

    ExamDetailPublicDTO getPublicExamDetail(Long id);

    String updateExamStatus(Long examId, String newStatus);

    PracticeResultDTO checkPracticeAnswers(PracticeRequestDTO request);

    void softDeleteExams(List<Long> ids);

    List<ExamSummaryDTO> getAllActiveExams();

    ExamSummaryDTO saveExtractedExam(SaveExamRequestDTO request);

    byte[] exportExamToWord(Long examId);
}
