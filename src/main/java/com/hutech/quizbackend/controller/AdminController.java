package com.hutech.quizbackend.controller;

import com.hutech.quizbackend.entity.User;
import com.hutech.quizbackend.repository.ExamRepository;
import com.hutech.quizbackend.repository.ResultRepository;
import com.hutech.quizbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private UserRepository userRepository;
    @Autowired private ExamRepository examRepository;
    @Autowired private ResultRepository resultRepository;

    // GET /api/admin/stats — Thống kê tổng quan
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        long totalUsers   = userRepository.count();
        long totalExams   = examRepository.countByActiveTrue();
        long totalUploads = examRepository.countByActiveTrue(); // mỗi đề = 1 lượt tải lên
        long totalResults = resultRepository.count();

        return ResponseEntity.ok(Map.of(
            "totalUsers",   totalUsers,
            "totalExams",   totalExams,
            "totalUploads", totalUploads,
            "totalResults", totalResults
        ));
    }

    // GET /api/admin/users — Danh sách người dùng
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }
}
