package com.hutech.quizbackend.controller;

import com.hutech.quizbackend.entity.Exam;
import com.hutech.quizbackend.entity.Question;
import com.hutech.quizbackend.repository.ExamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/exams")
@CrossOrigin("*")
public class ExamController {

    @Autowired
    private ExamRepository examRepository;

    // Lưu toàn bộ đề thi sau khi Mạnh đã duyệt xong trên React
    @PostMapping("/save-full")
    public Exam saveExam(@RequestBody Exam exam) {
        if (exam.getQuestions() != null) {
            for (Question q : exam.getQuestions()) {
                q.setExam(exam); // Gắn ID bộ đề vào từng câu hỏi
            }
        }
        return examRepository.save(exam);
    }

    // Lấy danh sách để hiện lên Dashboard (Yêu cầu 3)
    @GetMapping
    public List<Exam> getAll() {
        return examRepository.findAll();
    }
}