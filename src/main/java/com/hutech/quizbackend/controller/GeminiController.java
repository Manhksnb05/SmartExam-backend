package com.hutech.quizbackend.controller;

import com.hutech.quizbackend.model.request.AIExplainRequestDTO;
import com.hutech.quizbackend.model.response.AIExplainResponseDTO;
import com.hutech.quizbackend.service.Impl.FileService;
import com.hutech.quizbackend.service.Impl.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
// Sửa origins để khớp với SecurityConfig và Frontend Vite
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class GeminiController {

    @Autowired private GeminiService geminiService;
    @Autowired private FileService fileService;

    // 1. API: Trích xuất câu hỏi trong file user gửi lên
    @PostMapping("/upload")
    public ResponseEntity<String> generateByFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) return ResponseEntity.badRequest().body("Vui lòng chọn file!");

            // Log để kiểm tra quá trình xử lý trong Console IntelliJ
            System.out.println("Đang trích xuất file: " + file.getOriginalFilename());

            String extractedText = fileService.extractText(file);
            // Gửi prompt kèm nội dung file cho AI
            String aiResponse = geminiService.generateAndSaveQuiz(extractedText);

            return ResponseEntity.ok(aiResponse);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // 2. API: Yêu cầu AI giải thích câu hỏi
    @PostMapping("/explain")
    public ResponseEntity<?> explainAnswer(@RequestBody AIExplainRequestDTO request) {

        try {
            // Service giờ đây trả về DTO đã được chia nhỏ
            AIExplainResponseDTO response = geminiService.explainQuestion(request.getQuestionId(), request.getUserSelectedOption());
            return ResponseEntity.ok(response);
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 3. API: Phân tích lệnh giọng nói
    @PostMapping("/parse-voice")
    public ResponseEntity<String> parseVoiceCommand(@RequestBody Map<String, Object> body) {
        String transcript = (String) body.getOrDefault("transcript", "");
        int totalQuestions = body.get("totalQuestions") instanceof Number
                ? ((Number) body.get("totalQuestions")).intValue()
                : 999;
        String result = geminiService.parseVoiceCommand(transcript, totalQuestions);
        return ResponseEntity.ok(result);
    }
}