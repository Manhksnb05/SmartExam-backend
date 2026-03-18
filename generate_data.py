import pandas as pd
import numpy as np
import random

# Thiết lập seed để kết quả sinh ra luôn cố định mỗi lần chạy (dễ debug)
np.random.seed(42)
random.seed(42)

num_samples = 5000
data = []

print("Đang tiến hành sinh 5.000 dữ liệu hành vi người dùng...")

for _ in range(num_samples):
    # Tỉ lệ phân bố học sinh trong thực tế: 30% Yếu, 45% Trung bình/Khá, 25% Giỏi
    level = np.random.choice([1, 2, 3], p=[0.30, 0.45, 0.25])

    if level == 1:  # Level Yếu
        score = np.random.uniform(0.0, 5.0)
        # 80% làm rất lâu (300s - 900s), 20% đánh lụi cực nhanh (60s - 150s)
        if random.random() < 0.8:
            time_taken = np.random.uniform(300, 900)
        else:
            time_taken = np.random.uniform(60, 150)

    elif level == 2:  # Level Trung bình/Khá
        score = np.random.uniform(5.0, 8.0)
        # Làm bài nghiêm túc, tốn thời gian suy nghĩ (400s - 800s)
        time_taken = np.random.uniform(400, 800)

    else:  # Level Giỏi
        score = np.random.uniform(8.0, 10.0)
        # Suy nghĩ sắc bén, giải quyết nhanh gọn (200s - 450s)
        time_taken = np.random.uniform(200, 450)

    # Tính toán thêm cột Tỉ lệ sai (để AI có thêm góc nhìn phân tích)
    wrong_ratio = (10.0 - score) / 10.0

    # Làm tròn số cho đẹp giống dữ liệu thực tế
    score = round(score, 2)
    time_taken = int(time_taken)
    wrong_ratio = round(wrong_ratio, 2)

    data.append([score, time_taken, wrong_ratio, level])

# Chuyển đổi list thành DataFrame
df = pd.DataFrame(data, columns=['recent_score', 'time_taken_seconds', 'wrong_ratio', 'user_level'])

# Xuất ra file CSV
csv_filename = 'user_dataset.csv'
df.to_csv(csv_filename, index=False)

print(f"✅ Hoàn tất! Đã lưu dữ liệu vào file: {csv_filename}")
print("Xem thử 5 dòng đầu tiên:")
print(df.head())