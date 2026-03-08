package com.hutech.quizbackend.service;

import com.hutech.quizbackend.model.request.QuestionRequestDTO;
import com.hutech.quizbackend.model.response.AIExplainResponseDTO;

import java.util.List;

public interface IGeminiService {

    String generateAndSaveQuiz(String promptText);

    AIExplainResponseDTO explainQuestion(Long questionId, String userSelectedOption);

    List<QuestionRequestDTO> generateAdaptiveQuestions(List<String> weakQuestions, int count);
}
