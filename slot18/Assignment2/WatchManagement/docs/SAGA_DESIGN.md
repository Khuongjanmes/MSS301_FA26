# Saga Design – Watch Management

> [TODO-4.1] Tài liệu thiết kế Saga, viết **trước** khi code MSBrand / MSWatch.

## 1. Participants

| Service | Vai trò | Database | Dữ liệu tham gia Saga |
|---|---|---|---|
| MSWatch | **Orchestrator** | DBWatch | `Watches`, `SagaStates`, `SagaLogs` |
| MSBrand | **Participant** | DBBrand | `BrandWatches` |

MSWatch gọi MSBrand qua Feign, trực tiếp cổng 8082, bằng API nội bộ (Gateway **không** route `/internal/**`):

| API nội bộ MSBrand | Dùng cho | Hành vi idempotent |
|---|---|---|
| `PUT /internal/brands/{brandId}/watches/{watchId}` | ASSIGN_BRAND, ASSIGN_BRAND_BACK | Upsert theo `watchId`: đã đúng brand thì không làm gì; brand không tồn tại → 404 |
| `DELETE /internal/brands/watches/{watchId}` | UNASSIGN_BRAND | Không có gì để gỡ → vẫn 200 |

## 2. Saga CREATE_WATCH

| # | Local transaction | Service / DB | Compensation | Idempotent? | Cách hiện thực idempotent |
|---|---|---|---|---|---|
| 1 | `CREATE_WATCH` – INSERT `Watches` | MSWatch / DBWatch | `DELETE_WATCH` | ✔ | `deleteIfExists(watchId)`: id không còn → bỏ qua |
| 2 | `ASSIGN_BRAND` – upsert `BrandWatches(brandId, watchId)` | MSBrand / DBBrand | `UNASSIGN_BRAND` | ✔ | DELETE theo `watchId`, không có dòng → 200 |

Thứ tự bù trừ (LIFO): `UNASSIGN_BRAND` → `DELETE_WATCH`.

## 3. Saga UPDATE_WATCH

Trước khi chạy, Orchestrator lưu **snapshot `before`** của watch (toàn bộ field, kể cả `brandId` cũ).

| # | Local transaction | Service / DB | Compensation | Idempotent? | Cách hiện thực idempotent |
|---|---|---|---|---|---|
| 1 | `UPDATE_WATCH` – ghi dữ liệu mới | MSWatch / DBWatch | `RESTORE_WATCH` | ✔ | Ghi đè bằng snapshot `before` (giá trị tuyệt đối, không cộng dồn) → chạy N lần vẫn ra cùng kết quả |
| 2 | `ASSIGN_BRAND` – chuyển watch sang brand mới | MSBrand / DBBrand | `ASSIGN_BRAND_BACK` | ✔ | `PUT /internal/brands/{oldBrandId}/watches/{id}` là upsert → gọi lặp vẫn an toàn |

Thứ tự bù trừ (LIFO): `ASSIGN_BRAND_BACK` → `RESTORE_WATCH`.

## 4. Saga DELETE_WATCH

Trước khi chạy, Orchestrator lưu `brandId` hiện tại của watch để có thể gán lại.

| # | Local transaction | Service / DB | Compensation | Idempotent? | Cách hiện thực idempotent |
|---|---|---|---|---|---|
| 1 | `UNASSIGN_BRAND` – gỡ watch khỏi brand | MSBrand / DBBrand | `ASSIGN_BRAND_BACK` | ✔ | Upsert `(oldBrandId, watchId)` |
| 2 | `DELETE_WATCH` – xoá cứng watch | MSWatch / DBWatch | — **PIVOT** | — | — |

### Vì sao `DELETE_WATCH` là pivot và đứng cuối

- Xoá cứng một dòng có `IDENTITY` thì **không khôi phục được đúng `WatchID` cũ**. INSERT lại sẽ sinh id mới, và mọi tham chiếu tới id cũ bị lệch. Vì vậy bước này **không có compensation**.
- Sau khi pivot commit, Saga **chỉ được đi tiếp**, không quay lui được nữa. Nên pivot phải là bước **cuối cùng**: lúc đó không còn bước nào sau nó có thể lỗi.
- Bước có rủi ro lỗi cao (gọi qua mạng sang MSBrand) được làm **trước** pivot, khi vẫn còn bù trừ được.
- Fault injection `X-Saga-Fail-After-Step: 2` cho Saga DELETE bị **bỏ qua** (log `FAULT SKIPPED`), Saga kết thúc `COMPLETED`.

## 5. State machine

```
PENDING ──► EXECUTING ──────────────────────────────► COMPLETED
                │ (một bước lỗi)
                ▼
             FAILED ──► COMPENSATING ──► COMPENSATED      (đã bù trừ xong, dữ liệu nhất quán)
                              │
                              └─ (compensation lỗi sau 3 lần retry) dừng ở COMPENSATING
                                 → cần xử lý thủ công
```

| Trạng thái | Ý nghĩa |
|---|---|
| `PENDING` | Đã tạo dòng `SagaStates`, chưa chạy bước nào |
| `EXECUTING` | Đang chạy các local transaction theo thứ tự |
| `COMPLETED` | Mọi bước (hoặc tới pivot) đã commit |
| `FAILED` | Một bước lỗi; ghi lại `failedStep` và `reason` |
| `COMPENSATING` | Đang bù trừ; nếu dừng ở đây nghĩa là compensation thất bại, dữ liệu có thể lệch |
| `COMPENSATED` | Mọi bước đã commit đều được bù trừ thành công |

## 6. Luồng thất bại: CREATE với brand 999

```
MSWatch (Orchestrator)                 DBWatch                 MSBrand / DBBrand
   │ PENDING → EXECUTING
   │ [1] CREATE_WATCH ───────────────► INSERT Watches (id=11) ✔ COMMIT
   │ [2] ASSIGN_BRAND ──── PUT /internal/brands/999/watches/11 ──► 404 Brand not found
   │      → FAILED   (failedStep = ASSIGN_BRAND)
   │      → COMPENSATING
   │ [C1] DELETE_WATCH ──────────────► DELETE Watches id=11 ✔
   │      → COMPENSATED
   ▼  HTTP 400 {"sagaStatus":"COMPENSATED","failedStep":"ASSIGN_BRAND"}  + header X-Saga-Id
```

Bằng chứng đây là **compensation**, không phải rollback:

- `SagaStates` có status `COMPENSATED`; `SagaLogs` có các dòng `CREATE_WATCH / EXECUTE / SUCCESS`, `ASSIGN_BRAND / EXECUTE / FAILED`, `DELETE_WATCH / COMPENSATE / SUCCESS`.
- id 11 đã được commit rồi mới bị xoá → watch tạo thành công tiếp theo có id = 12.

## 7. Yêu cầu với compensation

| Thuộc tính | Hiện thực |
|---|---|
| **Idempotent** | Như cột "Cách hiện thực idempotent" ở các bảng trên |
| **Reversible** | Snapshot `before` cho `RESTORE_WATCH`; lưu `oldBrandId` cho `ASSIGN_BRAND_BACK` |
| **Reliable** | Retry tối đa 3 lần, cách nhau 1s; vẫn lỗi thì dừng ở `COMPENSATING` và **không** chạy tiếp các compensation còn lại |

## 8. Quyết định thiết kế

1. **Orchestration** (không dùng Choreography): chỉ có 2 service, luồng ngắn, dễ quan sát và dễ chứng minh qua `/api/sagas/{id}`.
2. Hàm orchestrator **không** có `@Transactional`. Mỗi bước là 1 local transaction riêng; `SagaStates` và `SagaLogs` được ghi bằng `REQUIRES_NEW` để không bị rollback cùng bước lỗi.
3. Compensation chạy theo thứ tự **LIFO** (stack các bước đã commit, không gồm pivot).
4. **Không** có fallback "lưu tạm" ở MSWatch: lỗi từ MSBrand (404, 5xx, mất kết nối) luôn dẫn tới compensation. Circuit Breaker + fallback chỉ đặt ở Gateway, và chỉ trả 503.
5. Validate rẻ (name, price, releaseDate, category) chạy **trước** khi tạo Saga → lỗi validation trả 400, không sinh dòng `SagaStates`.
6. Mã HTTP khi Saga thất bại: MSBrand 404 → **400**; MSBrand tắt / 5xx → **503**; fault injection → **500**.

## 9. Vì sao không dùng `@Transactional` / 2PC (TODO-4.2)

- `@Transactional` chỉ quản lý **một** database (DBWatch). Lời gọi HTTP sang MSBrand nằm ngoài transaction, nên rollback DBWatch **không** hoàn tác được DBBrand. Nếu bọc cả orchestrator trong `@Transactional`, dòng `SagaStates` cũng bị rollback và không bao giờ thấy `COMPENSATED`.
- **2PC** phải lock tài nguyên ở cả hai service suốt 2 pha, nên giảm throughput; coordinator là single point of failure; REST/Feign không hỗ trợ giao thức XA giữa hai service.
- Saga chấp nhận **eventual consistency**: mỗi bước commit ngay, lỗi thì bù trừ. Hệ thống có thể lệch tạm thời trong lúc Saga đang chạy.

## 10. Rủi ro đã biết

- **Timeout ở bước gọi MSBrand:** nếu `ASSIGN_BRAND` thực ra đã commit ở MSBrand nhưng response bị mất, Orchestrator coi bước đó là lỗi và chỉ bù trừ các bước *trước* nó → có thể còn dòng thừa trong `BrandWatches`. Vì `UNASSIGN_BRAND` idempotent, có thể chọn luôn bù trừ cả bước remote bị lỗi để an toàn; trong phạm vi bài, rủi ro này được ghi nhận và kiểm tra bằng kịch bản tắt MSBrand (mục 6.3 của đề).
- **Dừng ở `COMPENSATING`:** cần admin xử lý thủ công dựa trên `SagaLogs` (kịch bản 6.4 của đề).
