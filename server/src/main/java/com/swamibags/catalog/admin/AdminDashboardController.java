package com.swamibags.catalog.admin;

import com.swamibags.catalog.config.AppProperties;
import com.swamibags.catalog.product.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final ProductService products;
    private final AppProperties properties;

    public AdminDashboardController(ProductService products, AppProperties properties) {
        this.products = products;
        this.properties = properties;
    }

    @GetMapping
    public ProductService.Dashboard dashboard() {
        return products.dashboard(properties.aiConfigured());
    }
}
