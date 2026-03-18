package com.hutech.quizbackend.service.Impl;

import com.hutech.quizbackend.entity.*;
import com.hutech.quizbackend.model.dto.*;
import com.hutech.quizbackend.model.request.CustomExamRequestDTO;
import com.hutech.quizbackend.model.request.QuestionRequestDTO;
import com.hutech.quizbackend.model.request.SubmitCustomExamRequestDTO;
import com.hutech.quizbackend.model.response.CustomExamResponseDTO;
import com.hutech.quizbackend.repository.*;
import com.hutech.quizbackend.service.ICustomExamService;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomExamService implements ICustomExamService {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private CustomExamRepository customExamRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private UserQuestionStatRepository userQuestionStatRepository;

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private AIService aiService;

    @Override
    @Transactional
    public CustomExamResponseDTO createCustomExam(CustomExamRequestDTO request) {
        Exam originExam = examRepository.findById(request.getOriginExamId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ đề gốc!"));

        String baseTitle = request.getTitle() != null && !request.getTitle().trim().isEmpty()
                ? request.getTitle().trim()
                : "Thi thử: " + originExam.getTitle();

        String customTitle = baseTitle;
        int suffix = 1;
        while (customExamRepository.existsByTitleAndOriginExamIdAndActiveTrue(customTitle, originExam.getId())) {
            customTitle = baseTitle + " (" + suffix + ")";
            suffix++;
        }

        int targetCount = request.getQuestionCount();
        List<Question> finalQuestions = new ArrayList<>();

        // ==============================================================
        // 🛠️ FIX 1: TỰ ĐỘNG DÒ TÌM USER ID NẾU FRONTEND QUÊN GỬI
        // ==============================================================
        Long validUserId = request.getUserId();
        if (validUserId == null && request.getUserEmail() != null) {
            User foundUser = userRepository.findAll().stream()
                    .filter(u -> request.getUserEmail().equals(u.getEmail()))
                    .findFirst()
                    .orElse(null);
            if (foundUser != null) {
                validUserId = foundUser.getId();
            }
        }

        // ==============================================================
        // KHỐI 1: AI #1 - ĐÁNH GIÁ TRÌNH ĐỘ NGƯỜI DÙNG
        // ==============================================================
        int userLevel = 2; // Mặc định là Khá/TB nếu chưa thi bao giờ
        if (validUserId != null) {
            Optional<Result> latestResultOpt = resultRepository.findFirstByUserIdOrderByCompletedAtDesc(validUserId);
            if (latestResultOpt.isPresent()) {
                Result latestResult = latestResultOpt.get();
                double recentScore = latestResult.getScore() != null ? latestResult.getScore() : 0.0;
                int timeTaken = latestResult.getTimeTaken() != null ? latestResult.getTimeTaken() : 0;

                double wrongRatio = 0.0;
                if (latestResult.getTotalQuestions() != null && latestResult.getTotalQuestions() > 0) {
                    int wrongAnswers = latestResult.getTotalQuestions() - (latestResult.getCorrectAnswers() != null ? latestResult.getCorrectAnswers() : 0);
                    wrongRatio = (double) wrongAnswers / latestResult.getTotalQuestions();
                }
                userLevel = aiService.predictUserLevel(recentScore, timeTaken, wrongRatio);
            }
        }

        // ==============================================================
        // KHỐI 2: AI #2 - GEMINI SINH CÂU HỎI BÙ LỖ HỔNG
        // ==============================================================
        List<UserQuestionStat> weakStats = new ArrayList<>();
        if (validUserId != null) {
            weakStats = userQuestionStatRepository
                    .findByUserIdAndExamIdAndWrongCountGreaterThanOrderByWrongCountDesc(validUserId, originExam.getId(), 0);
        }

        List<Question> weakQuestions = weakStats.stream().map(UserQuestionStat::getQuestion).limit(3).collect(Collectors.toList());
        finalQuestions.addAll(weakQuestions);

        if (!weakQuestions.isEmpty()) {
            List<String> weakContents = weakQuestions.stream().map(Question::getQuestion).collect(Collectors.toList());

            // Gọi Gemini sinh câu hỏi
            List<QuestionRequestDTO> aiGeneratedDTOs = geminiService.generateAdaptiveQuestions(weakContents, 2);

            for (QuestionRequestDTO dto : aiGeneratedDTOs) {
                Question newQ = new Question();
                newQ.setQuestion(dto.getQuestion());
                newQ.setOptions(dto.getOptions());
                String ans = dto.getAnswer() != null && !dto.getAnswer().isEmpty() ? dto.getAnswer().substring(0,1).toUpperCase() : "A";
                newQ.setAnswer(ans);

                int diff = 2; // Mặc định câu vá lỗi là Khó
                try {
                    diff = aiService.predictDifficulty(dto.getQuestion());
                } catch (Exception ignored) {}

                newQ.setDifficulty(diff);
                newQ.setExam(originExam);
                newQ = questionRepository.save(newQ);
                finalQuestions.add(newQ);
            }
        }

        // ==============================================================
        // KHỐI 4: THUẬT TOÁN PHÂN BỔ ĐỀ THI THEO TRÌNH ĐỘ (SMART SHUFFLE)
        // ==============================================================
        int remainingNeeded = targetCount - finalQuestions.size();
        if (remainingNeeded > 0) {
            List<Question> availablePool = new ArrayList<>(originExam.getQuestions());
            availablePool.removeAll(finalQuestions); // Bỏ các câu đã chọn
            Collections.shuffle(availablePool); // Lắc đều trước khi chọn

            List<Question> easyPool = availablePool.stream().filter(q -> q.getDifficulty() != null && q.getDifficulty() == 0).collect(Collectors.toList());
            List<Question> mediumPool = availablePool.stream().filter(q -> q.getDifficulty() != null && q.getDifficulty() == 1).collect(Collectors.toList());
            List<Question> hardPool = availablePool.stream().filter(q -> q.getDifficulty() != null && q.getDifficulty() == 2).collect(Collectors.toList());

            mediumPool.addAll(availablePool.stream().filter(q -> q.getDifficulty() == null).collect(Collectors.toList()));

            int easyNeeded = 0, medNeeded = 0, hardNeeded = 0;
            if (userLevel == 1) {
                easyNeeded = (int) Math.round(remainingNeeded * 0.7);
                medNeeded = remainingNeeded - easyNeeded;
            } else if (userLevel == 3) {
                hardNeeded = (int) Math.round(remainingNeeded * 0.6);
                medNeeded = (int) Math.round(remainingNeeded * 0.3);
                easyNeeded = remainingNeeded - hardNeeded - medNeeded;
            } else {
                easyNeeded = (int) Math.round(remainingNeeded * 0.3);
                hardNeeded = (int) Math.round(remainingNeeded * 0.3);
                medNeeded = remainingNeeded - easyNeeded - hardNeeded;
            }

            int deficit = 0;
            int easyToTake = Math.min(easyNeeded, easyPool.size());
            finalQuestions.addAll(easyPool.subList(0, easyToTake));
            deficit += (easyNeeded - easyToTake);
            availablePool.removeAll(easyPool.subList(0, easyToTake));

            int medToTake = Math.min(medNeeded, mediumPool.size());
            finalQuestions.addAll(mediumPool.subList(0, medToTake));
            deficit += (medNeeded - medToTake);
            availablePool.removeAll(mediumPool.subList(0, medToTake));

            int hardToTake = Math.min(hardNeeded, hardPool.size());
            finalQuestions.addAll(hardPool.subList(0, hardToTake));
            deficit += (hardNeeded - hardToTake);
            availablePool.removeAll(hardPool.subList(0, hardToTake));

            if (deficit > 0 && !availablePool.isEmpty()) {
                int finalFill = Math.min(deficit, availablePool.size());
                finalQuestions.addAll(availablePool.subList(0, finalFill));
            }
        }

        // Chốt sổ & Lưu trữ
        finalQuestions = finalQuestions.stream().limit(targetCount).collect(Collectors.toList());
        Collections.shuffle(finalQuestions);

        String selectedIds = finalQuestions.stream().map(q -> String.valueOf(q.getId())).collect(Collectors.joining(","));

        CustomExam customExam = new CustomExam();
        customExam.setTitle(customTitle);
        customExam.setTimeLimit(request.getTimeLimit());
        customExam.setQuestionCount(finalQuestions.size());
        customExam.setSelectedQuestionIds(selectedIds);
        customExam.setOriginExam(originExam);
        customExam.setUserEmail(request.getUserEmail());
        customExam.setActive(true);

        CustomExam savedExam = customExamRepository.save(customExam);

        CustomExamResponseDTO response = new CustomExamResponseDTO();
        response.setCustomExamId(savedExam.getId());
        response.setTitle(savedExam.getTitle());
        response.setTimeLimit(savedExam.getTimeLimit());
        response.setQuestionCount(savedExam.getQuestionCount());

        String msg = "Tạo đề AI hoàn tất! Chế độ: " + (userLevel == 1 ? "Cơ bản" : (userLevel == 3 ? "Thử thách" : "Tiêu chuẩn"));
        if (!weakQuestions.isEmpty()) msg += " (Đã kèm câu hỏi vá lỗ hổng)";
        response.setMessage(msg);

        return response;
    }

    @Transactional
    @Override
    public CustomExamResultDTO submitCustomExam(SubmitCustomExamRequestDTO request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người dùng!"));

        CustomExam customExam = customExamRepository.findById(request.getCustomExamId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài thi này!"));

        int correctCount = 0;
        int totalQuestions = request.getAnswers().size();

        for (AnswerSubmitDTO ans : request.getAnswers()) {
            Question q = questionRepository.findById(ans.getQuestionId()).orElse(null);

            if (q != null) {
                // Kiểm tra đáp án đúng hay sai
                boolean isCorrect = q.getAnswer() != null && ans.getSelectedOption() != null &&
                        q.getAnswer().trim().equalsIgnoreCase(ans.getSelectedOption().trim());

                if (isCorrect) {
                    correctCount++;
                } else {
                    // ==============================================================
                    // 🧠 AI FEATURE: GHI CHÉP CÂU LÀM SAI VÀO BẢNG THỐNG KÊ ĐIỂM YẾU
                    // ==============================================================
                    if (user != null) {
                        // Tìm xem câu này user đã từng làm sai trước đây chưa
                        UserQuestionStat stat = userQuestionStatRepository
                                .findByUserIdAndQuestionId(user.getId(), q.getId())
                                .orElse(null);

                        if (stat == null) {
                            // Nếu sai lần đầu -> Tạo mới
                            stat = new UserQuestionStat();
                            stat.setUser(user);
                            stat.setQuestion(q);
                            stat.setExam(customExam.getOriginExam()); // Lưu theo ID bộ đề gốc
                            stat.setWrongCount(1);
                        } else {
                            // Nếu đã từng sai rồi -> Tăng số lần sai lên
                            int currentWrong = stat.getWrongCount();
                            stat.setWrongCount(currentWrong + 1);
                        }

                        // Lưu vào DB
                        userQuestionStatRepository.save(stat);
                    }
                    // ==============================================================
                }
            }
        }

        double score = totalQuestions == 0 ? 0 : (double) correctCount / totalQuestions * 10;
        double finalScore = (double) Math.round(score * 100) / 100;

        // Lưu kết quả làm bài vào Database
        Result result = new Result();
        result.setUser(user);
        result.setCustomExam(customExam);
        result.setScore(finalScore);
        result.setCorrectAnswers(correctCount);
        result.setTotalQuestions(totalQuestions);
        result.setTimeTaken(request.getTimeTakenSeconds()); // Lưu thời gian làm bài
        result.setActive(true);

        Result savedResult = resultRepository.save(result);

        // Trả kết quả về cho Frontend
        CustomExamResultDTO resultDTO = new CustomExamResultDTO();
        resultDTO.setResultId(savedResult.getId());
        resultDTO.setTotalQuestions(totalQuestions);
        resultDTO.setCorrectAnswers(correctCount);
        resultDTO.setScore(finalScore);

        return resultDTO;
    }

    // ✅ FIX: Thêm qDto.setAnswer(q.getAnswer()) để frontend nhận đúng đáp án
    @Override
    public CustomExamTakeDTO getCustomExamForTake(Long customExamId) {
        CustomExam customExam = customExamRepository.findById(customExamId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi này!"));

        String idsStr = customExam.getSelectedQuestionIds();
        List<Long> questionIds = Arrays.stream(idsStr.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());

        List<Question> questionsFromDb = questionRepository.findAllById(questionIds);

        Map<Long, Question> questionMap = questionsFromDb.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<QuestionClientDTO> questionClientDTOs = new ArrayList<>();
        for (Long id : questionIds) {
            Question q = questionMap.get(id);
            if (q != null) {
                QuestionClientDTO qDto = new QuestionClientDTO();
                qDto.setId(q.getId());
                qDto.setQuestionContent(q.getQuestion());
                qDto.setOptions(q.getOptions());
                qDto.setAnswer(q.getAnswer()); // ✅ FIX: Set đáp án đúng
                questionClientDTOs.add(qDto);
            }
        }

        CustomExamTakeDTO response = new CustomExamTakeDTO();
        response.setCustomExamId(customExam.getId());
        response.setTitle(customExam.getTitle());
        response.setTimeLimit(customExam.getTimeLimit());
        response.setQuestions(questionClientDTOs);

        return response;
    }

    @Transactional
    @Override
    public void softDeleteCustomExams(List<Long> ids) {
        List<CustomExam> customExams = customExamRepository.findAllById(ids);

        if (customExams.isEmpty()) {
            throw new RuntimeException("Không tìm thấy đề thi tùy chỉnh nào để xóa!");
        }

        customExams.forEach(exam -> exam.setActive(false));
        customExamRepository.saveAll(customExams);
    }

    @Override
    public byte[] exportCustomExamToWord(Long customExamId) {
        CustomExam customExam = customExamRepository.findById(customExamId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi tùy chỉnh!"));

        String idsStr = customExam.getSelectedQuestionIds();
        List<Long> questionIds = Arrays.stream(idsStr.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());

        List<Question> questionsFromDb = questionRepository.findAllById(questionIds);
        Map<Long, Question> questionMap = questionsFromDb.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        List<Question> orderedQuestions = new ArrayList<>();
        for (Long id : questionIds) {
            if (questionMap.containsKey(id)) {
                orderedQuestions.add(questionMap.get(id));
            }
        }

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            titleRun.setText("SMART EXAMS - ĐỀ THI: " + customExam.getTitle().toUpperCase());

            XWPFParagraph meta = document.createParagraph();
            meta.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun metaRun = meta.createRun();
            metaRun.setItalic(true);
            metaRun.setText("Thời gian: " + customExam.getTimeLimit() + " phút | Tổng số câu: " + customExam.getQuestionCount());

            document.createParagraph();

            int index = 1;
            for (Question q : orderedQuestions) {
                XWPFParagraph p = document.createParagraph();
                XWPFRun qRun = p.createRun();
                qRun.setBold(true);
                qRun.setText("Câu " + (index++) + ": " + q.getQuestion());

                for (String opt : q.getOptions()) {
                    XWPFParagraph optP = document.createParagraph();
                    optP.setIndentationLeft(400);
                    XWPFRun optRun = optP.createRun();
                    optRun.setText(opt);
                }
                document.createParagraph();
            }

            document.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi trong quá trình tạo file Word: " + e.getMessage());
        }
    }

    @Override
    public List<CustomExamSummaryDTO> getUserCustomExams(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        List<CustomExam> exams = customExamRepository.findByUserEmailAndActiveTrueOrderByCreatedAtDesc(user.getEmail());

        List<CustomExamSummaryDTO> dtoList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (CustomExam ce : exams) {
            CustomExamSummaryDTO dto = new CustomExamSummaryDTO();
            dto.setId(ce.getId());
            dto.setTitle(ce.getTitle());
            dto.setTimeLimit(ce.getTimeLimit());
            dto.setQuestionCount(ce.getQuestionCount());
            if (ce.getCreatedAt() != null) {
                dto.setCreatedAt(ce.getCreatedAt().format(formatter));
            }
            dtoList.add(dto);
        }
        return dtoList;
    }
}