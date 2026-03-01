package com.hutech.quizbackend.repository;

import com.hutech.quizbackend.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Interface quản lý các thao tác Database cho bảng exams
 * JpaRepository cung cấp sẵn các hàm: save, findAll, findById, delete...
 */
@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    // Mạnh có thể thêm các hàm truy vấn tùy chỉnh tại đây nếu cần
}