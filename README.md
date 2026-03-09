# 🚀 SMART EXAMS - HỆ THỐNG TẠO ĐỀ THI TRẮC NGHIỆM TỰ ĐỘNG TỪ TÀI LIỆU BẰNG TRÍ TUỆ NHÂN TẠO

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Version](https://img.shields.io/badge/version-v1.0.0-blue.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen.svg)]()
[![Cloud](https://img.shields.io/badge/Deployed_on-AWS-orange.svg)]()

Dự án tham gia cuộc thi **Website & AI Innovation Contest 2026** (Bảng B - Advanced Track).

## 📖 1. Giới thiệu Dự án
**Smart Exams** không chỉ là một hệ thống quản lý và thi trắc nghiệm thông thường. Chúng tôi tích hợp sâu trí tuệ nhân tạo (Generative AI) để tự động hóa quá trình biên soạn đề thi và đặc biệt là tính năng **Học tập thích ứng (Adaptive Learning)** - tự động phân tích lỗ hổng kiến thức của người học để cá nhân hóa đề thi, giúp sinh viên ôn tập hiệu quả nhất.

## 🌟 2. Tính năng nổi bật (Core & AI Features)
* **🤖 Trích xuất Đề thi Tự động (AI Content Generation):** Người dùng chỉ cần tải lên file Word/PDF (đề cương hoặc lý thuyết), AI (Google Gemini) sẽ tự động phân tích, định dạng và trích xuất thành bộ câu hỏi trắc nghiệm hoàn chỉnh.
* **🧠 Học tập thích ứng (Adaptive Learning):** Hệ thống theo dõi lịch sử làm bài, phân tích các câu hỏi người dùng thường xuyên trả lời sai. Khi tạo đề thi tùy chỉnh (Custom Exam), thuật toán Smart Shuffle sẽ ưu tiên bốc các câu sai, đồng thời AI sẽ tự động sinh thêm các câu hỏi MỚI TINH lấp vào lỗ hổng kiến thức đó.
* **Gia sư AI (AI Explainer):** Giải thích chi tiết từng câu hỏi đúng/sai ngay lập tức.
* **Quản lý Ngân hàng đề thi:** Hỗ trợ tính năng Public/Private để chia sẻ cộng đồng. Chấm điểm tức thì, thống kê lịch sử và xuất đề thi ra file Word (`.docx`).

## 💻 3. Công nghệ & Kiến trúc (Tech Stack)
Dự án được xây dựng theo mô hình 3-Layer chuẩn Enterprise, tách biệt rõ ràng giữa Backend và Frontend, được triển khai hoàn toàn trên nền tảng Điện toán đám mây.
* **Backend:** Java 17, Spring Boot 3, Spring Data JPA, Apache POI (xử lý file Word).
* **Frontend:** ReactJS
* **Database:** MySQL 8.0.
* **AI Engine:** Google Gemini 2.5 Flash API.
* **Cloud Deployment:** Triển khai trên AWS (EC2/RDS)

## 📦 4. Yêu cầu hệ thống & Phụ thuộc (Dependencies)
Để chạy dự án này, hệ thống của bạn cần cài đặt:
* [Java JDK 17+](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
* [Maven 3.8+](https://maven.apache.org/download.cgi)
* [MySQL Server 8.0](https://dev.mysql.com/downloads/)
* Tài khoản Google Gemini API Key.

**Các thư viện chính (Được quản lý qua `pom.xml`):**
* `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`
* `mysql-connector-j`
* `org.apache.poi` (Thao tác file Word/PDF)
* `lombok` (Giảm thiểu boilerplate code)

## ⚙️ 5. Hướng dẫn Cài đặt & Khởi chạy (Installation & Run)
Thực hiện các bước sau để chạy dự án tại môi trường Local:

**Bước 1: Clone mã nguồn**
```bash
git clone https://github.com/Manhksnb05/quiz-ai-backend
cd quiz-ai-backend
```

**Bước 2: Cấu hình biến môi trường (.env)**
Tạo một file mới tên là `.env` ở thư mục gốc của project (ngang hàng với `pom.xml`) và điền các thông tin Key của bạn vào theo mẫu sau:
` ` `env
GOOGLE_CLIENT_ID=nhập_client_id_của_bạn_tại_đây
GOOGLE_CLIENT_SECRET=nhập_client_secret_của_bạn_tại_đây
GEMINI_API_KEY=nhập_gemini_api_key_của_bạn_tại_đây
` ` `

**Bước 3: Chuẩn bị Database**
Tạo một schema mới trong MySQL của bạn:
` ` `sql
CREATE DATABASE quiz_app;
` ` `
*(Hệ thống Hibernate sẽ tự động generate các table khi chạy dự án)*. Lưu ý cấu hình lại username/password MySQL trong `src/main/resources/application.properties` nếu máy bạn dùng pass khác `123456`.

**Bước 4: Build và Chạy ứng dụng**
` ` `bash
mvn clean install
mvn spring-boot:run
` ` `
Hệ thống Backend sẽ khởi chạy tại: `http://localhost:8080`.

## 📈 6. Quản lý Phiên bản (Release & Changelog)
Mọi thay đổi cập nhật tính năng đều được ghi chú rõ ràng. Vui lòng xem chi tiết tại file CHANGELOG.md đi kèm trong mã nguồn.

* Phiên bản hiện tại: v1.0.0-Release

## 📄 7. Giấy phép mã nguồn mở (License)
Dự án được phát hành dưới Giấy phép MIT License (OSI-approved). Bạn có thể tự do sao chép, chỉnh sửa, phân phối và sử dụng cho mục đích thương mại. Xem chi tiết tại file LICENSE.

Dự án được phát triển bởi Nhóm gồm các thành viên: Nguyễn Đức Mạnh, Lê Hữu Tường, Nguyễn Minh Thành - Trường ĐH Công nghệ TP.HCM (HUTECH)