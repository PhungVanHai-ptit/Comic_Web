# 📖 Comic Web - Hệ Thống Đọc Truyện Tranh Trực Tuyến

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)
![MinIO](https://img.shields.io/badge/MinIO-Object%20Storage-red.svg)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Template%20Engine-blueviolet.svg)

Comic Web là một nền tảng đọc truyện tranh trực tuyến hiện đại, được xây dựng trên nền tảng Spring Boot. Dự án hỗ trợ quản lý truyện, chương, người dùng và tích hợp lưu trữ hình ảnh thông qua MinIO.

---

## ✨ Tính Năng Chính

### 👤 Cho Người Dùng
- **Duyệt & Tìm Kiếm:** Tìm kiếm truyện theo tên, trạng thái hoặc thể loại.
- **Đọc Truyện:** Giao diện đọc truyện mượt mà, hỗ trợ chuyển chương nhanh chóng.
- **Tài Khoản:** Đăng ký, đăng nhập, cập nhật thông tin cá nhân và ảnh đại diện.
- **Bảo Mật:** Xác thực OTP qua Email khi đăng ký hoặc quên mật khẩu.
- **Tương Tác:** Bình luận và đánh giá truyện (nếu có).

### 🛠️ Cho Quản Trị Viên (Admin)
- **Quản Lý Truyện:** Thêm, sửa, xóa truyện tranh, quản lý ảnh bìa.
- **Quản Lý Chương:** Cập nhật nội dung chương, upload nhiều ảnh cùng lúc.
- **Quản Lý Thể Loại:** Phân loại truyện theo các chủ đề khác nhau.
- **Quản Lý Người Dùng:** Kiểm soát quyền hạn và trạng thái hoạt động của thành viên.
- **Quản Lý Bình Luận:** Kiểm duyệt các nội dung thảo luận trên hệ thống.

---

## 🚀 Công Nghệ Sử Dụng

- **Backend:** Java 21, Spring Boot 3.3.5
- **Security:** Spring Security (Xác thực & Phân quyền)
- **Database:** MySQL 8.0
- **Storage:** MinIO (Lưu trữ ảnh truyện và avatar)
- **Template Engine:** Thymeleaf
- **Tooling:** Lombok, Maven, Spring Data JPA, Spring Mail
- **Frontend:** HTML5, CSS3, JavaScript (Vanilla JS)

---

## 🛠️ Hướng Dẫn Cài Đặt

### 📋 Yêu Cầu Hệ Thống
- **JDK 21** trở lên.
- **MySQL Server** (đang chạy cổng 3306).
- **MinIO Server** (đang chạy cổng 9000).
- **Maven** (đã tích hợp sẵn `mvnw`).

### 1. Cấu Hình Cơ Sở Dữ Liệu
Tạo database có tên `comic_reader_db` trong MySQL:
```sql
CREATE DATABASE comic_reader_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Cấu Hình Ứng Dụng
Dự án sử dụng cơ chế Profile của Spring Boot để bảo mật thông tin.
- **`src/main/resources/application.properties`**: Chứa cấu hình chung và các giá trị mặc định. **Không** lưu mật khẩu thật ở đây.
- **`src/main/resources/application-local.properties`**: (Đã được gitignore) Bạn cần tạo file này trên máy cục bộ và điền thông tin thật của mình:
  ```properties
  spring.datasource.username=root
  spring.datasource.password=your_db_password
  spring.mail.username=your_email@gmail.com
  spring.mail.password=your_app_password
  minio.access-key=minioadmin
  minio.secret-key=minioadmin
  ```

> [!NOTE]
> Mặc định dự án đã tự động bao gồm profile `local` (được cấu hình trong `application.properties`). Chỉ cần file `application-local.properties` tồn tại, các cấu hình sẽ được áp dụng.

### 3. Cấu Hình MinIO
- Tạo 2 bucket trong MinIO: `avatars` và `comics`.
- Chỉnh sửa quyền truy cập (Access Policy) là `Public` để hiển thị được hình ảnh.

---

## 🏃 Chạy Ứng Dụng

Sử dụng Maven để chạy dự án với profile `local`:

```bash
# Cách 1: Chạy trực tiếp (profile local đã được include mặc định)
./mvnw spring-boot:run

# Cách 2: Kích hoạt profile local một cách tường minh
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
Sau đó truy cập: [http://localhost:8080](http://localhost:8080)

---

## 📁 Cấu Trúc Dự Án
```text
src/main/java/com/haiphung/comic_web/
├── config/        # Cấu hình Security, MinIO, Mail...
├── controller/    # Xử lý Request (User & Admin)
├── dto/           # Data Transfer Objects
├── entity/        # Định nghĩa các bảng Database
├── repository/    # Giao tiếp với Database
└── service/       # Xử lý logic nghiệp vụ
```

## 🤝 Tác Giả
- **Họ và tên:** Phùng Văn Hải
- **Mã sinh viên:** B23DCCN281
- **Email:** [phamtungnui@gmail.com](mailto:phamtungnui@gmail.com)

---
*Dự án được phát triển nhằm mục đích học tập và xây dựng nền tảng web hoàn chỉnh.*