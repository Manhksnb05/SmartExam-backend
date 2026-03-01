package com.hutech.quizbackend.repository;

import com.hutech.quizbackend.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    // 1. Tìm tất cả câu hỏi của 1 bộ đề (Dùng cho Yêu cầu 3)
    List<Question> findByExamId(Long examId);

    // 2. LẤY NGẪU NHIÊN N CÂU HỎI (Phục vụ Yêu cầu 2)
    @Query(value = "SELECT * FROM questions WHERE exam_id = ?1 ORDER BY RAND() LIMIT ?2", nativeQuery = true)
    List<Question> findRandomQuestionsByExamId(Long examId, int limit);
}