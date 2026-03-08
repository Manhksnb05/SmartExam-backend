package com.hutech.quizbackend.repository;

import com.hutech.quizbackend.entity.UserQuestionStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserQuestionStatRepository extends JpaRepository<UserQuestionStat, Long> {

    // Tìm bản ghi thống kê của 1 user với 1 câu hỏi cụ thể
    Optional<UserQuestionStat> findByUserIdAndQuestionId(Long userId, Long questionId);

    // Tìm các câu hỏi làm sai nhiều nhất của user trong 1 đề thi (Sẽ dùng cho Giai đoạn 2)
    List<UserQuestionStat> findByUserIdAndExamIdAndWrongCountGreaterThanOrderByWrongCountDesc(Long userId, Long examId, int wrongCountThreshold);
}