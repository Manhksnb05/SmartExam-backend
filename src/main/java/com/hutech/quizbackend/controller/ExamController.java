package com.hutech.quizbackend.controller;

import com.hutech.quizbackend.entity.Exam;
import com.hutech.quizbackend.entity.Question;
import com.hutech.quizbackend.model.dto.*;
import com.hutech.quizbackend.model.request.SaveExamRequestDTO;
import com.hutech.quizbackend.repository.ExamRepository;
import com.hutech.quizbackend.service.Impl.ExamService;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.List;

@RestController
@RequestMapping("/api/exams")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ExamController {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamService examService;

    // 1. Lấy toàn bộ danh sách bộ đề (Hiện ô vuông ở Dashboard)
    @GetMapping
    public ResponseEntity<?> getAllActiveExams() {

        try {
            List<ExamSummaryDTO> exams = examService.getAllActiveExams();
            return ResponseEntity.ok(exams);
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 2. Lưu bộ đề thi (Sau khi AI trích xuất xong)
    @PostMapping("/save-full")
    public ResponseEntity<?> saveExam(@RequestBody SaveExamRequestDTO request) {
        try {
            ExamSummaryDTO savedExam = examService.saveExtractedExam(request);
            return ResponseEntity.ok(savedExam);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi khi lưu đề thi: " + e.getMessage());
        }
    }

    // 3. Xuất bộ đề gốc ra file Word (.docx)
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportExam(@PathVariable Long id) {
        try {
            byte[] documentBytes = examService.exportExamToWord(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "DeThi_Goc_" + id + ".docx");

            return new ResponseEntity<>(documentBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // 4. API: Nộp bài Luyện đề (Chấm điểm & Ghi nhận câu sai/đúng)
    @PostMapping("/practice-submit")
    public ResponseEntity<PracticeResultDTO> submitPractice(@RequestBody PracticeRequestDTO request) {
        try {
            PracticeResultDTO result = examService.checkPracticeAnswers(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 5. API: Xóa mềm nhiều đề thi gốc
    @DeleteMapping // Đường dẫn sẽ là /api/exams
    public ResponseEntity<String> deleteExams(@RequestBody List<Long> ids) {
        try {
            examService.softDeleteExams(ids);
            return ResponseEntity.ok("Đã xóa " + ids.size() + " bộ đề thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 6. API: Lấy danh sách đề thi Public (Cho Ngân hàng đề thi)
    @GetMapping("/public")
    public ResponseEntity<?> getPublicExams() {

        try {
            List<ExamPublicDTO> publicExams = examService.getPublicExams();
            return ResponseEntity.ok(publicExams);
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 7. API: Xem chi tiết 1 bộ đề Public (Bao gồm câu hỏi)
    @GetMapping("/public/{id}")
    public ResponseEntity<?> getPublicExamDetail(@PathVariable Long id) {
        try {
            ExamDetailPublicDTO detail = examService.getPublicExamDetail(id);
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 7b. API: Xem chi tiết 1 bộ đề bất kể Public/Private (dùng cho luyện đề cá nhân)
    @GetMapping("/{id}/detail")
    public ResponseEntity<?> getExamDetail(@PathVariable Long id) {
        try {
            ExamDetailPublicDTO detail = examService.getExamDetail(id);
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 8. API: Cập nhật trạng thái Public/Private cho bộ đề
    @PatchMapping("/{id}/status")
    public ResponseEntity<String> updateExamStatus(@PathVariable Long id, @RequestBody UpdateExamStatusDTO request) {
        try {
            String message = examService.updateExamStatus(id, request.getStatus());
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 9. API: Lấy danh sách bộ đề của User
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserExams(@PathVariable Long userId) {

        try {
            return ResponseEntity.ok(examService.getUserExams(userId));
        }
        catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 10. ✅ MỚI: Đổi tên bộ đề
    @PatchMapping("/{id}/title")
    public ResponseEntity<?> updateExamTitle(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        try {
            String newTitle = body.get("title");
            if (newTitle == null || newTitle.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Tên đề thi không được để trống!");
            }
            com.hutech.quizbackend.entity.Exam exam = examRepository.findByIdAndActiveTrue(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ đề!"));
            exam.setTitle(newTitle.trim());
            examRepository.save(exam);
            return ResponseEntity.ok(java.util.Map.of("message", "Đã cập nhật tên bộ đề thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}