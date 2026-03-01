package com.hutech.quizbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hutech.quizbackend.entity.Exam;
import com.hutech.quizbackend.entity.Question;
import com.hutech.quizbackend.repository.ExamRepository;
import com.hutech.quizbackend.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class GeminiService {
    @Value("${gemini.api.key}")
    private String apiKey;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private QuestionRepository questionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateAndSaveQuiz(String promptText) {
        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + apiKey;
        try {
            // Gọi Gemini AI để lấy JSON
            Map<String, Object> requestBody = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", "Trích xuất câu hỏi trắc nghiệm JSON từ văn bản sau: " + promptText)))));
            String rawResponse = new RestTemplate().postForObject(url, requestBody, String.class);
            JsonNode root = objectMapper.readTree(rawResponse);
            String quizJson = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            quizJson = quizJson.replace("```json", "").replace("```", "").trim();

            // LƯU VÀO DATABASE
            List<Map<String, Object>> list = objectMapper.readValue(quizJson, List.class);

            Exam exam = new Exam();
            exam.setTitle("Bộ đề luyện tập - " + LocalDateTime.now().withNano(0));
            exam.setTotalQuestions(list.size());
            exam = examRepository.save(exam); // Lưu Exam trước

            for (Map<String, Object> q : list) {
                Question question = new Question();
                question.setQuestion((String) q.get("question"));
                question.setAnswer((String) q.get("answer"));
                question.setOptions(new ArrayList<>((List<String>) q.get("options")));
                question.setExam(exam); // Gắn câu hỏi vào bộ đề
                questionRepository.save(question);
            }
            return quizJson;
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}

