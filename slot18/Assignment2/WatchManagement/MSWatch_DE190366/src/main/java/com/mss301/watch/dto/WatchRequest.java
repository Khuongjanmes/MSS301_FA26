package com.mss301.watch.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// [TODO-6.13: ràng buộc name > 10 ký tự, price >= 100, đủ field]
@Data
public class WatchRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 11, max = 255, message = "Name must be longer than 10 characters (max 255)")
    private String name;

    @NotNull(message = "CategoryId is required")
    private Integer categoryId;

    @NotNull(message = "BrandId is required")
    private Integer brandId;

    @NotBlank(message = "CaseMaterial is required")
    @Size(max = 100)
    private String caseMaterial;

    @NotBlank(message = "StrapMaterial is required")
    @Size(max = 100)
    private String strapMaterial;

    @NotBlank(message = "WaterpMaterial is required")
    @Size(max = 50)
    private String waterpMaterial;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "100.0", message = "Price must be at least 100")
    @Digits(integer = 8, fraction = 2, message = "Price must fit DECIMAL(10,2)")
    private BigDecimal price;

    @NotNull(message = "ReleaseDate is required")
    private LocalDate releaseDate;

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock must be >= 0")
    private Integer stock;
}
