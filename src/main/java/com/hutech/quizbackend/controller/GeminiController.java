package com.hutech.quizbackend.controller;

import com.hutech.quizbackend.service.FileService;
import com.hutech.quizbackend.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*") // Hỗ trợ kết nối Frontend sau này
public class GeminiController {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private FileService fileService;

    /**
     * CÁCH 1: Tạo câu hỏi từ từ khóa (Topic)
     * URL: http://localhost:8080/api/ai/generate?topic=Java
     */
    @GetMapping("/generate")
    public ResponseEntity<String> generateByTopic(@RequestParam String topic) {
        String result = geminiService.generateAndSaveQuiz(topic);
        return ResponseEntity.ok(result);
    }

    /**
     * CÁCH 2: Tạo câu hỏi bằng cách tải file (PDF, DOCX, TXT)
     * URL: POST http://localhost:8080/api/ai/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<String> generateByFile(@RequestParam("file") MultipartFile file) {
        try {
            // 1. Bảo mật: Kiểm tra file rỗng
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Vui lòng chọn một file!");
            }

            // 2. Chống DDoS: Giới hạn kích thước file (Ví dụ 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("File quá lớn! Giới hạn tối đa là 5MB.");
            }

            // 3. Bóc tách văn bản từ file tài liệu
            String extractedText = fileService.extractText(file);

            // 4. Gửi nội dung văn bản cho Gemini AI để soạn câu hỏi và lưu MySQL
            // Chúng ta lồng nội dung file vào Prompt để AI hiểu ngữ cảnh
            String aiResponse = geminiService.generateAndSaveQuiz("Dựa trên tài liệu này: " + extractedText);

            return ResponseEntity.ok(aiResponse);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi xử lý file: " + e.getMessage());
        }
    }
}