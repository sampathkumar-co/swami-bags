package com.swamibags.catalog.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swamibags.catalog.product.ProductRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class AdminApiIntegrationTest {
    private static final Path TEMP_DIR = createTempDirectory();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.admin-username", () -> "admin");
        registry.add("app.admin-password", () -> "admin-api-integration-password");
        registry.add("app.data-dir", TEMP_DIR::toString);
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEMP_DIR.resolve("admin-api.db"));
        registry.add("app.open-ai-api-key", () -> "");
        registry.add("server.servlet.session.cookie.secure", () -> false);
    }

    @Autowired
    WebApplicationContext context;

    @Autowired
    ProductRepository products;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void authStatusIsPublicWhileAdminEndpointsRemainProtected() throws Exception {
        mvc.perform(get("/api/admin/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.username").value(""));

        mvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/admin/auth/me").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void authenticatedAdminCanReadDashboardAndSettings() throws Exception {
        mvc.perform(get("/api/admin/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isNumber())
                .andExpect(jsonPath("$.published").isNumber())
                .andExpect(jsonPath("$.images").isNumber());

        mvc.perform(get("/api/admin/settings").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brandName").isString());
    }

    @Test
    void stateChangingAdminEndpointsRequireCsrf() throws Exception {
        String body = """
                {
                  "id":"SB-CSRF-CHECK",
                  "name":"CSRF Check Bag",
                  "category":"Cash Bags",
                  "material":"Polyester",
                  "price":100,
                  "priceUnit":"piece",
                  "moq":10,
                  "stock":20,
                  "features":[]
                }
                """;

        mvc.perform(post("/api/admin/products")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void productAdminFlowValidatesCategoryPublishAndImageContent() throws Exception {
        String invalidCategory = """
                {
                  "id":"SB-ADMIN-BAD",
                  "name":"Invalid Category Bag",
                  "category":"Unknown Bags",
                  "material":"Polyester",
                  "price":100,
                  "priceUnit":"piece",
                  "moq":10,
                  "stock":20,
                  "features":[]
                }
                """;

        mvc.perform(post("/api/admin/products")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidCategory))
                .andExpect(status().isBadRequest());

        String valid = """
                {
                  "id":"SB-ADMIN-FLOW",
                  "name":"Admin Flow Bag",
                  "category":"Cash Bags",
                  "material":"Polyester",
                  "price":100,
                  "priceUnit":"piece",
                  "moq":10,
                  "stock":20,
                  "features":["Inner pocket"]
                }
                """;

        mvc.perform(post("/api/admin/products")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(valid))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("SB-ADMIN-FLOW"))
                .andExpect(jsonPath("$.published").value(false));

        mvc.perform(post("/api/admin/products/SB-ADMIN-FLOW/publish")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"published\":true}"))
                .andExpect(status().isBadRequest());

        MockMultipartFile fake = new MockMultipartFile(
                "files",
                "fake.png",
                "image/png",
                "not-a-real-image".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/admin/products/SB-ADMIN-FLOW/images")
                        .file(fake)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        MockMultipartFile real = new MockMultipartFile(
                "files",
                "real.png",
                "image/png",
                java.util.Base64.getDecoder().decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="));

        String uploadBody = mvc.perform(multipart("/api/admin/products/SB-ADMIN-FLOW/images")
                        .file(real)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind").value("ORIGINAL"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        var imageMatcher = java.util.regex.Pattern.compile("\\\"imageId\\\":\\\"([^\\\"]+)\\\"").matcher(uploadBody);
        if (!imageMatcher.find()) {
            throw new AssertionError("Uploaded image response did not contain an imageId.");
        }
        String imageId = imageMatcher.group(1);

        mvc.perform(post("/api/admin/products/SB-ADMIN-FLOW/publish")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"published\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true));

        mvc.perform(delete("/api/admin/products/SB-ADMIN-FLOW/images/" + imageId)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/admin/products/SB-ADMIN-FLOW/publish")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"published\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(false));

        mvc.perform(delete("/api/admin/products/SB-ADMIN-FLOW/images/" + imageId)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mvc.perform(delete("/api/admin/products/SB-ADMIN-FLOW")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void changingBrandNameInvalidatesApprovedMarketingImages() throws Exception {
        String product = """
                {
                  "id":"SB-BRAND-INVALIDATE",
                  "name":"Brand Invalidation Bag",
                  "category":"Jute Bags",
                  "material":"Jute",
                  "price":0,
                  "priceUnit":"piece",
                  "moq":25,
                  "stock":50,
                  "features":["Inner pocket"]
                }
                """;

        mvc.perform(post("/api/admin/products")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(product))
                .andExpect(status().isCreated());

        var marketing = products.addImage(
                "SB-BRAND-INVALIDATE",
                "MARKETING",
                "media/products/SB-BRAND-INVALIDATE/marketing/test.jpg",
                "/media/products/SB-BRAND-INVALIDATE/marketing/test.jpg",
                0,
                false);

        mvc.perform(post("/api/admin/products/SB-BRAND-INVALIDATE/marketing/" + marketing.imageId() + "/approve")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images[?(@.imageId == '" + marketing.imageId() + "')].approved").value(true));

        String updatedSettings = """
                {
                  "brandName":"New Chandra Bags Brand Audit",
                  "whatsappNumber":"",
                  "businessPhone":"",
                  "businessEmail":"",
                  "businessAddress":"",
                  "publicBaseUrl":"",
                  "logoUrl":""
                }
                """;

        mvc.perform(put("/api/admin/settings")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedSettings))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brandName").value("New Chandra Bags Brand Audit"));

        mvc.perform(get("/api/admin/products/SB-BRAND-INVALIDATE")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images[?(@.imageId == '" + marketing.imageId() + "')].approved").value(false));

        mvc.perform(delete("/api/admin/products/SB-BRAND-INVALIDATE")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void settingsAndLogoEndpointsRejectInvalidInput() throws Exception {
        String invalidSettings = """
                {
                  "brandName":"New Chandra Bags",
                  "whatsappNumber":"123",
                  "businessPhone":"",
                  "businessEmail":"not-an-email",
                  "businessAddress":"",
                  "publicBaseUrl":"javascript:alert(1)",
                  "logoUrl":""
                }
                """;

        mvc.perform(put("/api/admin/settings")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidSettings))
                .andExpect(status().isBadRequest());

        MockMultipartFile fakeLogo = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                "not-a-real-image".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/admin/settings/logo")
                        .file(fakeLogo)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        String validSettings = """
                {
                  "brandName":"New Chandra Bags Test",
                  "whatsappNumber":"919876543210",
                  "businessPhone":"+91 98765 43210",
                  "businessEmail":"sales@example.test",
                  "businessAddress":"Test wholesale address",
                  "publicBaseUrl":"https://bags.example.test",
                  "logoUrl":""
                }
                """;

        mvc.perform(put("/api/admin/settings")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSettings))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brandName").value("New Chandra Bags Test"))
                .andExpect(jsonPath("$.whatsappNumber").value("919876543210"));

        MockMultipartFile validLogo = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                validPngBytes());

        String logoBody = mvc.perform(multipart("/api/admin/settings/logo")
                        .file(validLogo)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        if (!logoBody.contains("/media/brand/logo-")) {
            throw new AssertionError("Logo upload did not return a managed public media URL.");
        }

        mvc.perform(delete("/api/admin/settings/logo")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value(""));
    }

    private static byte[] validPngBytes() {
        return java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
    }

    private static Path createTempDirectory() {
        try {
            return Files.createTempDirectory("new-chandra-admin-api-");
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
