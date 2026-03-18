package com.hutech.quizbackend.repository;

import com.hutech.quizbackend.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

    // Tự động sinh SQL: Tìm theo UserId và sắp xếp thời gian giảm dần (mới nhất lên đầu)
    List<Result> findByUserIdOrderByCompletedAtDesc(Long userId);

    // Lấy bài thi gần nhất của user để AI phân tích hành vi
    java.util.Optional<Result> findFirstByUserIdOrderByCompletedAtDesc(Long userId);
}