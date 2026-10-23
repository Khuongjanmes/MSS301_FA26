# Hướng dẫn test KB1 & KB2 – Order Saga (Orchestration)

Tài liệu hướng dẫn test step-by-step 2 kịch bản ở mục 6.3 của `Saga_guide.md`:

- **KB1 – Happy path**: đặt hàng thành công.
- **KB2 – Thanh toán thất bại (402)**: Saga hoàn tác kho rồi huỷ đơn.

Đây là 2 kịch bản dễ nhất vì **không cần tắt service hay đổi cấu hình**, chỉ cần gửi request bằng Postman.

---

## 0. Chuẩn bị

### 0.1. Reset dữ liệu về ban đầu

Chạy trong thư mục `saga-demo` (PowerShell):

```powershell
docker compose down -v
$env:PAYMENT_DELAY_MS=0; docker compose up -d
docker compose ps        # chờ 3 DB (healthy), 3 service Up
```

> Phải reset vì dữ liệu nằm trong Docker volume. Sau nhiều lần chạy, ví C1 có thể không còn đủ 2000 → KB1 cũng ra 402.

Kiểm tra dữ liệu ban đầu (mở trên trình duyệt):

| URL | Kết quả mong đợi |
|---|---|
| `http://localhost:8082/api/inventory/products` | P1 = 10, P2 = 5 |
| `http://localhost:8083/api/payments/wallets` | C1 = 5000, C2 = 100 |

### 0.2. Mở terminal xem log

```powershell
# Terminal 1 – log orchestrator
docker compose logs -f order-service
# Terminal 2 – log inventory + payment
docker compose logs -f inventory-service payment-service
```

### 0.3. Postman

Import `Saga_Postman_Collection.json`. Gửi các request **theo thứ tự**, chạy **KB1 trước KB2**.

---

## 1. KB1 – Happy path

**Mục tiêu:** C1 mua 2 Laptop (P1), giá 2000 → cả 3 bước thành công.

### 1.1. Quy trình

| # | Request | Mục đích |
|---|---|---|
| 1 | `[KB1] 1. Snapshot kho & ví` | Lưu tồn kho / số dư **trước** khi đặt |
| 2 | `[KB1] 2. Đặt hàng` – `POST /api/orders`<br>`{"customerId":"C1","productId":"P1","quantity":2,"amount":2000}` | Chạy Saga |
| 3 | `[KB1] 3. Kiểm tra kho & ví` | So sánh chênh lệch |
| 4 | `[KB1] 4. Saga logs` – `GET /api/orders/{id}/saga-logs` | Xem từng bước Saga |

### 1.2. Kết quả mong đợi

**Response bước 2:**

```json
{ "id": 1, "customerId": "C1", "productId": "P1", "quantity": 2, "amount": 2000.0,
  "status": "CONFIRMED", "sagaStatus": "COMPLETED", "failureReason": null }
```

Test Results: `HTTP 200`, `Order status = CONFIRMED`, `Saga status = COMPLETED`, `Không có failureReason` – tất cả xanh.

**Kho & ví (bước 3):** `Tồn kho P1: 10 -> 8`, `Số dư C1: 5000 -> 3000`.

**Saga logs (bước 4):**

| step | result | message |
|---|---|---|
| CREATE_ORDER | SUCCESS | Order PENDING |
| RESERVE_INVENTORY | SUCCESS | Đã giữ 2 P1 |
| PROCESS_PAYMENT | SUCCESS | Đã thu 2000.0 |
| CONFIRM_ORDER | SUCCESS | Saga COMPLETED |

**Log container:**

```
inventory-service | RESERVE order=1 product=P1 qty=2 -> stock còn 8
payment-service   | PAY order=1 customer=C1 amount=2000.0 -> balance còn 3000.0
```

order-service **không có WARN** (Saga không lỗi).

### 1.3. Giải thích

`createPendingOrder` → `reserve` → `pay` → `confirmOrder` đều thành công. Stack compensation có `[release, refund]` nhưng không được dùng.

---

## 2. KB2 – Thanh toán thất bại (402) → compensate kho

**Mục tiêu:** C2 (ví 100) mua 1 Laptop giá 1000 → payment lỗi → Saga **trả lại hàng** rồi **huỷ đơn**.

### 2.1. Quy trình

| # | Request | Ghi chú |
|---|---|---|
| 1 | `[KB2] 1. Snapshot kho & ví` | P1 = 8 (sau KB1), C2 = 100 |
| 2 | `[KB2] 2. Đặt hàng`<br>`{"customerId":"C2","productId":"P1","quantity":1,"amount":1000}` | |
| 3 | `[KB2] 3. Kiểm tra kho & ví` | |
| 4 | `[KB2] 4. Saga logs` | |
| 5 | `[KB2] 5. Reservation đã RELEASED` – `GET /api/inventory/reservations` | |

### 2.2. Kết quả mong đợi

**Response bước 2:**

```json
{ "id": 2, "customerId": "C2", "productId": "P1", "quantity": 1, "amount": 1000.0,
  "status": "CANCELLED", "sagaStatus": "COMPENSATED",
  "failureReason": "402 - Số dư không đủ: còn 100.0, cần 1000.0" }
```

> HTTP status vẫn là **200** – lỗi nghiệp vụ thể hiện qua `status` / `sagaStatus` / `failureReason`.

Test Results: `Order status = CANCELLED`, `Saga status = COMPENSATED`, `failureReason bắt đầu bằng 402` – tất cả xanh.

**Kho & ví (bước 3):** `Tồn kho P1: 8 -> 8` (trừ rồi trả lại), `Số dư C2: 100 -> 100`.

**Saga logs (bước 4):**

| step | result | message |
|---|---|---|
| CREATE_ORDER | SUCCESS | Order PENDING |
| RESERVE_INVENTORY | SUCCESS | Đã giữ 1 P1 |
| SAGA | FAILED | 402 - Số dư không đủ: còn 100.0, cần 1000.0 |
| COMPENSATION | SUCCESS | Lần thử 1 |
| CANCEL_ORDER | SUCCESS | Saga COMPENSATED |

**Reservation (bước 5):**

```json
{ "orderId": 2, "productId": "P1", "quantity": 1, "status": "RELEASED" }
```

**Log container:**

```
order-service     | WARN  Saga order=2 thất bại: 402 - Số dư không đủ: còn 100.0, cần 1000.0
inventory-service | RESERVE order=2 product=P1 qty=1 -> stock còn 7
inventory-service | RELEASE order=2 -> trả lại 1 P1, stock = 8
```

payment-service **không in dòng `PAY`** vì đã ném 402 trước khi trừ tiền → ví và bảng `payment` không đổi.

**Xem trực tiếp trong DB (tuỳ chọn):**

```powershell
docker exec saga-mysql mysql -uroot -proot orderdb -e "SELECT id,status,saga_status,failure_reason FROM orders;"
docker exec saga-mongodb mongosh inventorydb --quiet --eval "db.reservation.find()"
```

### 2.3. Giải thích

1. `reserve` thành công → đẩy `release` vào stack.
2. `pay` trả về 402 → `HttpClientErrorException` → vào `catch`.
3. `markFailed` (`FAILED`) → `COMPENSATING` → pop `release` và chạy (thành công ở lần thử 1).
4. `cancelOrder` → `CANCELLED / COMPENSATED`.

Stack **không có `refund`** vì `pay` chưa thành công – chỉ hoàn tác những bước **đã** thành công.

---

## 3. So sánh KB1 và KB2

| | KB1 | KB2 |
|---|---|---|
| Lỗi ở bước | – | 3 (Payment 402) |
| `status / sagaStatus` | CONFIRMED / COMPLETED | CANCELLED / COMPENSATED |
| Kho P1 | −2 | 0 (trừ rồi trả) |
| Ví | C1 −2000 | C2 không đổi |
| Số dòng saga-log | 4 | 5 (thêm SAGA FAILED, COMPENSATION) |
| Log inventory | RESERVE | RESERVE → RELEASE |
| Log payment | PAY | (không có) |

---

## 4. Hỏi – đáp

### 4.1. Saga có những trạng thái nào?

6 trạng thái `SagaStatus` (Slide 27), tách biệt với `OrderStatus` (`PENDING | CONFIRMED | CANCELLED`):

| SagaStatus | Ý nghĩa | Được set ở |
|---|---|---|
| `PENDING` | Vừa tạo, chưa chạy bước nào | `createPendingOrder` |
| `EXECUTING` | Đang chạy các bước | `updateSagaStatus(EXECUTING)` |
| `COMPLETED` | Mọi bước thành công | `confirmOrder` |
| `FAILED` | Một bước lỗi, chưa hoàn tác | `markFailed` |
| `COMPENSATING` | Đang chạy compensation | `compensate()` |
| `COMPENSATED` | Đã hoàn tác xong | `cancelOrder` |

```
Thành công: PENDING → EXECUTING → COMPLETED                        (order CONFIRMED)
Thất bại:   PENDING → EXECUTING → FAILED → COMPENSATING → COMPENSATED  (order CANCELLED)
KB8:        ... → COMPENSATING (dừng lại – compensation cũng lỗi)    (order PENDING)
```

### 4.2. `release` có thể trả sai số lượng không?

Không. `release` **không nhận quantity từ bên ngoài**, mà đọc lại từ bản ghi `reservation` đã lưu khi reserve:

```java
Reservation r = reservationRepository.findById(orderId);   // _id = orderId
product.setStock(product.getStock() + r.getQuantity());    // trả đúng số đã giữ
```

Mỗi order release **đúng số lượng nó đã reserve** – yêu cầu *Reversible* (Slide 25). Nếu thấy log có `qty=2` và `trả lại 1`, đó là **2 order khác nhau**, hãy so `order=<id>`.

### 4.3. Vì sao KB2 chỉ có "COMPENSATION SUCCESS Lần thử 1" mà không phải 3 lần?

"3 lần" là **số lần thử tối đa**. `RetryTemplate` chỉ thử lại khi lần trước **thất bại**:

```java
RetryPolicy.builder()
        .includes(RestClientException.class)
        .maxRetries(2)                    // 1 lần đầu + tối đa 2 lần thử lại = tối đa 3 lần
        .delay(Duration.ofSeconds(1))
        .build();
```

| Kịch bản | inventory lúc `release` | Số lần thử | Saga log |
|---|---|---|---|
| KB2, KB5, KB6 | sống | 1 | `COMPENSATION SUCCESS Lần thử 1` |
| KB8 | đã tắt | 3 | 3 dòng `COMPENSATION FAILED` |

Muốn thấy retry, chạy KB8 (tắt cả payment và inventory trong lúc Saga chờ 15 giây).
