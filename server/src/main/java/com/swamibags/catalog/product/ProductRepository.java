package com.swamibags.catalog.product;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class ProductRepository {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public ProductRepository(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<Product> findAll() {
        var products = jdbc.query("""
                SELECT id, slug, name, category, material, price, price_unit, moq, stock,
                       restock_days, size, description, features_json, published, created_at, updated_at
                FROM products
                ORDER BY updated_at DESC
                """, this::mapProductWithoutImages);
        return products.stream().map(this::withImages).toList();
    }

    public List<Product> findPublished() {
        var products = jdbc.query("""
                SELECT id, slug, name, category, material, price, price_unit, moq, stock,
                       restock_days, size, description, features_json, published, created_at, updated_at
                FROM products
                WHERE published = 1
                ORDER BY category, name
                """, this::mapProductWithoutImages);
        return products.stream().map(this::withImages).toList();
    }

    public Optional<Product> findById(String id) {
        var rows = jdbc.query("""
                SELECT id, slug, name, category, material, price, price_unit, moq, stock,
                       restock_days, size, description, features_json, published, created_at, updated_at
                FROM products WHERE id = ?
                """, this::mapProductWithoutImages, id);
        return rows.stream().findFirst().map(this::withImages);
    }

    public void create(Product product) {
        jdbc.update("""
                INSERT INTO products (
                    id, slug, name, category, material, price, price_unit, moq, stock,
                    restock_days, size, description, features_json, published, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                product.id(), product.slug(), product.name(), product.category(), product.material(),
                product.price(), product.priceUnit(), product.moq(), product.stock(), product.restockDays(),
                product.size(), product.description(), writeFeatures(product.features()), product.published() ? 1 : 0,
                product.createdAt(), product.updatedAt());
    }

    public void update(Product product) {
        jdbc.update("""
                UPDATE products
                SET slug = ?, name = ?, category = ?, material = ?, price = ?, price_unit = ?,
                    moq = ?, stock = ?, restock_days = ?, size = ?, description = ?, features_json = ?,
                    updated_at = ?
                WHERE id = ?
                """,
                product.slug(), product.name(), product.category(), product.material(), product.price(),
                product.priceUnit(), product.moq(), product.stock(), product.restockDays(), product.size(),
                product.description(), writeFeatures(product.features()), product.updatedAt(), product.id());
    }

    public void setPublished(String id, boolean published) {
        jdbc.update("UPDATE products SET published = ?, updated_at = ? WHERE id = ?",
                published ? 1 : 0, Instant.now().toString(), id);
    }

    public void delete(String id) {
        jdbc.update("DELETE FROM products WHERE id = ?", id);
    }

    public ProductImage addImage(String productId, String kind, String path, String publicUrl, int sortOrder, boolean approved) {
        String imageId = java.util.UUID.randomUUID().toString();
        String createdAt = Instant.now().toString();
        jdbc.update("""
                INSERT INTO product_images (image_id, product_id, kind, path, public_url, sort_order, approved, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, imageId, productId, kind, path, publicUrl, sortOrder, approved ? 1 : 0, createdAt);
        return new ProductImage(imageId, productId, kind, path, publicUrl, sortOrder, approved, createdAt);
    }

    public Optional<ProductImage> findImage(String productId, String imageId) {
        return jdbc.query("""
                SELECT image_id, product_id, kind, path, public_url, sort_order, approved, created_at
                FROM product_images WHERE product_id = ? AND image_id = ?
                """, this::mapImage, productId, imageId).stream().findFirst();
    }

    public List<ProductImage> findImages(String productId) {
        return jdbc.query("""
                SELECT image_id, product_id, kind, path, public_url, sort_order, approved, created_at
                FROM product_images
                WHERE product_id = ?
                ORDER BY CASE WHEN kind = 'MARKETING' THEN 0 ELSE 1 END, approved DESC, sort_order, created_at DESC
                """, this::mapImage, productId);
    }

    public void approveMarketingImage(String productId, String imageId) {
        jdbc.update("UPDATE product_images SET approved = 0 WHERE product_id = ? AND kind = 'MARKETING'", productId);
        int changed = jdbc.update("""
                UPDATE product_images SET approved = 1
                WHERE product_id = ? AND image_id = ? AND kind = 'MARKETING'
                """, productId, imageId);
        if (changed == 0) {
            throw new IllegalArgumentException("Marketing image not found.");
        }
    }

    public void deleteImage(String productId, String imageId) {
        jdbc.update("DELETE FROM product_images WHERE product_id = ? AND image_id = ?", productId, imageId);
    }

    public int countAll() {
        return Optional.ofNullable(jdbc.queryForObject("SELECT COUNT(*) FROM products", Integer.class)).orElse(0);
    }

    public int countPublished() {
        return Optional.ofNullable(jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE published = 1", Integer.class)).orElse(0);
    }

    public int countOutOfStock() {
        return Optional.ofNullable(jdbc.queryForObject("SELECT COUNT(*) FROM products WHERE stock = 0", Integer.class)).orElse(0);
    }

    public int countImages() {
        return Optional.ofNullable(jdbc.queryForObject("SELECT COUNT(*) FROM product_images", Integer.class)).orElse(0);
    }

    private Product withImages(Product product) {
        return new Product(
                product.id(), product.slug(), product.name(), product.category(), product.material(), product.price(),
                product.priceUnit(), product.moq(), product.stock(), product.restockDays(), product.size(),
                product.description(), product.features(), product.published(), findImages(product.id()),
                product.createdAt(), product.updatedAt());
    }

    private Product mapProductWithoutImages(ResultSet rs, int rowNum) throws SQLException {
        Object restockValue = rs.getObject("restock_days");
        Integer restockDays = restockValue == null ? null : ((Number) restockValue).intValue();
        return new Product(
                rs.getString("id"),
                rs.getString("slug"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getString("material"),
                rs.getBigDecimal("price"),
                rs.getString("price_unit"),
                rs.getInt("moq"),
                rs.getInt("stock"),
                restockDays,
                rs.getString("size"),
                rs.getString("description"),
                readFeatures(rs.getString("features_json")),
                rs.getInt("published") == 1,
                List.of(),
                rs.getString("created_at"),
                rs.getString("updated_at"));
    }

    private ProductImage mapImage(ResultSet rs, int rowNum) throws SQLException {
        return new ProductImage(
                rs.getString("image_id"),
                rs.getString("product_id"),
                rs.getString("kind"),
                rs.getString("path"),
                rs.getString("public_url"),
                rs.getInt("sort_order"),
                rs.getInt("approved") == 1,
                rs.getString("created_at"));
    }

    private String writeFeatures(List<String> features) {
        try {
            return json.writeValueAsString(features == null ? List.of() : features);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize product features.", e);
        }
    }

    private List<String> readFeatures(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return json.readValue(value, STRING_LIST);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
