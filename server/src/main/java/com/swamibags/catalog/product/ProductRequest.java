package com.swamibags.catalog.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        String id,
        String slug,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 80) String category,
        @NotBlank @Size(max = 160) String material,
        @DecimalMin("0.0") BigDecimal price,
        @Size(max = 30) String priceUnit,
        @Min(1) Integer moq,
        @Min(0) Integer stock,
        @Min(0) Integer restockDays,
        @Size(max = 80) String size,
        @Size(max = 1600) String description,
        List<@Size(max = 160) String> features
) {
}
