package com.mss301.watch.entity;

import jakarta.persistence.*;
import lombok.*;

// [TODO-6.16: bảng WatchCategories theo đề bài]
@Entity
@Table(name = "WatchCategories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CategoryID")
    private Integer categoryId;

    @Column(name = "CategoryName", nullable = false, length = 100)
    private String categoryName;

    // Đề bài: TEXT. SQL Server đã deprecated TEXT -> dùng VARCHAR(MAX) tương đương.
    @Column(name = "Description", columnDefinition = "VARCHAR(MAX)")
    private String description;

    @Column(name = "MovementType", length = 50)
    private String movementType;

    @Column(name = "PriceRange", length = 100)
    private String priceRange;
}
