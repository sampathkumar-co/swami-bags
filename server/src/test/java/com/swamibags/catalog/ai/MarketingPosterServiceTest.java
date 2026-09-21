package com.swamibags.catalog.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.swamibags.catalog.product.Product;
import com.swamibags.catalog.settings.SiteSettings;
import com.swamibags.catalog.settings.SiteSettingsRepository;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class MarketingPosterServiceTest {

    @Test
    void composesVerifiedProductTextIntoExpectedPosterSize() throws Exception {
        var settings = mock(SiteSettingsRepository.class);
        when(settings.get()).thenReturn(new SiteSettings(
                "New Chandra Bags",
                "919876543210",
                "+91 98765 43210",
                "sales@example.com",
                "Test address",
                "https://example.com",
                ""));

        var product = new Product(
                "SB-POSTER-01",
                "poster-test-bag",
                "Poster Test Bag",
                "Jute Bags",
                "Natural jute and cotton",
                new BigDecimal("149.00"),
                "piece",
                50,
                120,
                null,
                "15 x 14 x 5 in",
                "Poster rendering test.",
                List.of("Strong handles", "Double zip", "Custom printing"),
                false,
                List.of(),
                "2026-09-20T00:00:00Z",
                "2026-09-20T00:00:00Z");

        byte[] source = sourceImage();
        byte[] poster = new MarketingPosterService(settings).compose(source, product);

        assertThat(poster).isNotEmpty();
        assertThat(poster[0] & 0xff).isEqualTo(0xff);
        assertThat(poster[1] & 0xff).isEqualTo(0xd8);

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(poster));
        assertThat(decoded).isNotNull();
        assertThat(decoded.getWidth()).isEqualTo(1536);
        assertThat(decoded.getHeight()).isEqualTo(1024);
    }

    @Test
    void zeroPriceRendersAsQuoteOnlyPosterWithoutFailing() throws Exception {
        var settings = mock(SiteSettingsRepository.class);
        when(settings.get()).thenReturn(new SiteSettings(
                "New Chandra Bags",
                "",
                "",
                "",
                "",
                "",
                ""));

        var product = new Product(
                "SB-POSTER-02",
                "quote-only-bag",
                "Quote Only Bag",
                "Cash Bags",
                "Polyester",
                BigDecimal.ZERO,
                "piece",
                100,
                0,
                7,
                "",
                "",
                List.of(),
                false,
                List.of(),
                "2026-09-20T00:00:00Z",
                "2026-09-20T00:00:00Z");

        byte[] poster = new MarketingPosterService(settings).compose(sourceImage(), product);
        assertThat(ImageIO.read(new ByteArrayInputStream(poster))).isNotNull();
    }

    private byte[] sourceImage() throws Exception {
        BufferedImage image = new BufferedImage(900, 700, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(239, 230, 218));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(new Color(125, 44, 52));
        graphics.fillRoundRect(120, 150, 540, 390, 40, 40);
        graphics.dispose();

        try (var output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpeg", output);
            return output.toByteArray();
        }
    }
}
