package com.hutech.quizbackend.service.Impl;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class AIService {

    // Khai báo RestTemplate làm biến final để tối ưu
    private final RestTemplate restTemplate = new RestTemplate();

    public Integer predictDifficulty(String questionText) {
        // Địa chỉ con Python Flask đang chạy
        String url = "http://localhost:5000/predict";

        // Tạo body gửi đi (JSON)
        Map<String, String> request = new HashMap<>();
        request.put("question", questionText);

        try {
            // Gửi request POST sang Python và nhận kết quả
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && response.get("difficulty") != null) {
                return (Integer) response.get("difficulty");
            }
            return 0; // Mặc định là Dễ (0) nếu kết quả trống
        } catch (Exception e) {
            System.err.println("Lỗi kết nối AI Service: " + e.getMessage());
            return 0; // Trả về Dễ nếu không gọi được AI (đảm bảo app không bị crash)
        }
    }

    // Tính năng: Gọi AI Python dự đoán trình độ người dùng (1: Yếu, 2: Trung bình/Khá, 3: Giỏi)
    public Integer predictUserLevel(Double recentScore, Integer timeTakenSeconds, Double wrongRatio) {
        String url = "http://localhost:5000/predict-level";

        // Chuẩn bị gói hàng (JSON) để gửi sang Flask
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("recent_score", recentScore);
        requestBody.put("time_taken_seconds", timeTakenSeconds);
        requestBody.put("wrong_ratio", wrongRatio);

        try {
            // Gửi request POST và nhận kết quả
            Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

            if (response != null && response.get("user_level") != null) {
                return (Integer) response.get("user_level");
            }
            return 2; // Mặc định trả về Level 2 (Trung bình) nếu AI không trả về dữ liệu

        } catch (Exception e) {
            System.err.println("Lỗi kết nối đến AI Server khi dự đoán trình độ: " + e.getMessage());
            return 2; // Fallback an toàn: Nếu server Python sập, app Java vẫn chạy bình thường với Level 2
        }
    }
}