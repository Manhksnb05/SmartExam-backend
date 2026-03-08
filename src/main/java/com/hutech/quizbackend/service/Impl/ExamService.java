package com.hutech.quizbackend.service.Impl;

import com.hutech.quizbackend.entity.Exam;
import com.hutech.quizbackend.entity.Question;
import com.hutech.quizbackend.entity.User;
import com.hutech.quizbackend.entity.UserQuestionStat;
import com.hutech.quizbackend.model.dto.*;
import com.hutech.quizbackend.model.request.QuestionRequestDTO;
import com.hutech.quizbackend.model.request.SaveExamRequestDTO;
import com.hutech.quizbackend.repository.ExamRepository;
import com.hutech.quizbackend.repository.QuestionRepository;
import com.hutech.quizbackend.repository.UserQuestionStatRepository;
import com.hutech.quizbackend.repository.UserRepository;
import com.hutech.quizbackend.service.IExamService;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExamService implements IExamService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private UserQuestionStatRepository userQuestionStatRepository;

    @Autowired
    private UserRepository userRepository;

    // 1. Logic lấy danh sách bộ đề (Dashboard)
    @Override
    public List<ExamSummaryDTO> getAllActiveExams() {
        List<Exam> exams = examRepository.findByActiveTrueOrderByCreatedAtDesc();
        List<ExamSummaryDTO> dtoList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Exam exam : exams) {
            ExamSummaryDTO dto = new ExamSummaryDTO();
            dto.setId(exam.getId());
            dto.setTitle(exam.getTitle());
            dto.setTotalQuestions(exam.getTotalQuestions());
            dto.setStatus(exam.getStatus());
            if (exam.getCreatedAt() != null) {
                dto.setCreatedAt(exam.getCreatedAt().format(formatter));
            }
            dtoList.add(dto);
        }
        return dtoList;
    }

    // 2. Logic Lưu bộ đề (Sau khi AI trích xuất)
    @Transactional
    @Override
    public ExamSummaryDTO saveExtractedExam(SaveExamRequestDTO request) {
        Exam exam = new Exam();
        exam.setTitle(request.getTitle());
        exam.setTotalQuestions(request.getQuestions() != null ? request.getQuestions().size() : 0);
        exam.setActive(true); // Mặc định không bị xóa
        exam.setStatus("Private"); // Mặc định là Private

        // Chuyển từ DTO sang Entity và gán quan hệ 2 chiều
        List<Question> questions = new ArrayList<>();
        if (request.getQuestions() != null) {
            for (QuestionRequestDTO qDto : request.getQuestions()) {
                Question q = new Question();
                q.setQuestion(qDto.getQuestion());
                q.setOptions(qDto.getOptions());
                q.setAnswer(qDto.getAnswer());
                q.setExam(exam); // Gán ID của bộ đề cho câu hỏi (Quan hệ ManyToOne)
                questions.add(q);
            }
        }
        exam.setQuestions(questions);

        // Lưu vào DB
        Exam savedExam = examRepository.save(exam);

        // Trả về DTO cho Frontend biết đã lưu thành công
        ExamSummaryDTO responseDTO = new ExamSummaryDTO();
        responseDTO.setId(savedExam.getId());
        responseDTO.setTitle(savedExam.getTitle());
        return responseDTO;
    }

    // 3. Logic xuất file Word từ Bộ đề gốc (Exam)
    @Override
    public byte[] exportExamToWord(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ đề!"));

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Tạo tiêu đề
            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            titleRun.setText("HUTECH QUIZ - ĐỀ THI: " + exam.getTitle().toUpperCase());

            document.createParagraph(); // Dòng trống

            // Ghi nội dung câu hỏi
            int index = 1;
            for (Question q : exam.getQuestions()) {
                XWPFParagraph p = document.createParagraph();
                XWPFRun qRun = p.createRun();
                qRun.setBold(true);
                qRun.setText("Câu " + (index++) + ": " + q.getQuestion());

                for (String opt : q.getOptions()) {
                    XWPFParagraph optP = document.createParagraph();
                    optP.setIndentationLeft(400); // Thụt đầu dòng
                    XWPFRun optRun = optP.createRun();
                    optRun.setText(opt);
                }
                document.createParagraph();
            }

            document.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi hệ thống khi tạo file Word: " + e.getMessage());
        }
    }

    // Tính năng: Chấm điểm Luyện đề & Lưu thống kê (Adaptive Learning - Phase 1)
    @Transactional
    @Override
    public PracticeResultDTO checkPracticeAnswers(PracticeRequestDTO request) {

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User"));
        Exam exam = examRepository.findById(request.getExamId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Exam"));

        int correctCount = 0;
        List<PracticeFeedbackDTO> details = new ArrayList<>();

        for (PracticeAnswerDTO ans : request.getAnswers()) {
            PracticeFeedbackDTO feedback = new PracticeFeedbackDTO();
            feedback.setQuestionId(ans.getQuestionId());

            Question q = questionRepository.findById(ans.getQuestionId()).orElse(null);

            if (q != null && q.getAnswer() != null && ans.getSelectedOption() != null) {
                boolean isCorrect = q.getAnswer().trim().equalsIgnoreCase(ans.getSelectedOption().trim());
                feedback.setCorrect(isCorrect);
                if (isCorrect) correctCount++;

                // --- BẮT ĐẦU THEO DÕI LOGIC (TRACKING) ---
                // Tìm xem user này đã từng làm câu này chưa. Nếu chưa thì tạo mới record.
                UserQuestionStat stat = userQuestionStatRepository
                        .findByUserIdAndQuestionId(user.getId(), q.getId())
                        .orElseGet(() -> {
                            UserQuestionStat newStat = new UserQuestionStat();
                            newStat.setUser(user);
                            newStat.setQuestion(q);
                            newStat.setExam(exam);
                            return newStat;
                        });

                // Cập nhật số đếm
                if (isCorrect) {
                    stat.setCorrectCount(stat.getCorrectCount() + 1);
                } else {
                    stat.setWrongCount(stat.getWrongCount() + 1);
                }

                userQuestionStatRepository.save(stat); // Lưu vào Database
                // --- KẾT THÚC TRACKING ---

            } else {
                feedback.setCorrect(false);
            }
            details.add(feedback);
        }

        // Gom kết quả trả về cho Frontend hiển thị
        PracticeResultDTO result = new PracticeResultDTO();
        result.setTotalQuestions(request.getAnswers().size());
        result.setCorrectCount(correctCount);
        result.setIncorrectCount(request.getAnswers().size() - correctCount);
        result.setDetails(details);

        return result;
    }

    // Tính năng: Xóa mềm NHIỀU đề thi gốc cùng lúc
    @Transactional
    @Override
    public void softDeleteExams(List<Long> ids) {
        // Lấy tất cả các Exam có ID nằm trong danh sách truyền vào
        List<Exam> exams = examRepository.findAllById(ids);

        if (exams.isEmpty()) {
            throw new RuntimeException("Không tìm thấy bộ đề nào để xóa!");
        }

        // Đổi trạng thái active = false cho tất cả
        exams.forEach(exam -> exam.setActive(false));

        // saveAll() giúp lưu danh sách nhanh hơn rất nhiều so với dùng save() trong vòng lặp
        examRepository.saveAll(exams);
    }

    // Tính năng: Lấy danh sách bộ đề Public cho Ngân hàng đề thi
    @Override
    public List<ExamPublicDTO> getPublicExams() {
        // 1. Gọi Repository lấy danh sách (truyền chữ "Public" vào)
        List<Exam> exams = examRepository.findByStatusAndActiveTrueOrderByCreatedAtDesc("Public");

        // 2. Chuyển đổi từ Entity sang DTO
        List<ExamPublicDTO> publicExams = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Exam exam : exams) {
            ExamPublicDTO dto = new ExamPublicDTO();
            dto.setId(exam.getId());
            dto.setTitle(exam.getTitle());
            dto.setTotalQuestions(exam.getTotalQuestions());

            // Format ngày tháng
            if (exam.getCreatedAt() != null) {
                dto.setCreatedAt(exam.getCreatedAt().format(formatter));
            }

            // Lấy tên người tạo (Nhờ mối quan hệ ManyToOne với User)
            if (exam.getUser() != null && exam.getUser().getName() != null) {
                dto.setCreatorName(exam.getUser().getName());
            } else {
                dto.setCreatorName("Người dùng ẩn danh");
            }

            publicExams.add(dto);
        }

        return publicExams;
    }

    // Tính năng: Xem chi tiết 1 bộ đề Public
    @Override
    public ExamDetailPublicDTO getPublicExamDetail(Long id) {
        // 1. Tìm đề thi (Bắt buộc phải là Public và chưa bị xóa)
        Exam exam = examRepository.findByIdAndStatusAndActiveTrue(id, "Public")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ đề hoặc bộ đề đang ở trạng thái Private!"));

        // 2. Chuyển đổi thông tin chung sang DTO
        ExamDetailPublicDTO dto = new ExamDetailPublicDTO();
        dto.setId(exam.getId());
        dto.setTitle(exam.getTitle());
        dto.setTotalQuestions(exam.getTotalQuestions());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        if (exam.getCreatedAt() != null) {
            dto.setCreatedAt(exam.getCreatedAt().format(formatter));
        }

        if (exam.getUser() != null && exam.getUser().getName() != null) {
            dto.setCreatorName(exam.getUser().getName());
        } else {
            dto.setCreatorName("Người dùng ẩn danh");
        }

        // 3. Chuyển đổi danh sách câu hỏi sang QuestionClientDTO để giấu đáp án đúng
        List<QuestionClientDTO> questionDTOs = exam.getQuestions().stream().map(q -> {
            QuestionClientDTO qDto = new QuestionClientDTO();
            qDto.setId(q.getId());
            qDto.setQuestionContent(q.getQuestion()); // Map vào trường questionContent của DTO
            qDto.setOptions(q.getOptions());
            return qDto;
        }).collect(Collectors.toList());

        dto.setQuestions(questionDTOs);

        return dto;
    }

    // Tính năng: Cập nhật trạng thái bộ đề (Private <-> Public)
    @Transactional
    @Override
    public String updateExamStatus(Long examId, String newStatus) {
        // 1. Kiểm tra tính hợp lệ của dữ liệu gửi lên
        if (newStatus == null || (!newStatus.equalsIgnoreCase("Public") && !newStatus.equalsIgnoreCase("Private"))) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ. Chỉ chấp nhận 'Public' hoặc 'Private'.");
        }

        // 2. Tìm bộ đề trong Database
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ đề với ID: " + examId));

        // 3. Kiểm tra xem bộ đề có bị xóa mềm không
        if (exam.getActive() != null && !exam.getActive()) {
            throw new RuntimeException("Bộ đề này đã bị xóa, không thể cập nhật trạng thái!");
        }

        // 4. Cập nhật trạng thái (Chuẩn hóa thành chữ cái đầu viết hoa để dữ liệu DB đồng nhất)
        String formattedStatus = newStatus.substring(0, 1).toUpperCase() + newStatus.substring(1).toLowerCase();
        exam.setStatus(formattedStatus);

        examRepository.save(exam);

        return "Đã cập nhật trạng thái bộ đề thành: " + formattedStatus;
    }
}