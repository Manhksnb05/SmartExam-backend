package com.hutech.quizbackend.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hutech.quizbackend.model.request.QuestionRequestDTO;
import com.hutech.quizbackend.model.response.AIExplainResponseDTO;
import com.hutech.quizbackend.entity.Exam;
import com.hutech.quizbackend.entity.Question;
import com.hutech.quizbackend.repository.ExamRepository;
import com.hutech.quizbackend.repository.QuestionRepository;
import com.hutech.quizbackend.service.IGeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class GeminiService implements IGeminiService {
    @Value("${gemini.api.key}")
    private String apiKey;

    @Autowired private ExamRepository examRepository;
    @Autowired private QuestionRepository questionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generateAndSaveQuiz(String promptText) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        try {
            // 1. TẠO PROMPT (KỸ NGHỆ NHẮC LỆNH)
            StringBuilder instruction = new StringBuilder();
            instruction.append("Bạn là một chuyên gia giáo dục. Hãy trích xuất hoặc tạo câu hỏi trắc nghiệm từ nội dung sau.\n\n");

            instruction.append("YÊU CẦU BẮT BUỘC ĐỂ XUẤT JSON:\n");
            instruction.append("1. 'options': Bắt buộc là mảng 4 phần tử. Mỗi phần tử BẮT ĐẦU BẰNG 'A. ', 'B. ', 'C. ', 'D. '. (Ví dụ: 'A. Lập trình'). Nếu văn bản gốc chưa có A, B, C, D, hãy tự thêm vào.\n");
            instruction.append("2. 'answer': BẮT BUỘC CHỈ LÀ 1 CHỮ CÁI IN HOA 'A', 'B', 'C', hoặc 'D'. Tuyệt đối không chứa nội dung văn bản dài.\n");
            instruction.append("3. Ưu tiên đáp án gốc: Nếu trong văn bản người dùng có đánh dấu đáp án đúng (ví dụ: *A, (Đ), bôi đậm, gạch chân, hoặc có bảng đáp án), BẮT BUỘC phải dùng đó làm đáp án chuẩn.\n\n");

            instruction.append("Cấu trúc JSON đầu ra BẮT BUỘC là một MẢNG:\n");
            instruction.append("[\n");
            instruction.append("  {\n");
            instruction.append("    \"question\": \"Nội dung câu hỏi?\",\n");
            instruction.append("    \"options\": [\"A. Lựa chọn 1\", \"B. Lựa chọn 2\", \"C. Lựa chọn 3\", \"D. Lựa chọn 4\"],\n");
            instruction.append("    \"answer\": \"A\"\n");
            instruction.append("  }\n");
            instruction.append("]\n\nChỉ trả về JSON hợp lệ, không giải thích thêm.\nNội dung cần xử lý: \n");

            Map<String, Object> requestBody = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", instruction.toString() + promptText)))));

            String rawResponse = new RestTemplate().postForObject(url, requestBody, String.class);
            JsonNode rootNode = objectMapper.readTree(rawResponse);
            String quizJson = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            // Xử lý Markdown nếu AI bao bọc JSON trong dấu ```json
            quizJson = quizJson.replaceAll("```json", "").replaceAll("```", "").trim();

            // 2. CHUYỂN JSON THÀNH NODE ĐỂ LƯU DATABASE
            JsonNode quizNode = objectMapper.readTree(quizJson);
            if (quizNode.isObject() && quizNode.has("questions")) {
                quizNode = quizNode.get("questions");
            }
            List<Map<String, Object>> list = objectMapper.convertValue(quizNode, List.class);

            // 3. LƯU VÀO DATABASE (Đã tích hợp Xóa mềm và Status)
            Exam exam = new Exam();
            exam.setTitle("Bộ đề AI trích xuất - " + LocalDateTime.now().withNano(0));
            exam.setTotalQuestions(list.size());
            exam.setActive(true);       // Thêm thuộc tính này theo logic mới của nhóm
            exam.setStatus("Private");  // Thêm thuộc tính này theo logic mới của nhóm

            exam = examRepository.save(exam);

            for (Map<String, Object> q : list) {
                Question question = new Question();
                String content = q.get("question") != null ? (String) q.get("question") : (String) q.get("content");

                if (content != null) {
                    question.setQuestion(content);

                    // PHÒNG THỦ (Defensive Programming): Đảm bảo answer luôn chỉ là 1 ký tự
                    String rawAnswer = (String) q.get("answer");
                    if (rawAnswer != null && !rawAnswer.isEmpty()) {
                        // Cắt lấy ký tự đầu tiên và in hoa. VD: Nếu AI lỡ trả "A. Biến" -> Cắt lấy chữ "A"
                        question.setAnswer(rawAnswer.trim().substring(0, 1).toUpperCase());
                    } else {
                        question.setAnswer("A"); // Fallback mặc định nếu AI lỗi
                    }

                    // Lưu Options (Danh sách A, B, C, D)
                    question.setOptions(new ArrayList<>((List<String>) q.get("options")));
                    question.setExam(exam);
                    questionRepository.save(question);
                }
            }

            return quizJson;

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Lỗi khi gọi AI hoặc lưu CSDL: " + e.getMessage() + "\"}";
        }
    }

    // Tính năng: Giải thích đáp án câu hỏi
    @Override
    public AIExplainResponseDTO explainQuestion(Long questionId, String userSelectedOption) {
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi"));

        boolean isUserCorrect = false;
        if (userSelectedOption != null && !userSelectedOption.isEmpty()) {
            isUserCorrect = q.getAnswer().trim().equalsIgnoreCase(userSelectedOption.trim());
        }

        // 1. Tạo Prompt ép AI trả về đúng cấu trúc JSON
        StringBuilder prompt = new StringBuilder();
        prompt.append("Bạn là một giáo viên IT tận tâm. Hãy giải thích câu hỏi trắc nghiệm sau.\n");
        prompt.append("Câu hỏi: ").append(q.getQuestion()).append("\n");
        prompt.append("Các đáp án: ").append(String.join(", ", q.getOptions())).append("\n");
        prompt.append("Đáp án đúng: ").append(q.getAnswer()).append("\n");
        prompt.append("Học sinh chọn: ").append(userSelectedOption != null ? userSelectedOption : "Không chọn").append("\n");

        prompt.append("\nQUAN TRỌNG: Bạn BẮT BUỘC phải trả về kết quả bằng ĐỊNH DẠNG JSON với cấu trúc chính xác như sau (không kèm theo bất kỳ văn bản nào khác ngoài JSON):\n");
        prompt.append("{\n");
        prompt.append("  \"greeting\": \"Lời chào thân thiện và nhận xét ngắn gọn việc học sinh làm đúng hay sai\",\n");
        prompt.append("  \"isCorrect\": ").append(isUserCorrect).append(",\n");
        prompt.append("  \"coreExplanation\": \"Giải thích đi thẳng vào vấn đề tại sao đáp án đúng lại chính xác\",\n");
        prompt.append("  \"details\": [\"Phân tích đáp án A sai ở đâu\", \"Phân tích đáp án B...\", \"Thêm kiến thức mở rộng (nếu có)\"],\n");
        prompt.append("  \"advice\": \"Một câu động viên hoặc lời khuyên ghi nhớ\"\n");
        prompt.append("}");

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        try {
            Map<String, Object> requestBody = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt.toString())))));
            String rawResponse = new RestTemplate().postForObject(url, requestBody, String.class);

            // Lấy chuỗi phản hồi từ AI
            JsonNode rootNode = objectMapper.readTree(rawResponse);
            String aiText = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            // Xóa các ký tự đánh dấu markdown JSON (```json ... ```) nếu AI có lỡ bọc vào
            aiText = aiText.replaceAll("```json", "").replaceAll("```", "").trim();

            // Chuyển đổi chuỗi JSON của AI thành Object DTO của chúng ta
            AIExplainResponseDTO responseDTO = objectMapper.readValue(aiText, AIExplainResponseDTO.class);
            return responseDTO;

        } catch (Exception e) {
            e.printStackTrace();
            // Xử lý lỗi trả về một DTO mặc định để app không bị sập
            AIExplainResponseDTO errorResponse = new AIExplainResponseDTO();
            errorResponse.setGreeting("Lỗi kết nối Gia sư AI.");
            errorResponse.setCoreExplanation("Hệ thống đang quá tải, vui lòng thử lại sau: " + e.getMessage());
            return errorResponse;
        }
    }

    // Tính năng: AI sinh câu hỏi thích ứng (Adaptive Learning)
    @Override
    public List<QuestionRequestDTO> generateAdaptiveQuestions(List<String> weakQuestions, int count) {
        if (weakQuestions == null || weakQuestions.isEmpty()) return new ArrayList<>();

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("Bạn là giáo viên IT. Một học sinh thường xuyên làm sai các câu hỏi sau:\n");
            for (int i = 0; i < weakQuestions.size(); i++) {
                prompt.append(i + 1).append(". ").append(weakQuestions.get(i)).append("\n");
            }
            prompt.append("\nHãy sinh ra ĐÚNG ").append(count).append(" câu hỏi trắc nghiệm HOÀN TOÀN MỚI kiểm tra cùng phạm vi kiến thức với các câu trên để học sinh luyện tập thêm.\n");

            // Ép khuôn JSON nghiêm ngặt
            prompt.append("BẮT BUỘC trả về mảng JSON định dạng sau (không giải thích thêm):\n");
            prompt.append("[\n  {\n    \"question\": \"...\",\n    \"options\": [\"A. ...\", \"B. ...\", \"C. ...\", \"D. ...\"],\n    \"answer\": \"A\"\n  }\n]");

            Map<String, Object> requestBody = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt.toString())))));
            String rawResponse = new RestTemplate().postForObject(url, requestBody, String.class);

            JsonNode rootNode = objectMapper.readTree(rawResponse);
            String aiText = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            aiText = aiText.replaceAll("```json", "").replaceAll("```", "").trim();

            JsonNode arrayNode = objectMapper.readTree(aiText);
            // Chuyển JSON thành List DTO
            return objectMapper.convertValue(arrayNode, objectMapper.getTypeFactory().constructCollectionType(List.class, QuestionRequestDTO.class));

        } catch (Exception e) {
            System.err.println("Lỗi khi AI sinh câu hỏi thích ứng: " + e.getMessage());
            return new ArrayList<>(); // Nếu AI sập, trả về mảng rỗng để luồng code không bị chết (Fallback)
        }
    }
}