from flask import Flask, request, jsonify
import joblib
import os
import warnings

# Tắt các cảnh báo không cần thiết của thư viện AI cho console đỡ rác
warnings.filterwarnings("ignore")

app = Flask(__name__)

# ==========================================
# 1. LOAD MODEL CŨ: DỰ ĐOÁN ĐỘ KHÓ CÂU HỎI
# ==========================================
MODEL_PATH = "src/main/resources/models/difficulty_model_v1.pkl"
VECTORIZER_PATH = "src/main/resources/models/tfidf_vectorizer_v1.pkl"

try:
    model = joblib.load(MODEL_PATH)
    vectorizer = joblib.load(VECTORIZER_PATH)
    print("✅ Đã load thành công AI Dự đoán độ khó câu hỏi")
except Exception as e:
    print(f"⚠️ Lỗi load difficulty model: {e}")

# ==========================================
# 2. LOAD MODEL MỚI: ĐÁNH GIÁ TRÌNH ĐỘ USER
# ==========================================
LEVEL_MODEL_PATH = "src/main/resources/models/user_level_model.pkl"

try:
    level_model = joblib.load(LEVEL_MODEL_PATH)
    print("✅ Đã load thành công AI Đánh giá trình độ (user_level_model.pkl)")
except Exception as e:
    print(f"⚠️ Lỗi load level model: {e}")

# ==========================================
# 3. API CŨ: DỰ ĐOÁN ĐỘ KHÓ CÂU HỎI
# ==========================================
@app.route('/predict', methods=['POST'])
def predict():
    try:
        data = request.get_json()
        question = data.get('question', '')

        # AI dự đoán
        X_vec = vectorizer.transform([question])
        prediction = model.predict(X_vec)[0]

        # 0: Dễ, 1: Trung bình, 2: Khó
        return jsonify({'difficulty': int(prediction)})
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# ==========================================
# 4. API MỚI: DỰ ĐOÁN TRÌNH ĐỘ USER
# ==========================================
@app.route('/predict-level', methods=['POST'])
def predict_level():
    try:
        # Nhận cục JSON từ Java Spring Boot gửi sang
        data = request.get_json()

        # Lấy 3 chỉ số hành vi (Nếu không có thì mặc định là 0)
        recent_score = float(data.get('recent_score', 0))
        time_taken = float(data.get('time_taken_seconds', 0))
        wrong_ratio = float(data.get('wrong_ratio', 0))

        # Đưa cho AI dự đoán (Lưu ý: input phải là mảng 2 chiều [[...]])
        prediction = level_model.predict([[recent_score, time_taken, wrong_ratio]])

        # Kết quả trả về là 1 mảng [level], ta lấy phần tử đầu tiên ép kiểu về số nguyên (1, 2 hoặc 3)
        user_level = int(prediction[0])

        # Trả JSON về lại cho Java
        return jsonify({'user_level': user_level})

    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    print("🚀 AI Bridge đang chạy tại port 5000...")
    app.run(port=5000)