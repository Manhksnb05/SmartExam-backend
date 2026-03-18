package com.hutech.quizbackend.repository;

import com.hutech.quizbackend.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Interface quản lý các thao tác Database cho bảng exams
 * JpaRepository cung cấp sẵn các hàm: save, findAll, findById, delete...
 */
@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    // Tìm các đề thi có status cụ thể VÀ chưa bị xóa (active = true), sắp xếp mới nhất lên đầu
    List<Exam> findByStatusAndActiveTrueOrderByCreatedAtDesc(String status);

    // Tìm 1 bộ đề cụ thể theo ID, đảm bảo nó đang Public và chưa bị xóa
    java.util.Optional<Exam> findByIdAndStatusAndActiveTrue(Long id, String status);

    // Lấy đề bất kể Public/Private (chỉ cần active = true)
    java.util.Optional<Exam> findByIdAndActiveTrue(Long id);

    // Lấy toàn bộ danh sách bộ đề chưa bị xóa, mới nhất xếp trước
    List<Exam> findByActiveTrueOrderByCreatedAtDesc();

    // Tìm danh sách bộ đề do 1 User tạo, chưa bị xóa, mới nhất xếp trước
    List<Exam> findByUserIdAndActiveTrueOrderByCreatedAtDesc(Long userId);

    // Đếm tổng số đề đang active (dùng cho Admin stats)
    long countByActiveTrue();
}