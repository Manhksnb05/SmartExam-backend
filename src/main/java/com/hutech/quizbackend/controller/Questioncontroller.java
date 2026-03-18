package com.hutech.quizbackend.controller;

import com.hutech.quizbackend.entity.Question;
import com.hutech.quizbackend.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
public class Questioncontroller {

    @Autowired
    private QuestionRepository questionRepository;

    // PUT /api/questions/{id} — Sửa nội dung câu hỏi
    @PutMapping("/{id}")
    public ResponseEntity<?> updateQuestion(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            Question q = questionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi!"));

            if (body.containsKey("questionContent")) {
                q.setQuestion((String) body.get("questionContent"));
            }
            if (body.containsKey("options")) {
                q.setOptions((List<String>) body.get("options"));
            }
            if (body.containsKey("answer")) {
                String ans = ((String) body.get("answer")).trim().toUpperCase();
                q.setAnswer(ans);
            }

            questionRepository.save(q);
            return ResponseEntity.ok(Map.of("message", "Cập nhật câu hỏi thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}