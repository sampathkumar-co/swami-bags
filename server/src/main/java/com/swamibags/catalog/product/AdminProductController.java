package com.swamibags.catalog.product;

import com.swamibags.catalog.ai.MarketingImageService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {
    private final ProductService products;
    private final MarketingImageService marketing;

    public AdminProductController(ProductService products, MarketingImageService marketing) {
        this.products = products;
        this.marketing = marketing;
    }

    @GetMapping
    public List<Product> all() {
        return products.all();
    }

    @GetMapping("/{id}")
    public Product get(@PathVariable String id) {
        return products.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody ProductRequest request) {
        return products.create(request);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable String id, @Valid @RequestBody ProductRequest request) {
        return products.update(id, request);
    }

    @PostMapping("/{id}/publish")
    public Product publish(@PathVariable String id, @RequestBody PublishRequest request) {
        return products.setPublished(id, request.published());
    }

    @PostMapping("/{id}/images")
    public List<ProductImage> upload(
            @PathVariable String id,
            @RequestPart("files") List<MultipartFile> files) throws IOException {
        return products.addOriginalImages(id, files);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@PathVariable String id, @PathVariable String imageId) {
        products.removeImage(id, imageId);
    }

    @PostMapping("/{id}/marketing/generate")
    public ProductImage generateMarketing(@PathVariable String id) {
        return marketing.generate(id);
    }

    @PostMapping("/{id}/marketing/{imageId}/approve")
    public Product approveMarketing(@PathVariable String id, @PathVariable String imageId) {
        return products.approveMarketingImage(id, imageId);
    }

    @GetMapping("/generations")
    public List<MarketingImageService.Generation> generations(@RequestParam(required = false) String productId) {
        return marketing.history(productId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        products.delete(id);
    }

    public record PublishRequest(boolean published) {}
}
