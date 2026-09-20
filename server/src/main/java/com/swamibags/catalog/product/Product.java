package com.swamibags.catalog.product;

import java.math.BigDecimal;
import java.util.List;

public record Product(
        String id,
        String slug,
        String name,
        String category,
        String material,
        BigDecimal price,
        String priceUnit,
        int moq,
        int stock,
        Integer restockDays,
        String size,
        String description,
        List<String> features,
        boolean published,
        List<ProductImage> images,
        String createdAt,
        String updatedAt
) {
    public String displayImage() {
        return images.stream()
                .filter(ProductImage::isMarketing)
                .filter(ProductImage::approved)
                .map(ProductImage::publicUrl)
                .findFirst()
                .or(() -> images.stream()
                        .filter(ProductImage::isOriginal)
                        .map(ProductImage::publicUrl)
                        .findFirst())
                .orElse("");
    }

    public List<String> originalImageUrls() {
        return images.stream()
                .filter(ProductImage::isOriginal)
                .sorted((a, b) -> Integer.compare(a.sortOrder(), b.sortOrder()))
                .map(ProductImage::publicUrl)
                .toList();
    }
}
