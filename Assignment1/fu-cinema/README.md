# FUCinemaBookingSystem – MSS301 Assignment 01

Hệ thống đặt vé xem phim theo kiến trúc **3 microservice + 1 API Gateway**, mỗi service dùng một loại database riêng (polyglot persistence).

| Service | Port | Database | Chức năng |
|---|---|---|---|
| `api-gateway` | **9000** | – | Điểm vào duy nhất: xác thực JWT (HS256), phân quyền theo role, chèn header `X-User-*`, định tuyến |
| `customer-service` | 8081 | SQL Server 2022 – `cinema_customer` | Đăng nhập (cấp JWT), đăng ký, profile, Admin CRUD customer |
| `movie-service` | 8082 | MongoDB 7 – `cinema_movie` | Thể loại, phòng chiếu, phim, suất chiếu |
| `booking-service` | 8083 | MySQL 8 – `cinema_booking` | Đặt vé, sơ đồ ghế, lịch sử, hủy vé, báo cáo doanh thu (gọi movie-service qua OpenFeign) |

**Công nghệ:** Java 21 · Spring Boot 4.1.0 · Spring Cloud 2025.1.3 (Gateway Server Web MVC, OpenFeign) · Spring Data JPA / MongoDB · Flyway · OAuth2 Resource Server · Docker Compose.

```
fu-cinema/
├── docker-compose.yml      SQL Server (+ sqlserver-init), MongoDB, MySQL
├── sqlserver/init.sql      tạo DB cinema_customer
├── mysql/init.sql          tạo DB cinema_booking
├── customer-service/       Flyway T-SQL (V1__init, V2__seed)
├── movie-service/          DataSeeder nạp dữ liệu mẫu (chỉ khi collection rỗng)
├── booking-service/        Flyway MySQL (V1__init)
├── api-gateway/
└── postman/                Collection + Environment + ảnh kết quả Collection Runner
```

## 1. Yêu cầu

- JDK 21, Maven 3.9+ (hoặc dùng `mvnw` có sẵn trong từng project)
- Docker Desktop (cấp ≥ 4 GB RAM)
- Postman Desktop

> ⚠️ Các cổng **1433, 27017, 3306, 9000** phải trống. Nếu máy có cài sẵn SQL Server / MongoDB / MySQL, hãy tắt trước khi chạy (PowerShell quyền Admin):
> ```powershell
> Stop-Service MSSQLSERVER, MongoDB, MySQL80
> ```

## 2. Thứ tự khởi động

**Bước 1 – Database** (trong thư mục `fu-cinema`):

```bash
docker compose up -d
docker compose ps -a
```

Chờ đến khi `cinema-sqlserver` là **healthy** và `cinema-sqlserver-init` là **Exited (0)**; `cinema-mongo`, `cinema-mysql` ở trạng thái **Up**.

**Bước 2 – 4 service**, chạy **theo đúng thứ tự**, mỗi service một terminal (hoặc Spring Boot Dashboard trong VS Code / tab Services trong IntelliJ):

| # | Lệnh | Chờ log |
|---|---|---|
| 1 | `cd customer-service && ./mvnw spring-boot:run` | `Tomcat started on port 8081` (lần đầu: Flyway áp 2 migration) |
| 2 | `cd movie-service && ./mvnw spring-boot:run` | `Tomcat started on port 8082` (lần đầu: `Seeded MongoDB: 5 genres, 4 rooms, 4 movies, 5 showtimes`) |
| 3 | `cd booking-service && ./mvnw spring-boot:run` | `Tomcat started on port 8083` |
| 4 | `cd api-gateway && ./mvnw spring-boot:run` | `Tomcat started on port 9000` |

> Trên Windows PowerShell dùng `.\mvnw spring-boot:run`.

**Bước 3 – Kiểm tra nhanh** (mọi request đều đi qua gateway `http://localhost:9000`):

```bash
curl http://localhost:9000/actuator/health          # {"status":"UP"}
curl http://localhost:9000/api/movies               # public
curl -X POST http://localhost:9000/api/auth/login -H "Content-Type: application/json" \
     -d "{\"email\":\"admin@fucinema.com\",\"password\":\"@@abc123@@\"}"
```

Tắt hệ thống: `Ctrl + C` ở từng service, sau đó `docker compose stop` (giữ dữ liệu) hoặc `docker compose down -v` + xóa thư mục `docker/` (xóa sạch dữ liệu để chạy lại từ đầu).

## 3. Tài khoản test

| Vai trò | Email | Mật khẩu | Ghi chú |
|---|---|---|---|
| Admin | `admin@fucinema.com` | `@@abc123@@` | Lưu trong `customer-service/application.properties` (không có trong DB) |
| Customer | `an@gmail.com` | `123456` | ID 1, ACTIVE |
| Customer | `binh@gmail.com` | `123456` | ID 2, ACTIVE |
| Customer | `chi@gmail.com` | `123456` | ID 3, **INACTIVE** → đăng nhập trả **403** |

Dữ liệu mẫu MongoDB có ID cố định, ví dụ suất chiếu `66f300000000000000000001` (Galaxy Rangers, Room 01, 8 × 10 ghế, 95.000đ).

## 4. Kiểm thử bằng Postman

1. **Import** 2 file trong `postman/`:
   - `FUCinemaBookingSystem.postman_collection.json` – 8 folder `01-Auth` → `08-Report`, **85 request**, mỗi request có Post-response test script
   - `FUCinema-Local.postman_environment.json` – environment `FUCinema-Local` (`gateway = http://localhost:9000`, token, id, seed id)
2. Chọn environment **FUCinema-Local** (góc trên bên phải).
3. Chuột phải collection → **Run collection** → giữ thứ tự 01 → 08 → **Run**.

Kết quả: **85 request – 216 assertion – 0 failed**. Collection chạy lặp lại nhiều lần vẫn pass (email/tên sinh ngẫu nhiên, ngày chiếu = hôm nay + 7).

![Kết quả Collection Runner](postman/collection-runner-result.png)

> Test làm tay **6.15 (BR14)**: tắt `movie-service` rồi đặt vé → `503 Movie service is unavailable`.

> **Ghi chú:** request **3.6** trong hướng dẫn gốc xóa genre "Hành động" (`66f0…001`) và kỳ vọng 409, nhưng trong dữ liệu seed genre này **không có phim nào** nên API trả 204 và xóa mất genre. Collection đã sửa thành xóa genre "Khoa học viễn tưởng" (`66f0…005`, có phim Galaxy Rangers) → **409** đúng BR03.

## 5. Lỗi thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| `Login failed for user 'sa'` (customer-service) | SQL Server cài trên máy đang chiếm cổng 1433 → `Stop-Service MSSQLSERVER` |
| `AuthenticationFailed` (movie-service) | MongoDB cài trên máy đang chiếm cổng 27017 → `Stop-Service MongoDB` |
| `ports are not available ... 3306` khi `docker compose up` | MySQL cài trên máy → `Stop-Service MySQL80` |
| `Port 9000 was already in use` (gateway) | Container/ứng dụng khác dùng cổng 9000 → dừng nó trước |
| `SSLHandshakeException: NotBefore` (booking-service) | Đồng hồ máy bị lùi sau khi tạo container MySQL → bật đồng bộ giờ, tạo lại container: `docker compose rm -sf mysql`, xóa `docker/mysql`, `docker compose up -d mysql` |
| Gọi thẳng 8081/8083 trả `401 Missing user context header` | Bình thường – các API cần user phải gọi qua gateway `:9000` |
