package com.swamibags.catalog.product;

public record ProductImage(
        String imageId,
        String productId,
        String kind,
        String path,
        String publicUrl,
        int sortOrder,
        boolean approved,
        String createdAt
) {
    public boolean isOriginal() {
        return "ORIGINAL".equals(kind);
    }

    public boolean isMarketing() {
        return "MARKETING".equals(kind);
    }
}
