package com.mss301.order.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "orders")   // "order" là từ khóa SQL -> đặt tên bảng khác
@Getter @Setter @NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // MySQL AUTO_INCREMENT
    private Long id;

    private String customerId;
    private String productId;
    private int quantity;
    private double amount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    private SagaStatus sagaStatus;

    @Column(length = 1000)    // cùng chuỗi reason với SagaLog.message, lỗi mạng có thể > 255 ký tự
    private String failureReason;
}
