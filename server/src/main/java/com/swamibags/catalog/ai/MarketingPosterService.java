package com.swamibags.catalog.ai;

import com.swamibags.catalog.config.AppProperties;
import com.swamibags.catalog.product.Product;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;

import org.springframework.stereotype.Service;

@Service
public class MarketingPosterService {
    private static final Color WINE = new Color(145, 24, 33);
    private static final Color INK = new Color(36, 27, 23);
    private static final Color MUTED = new Color(101, 87, 80);
    private static final Color CREAM = new Color(255, 250, 243);

    private final AppProperties properties;

    public MarketingPosterService(AppProperties properties) {
        this.properties = properties;
    }

    public byte[] compose(byte[] baseImage, Product product) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(baseImage));
            if (source == null) {
                throw new IllegalArgumentException("Generated image could not be decoded.");
            }

            int width = 1536;
            int height = 1024;
            BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = canvas.createGraphics();
            configure(g);

            drawCover(g, source, 0, 0, width, height);

            int panelX = 980;
            int panelY = 64;
            int panelW = 500;
            int panelH = 896;

            g.setComposite(AlphaComposite.SrcOver.derive(0.94f));
            g.setColor(CREAM);
            g.fill(new RoundRectangle2D.Double(panelX, panelY, panelW, panelH, 34, 34));
            g.setComposite(AlphaComposite.SrcOver);
            g.setColor(new Color(221, 199, 184));
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Double(panelX, panelY, panelW, panelH, 34, 34));

            int x = panelX + 42;
            int y = panelY + 60;
            int textW = panelW - 84;

            g.setColor(WINE);
            g.setFont(new Font(Font.SERIF, Font.BOLD, 38));
            g.drawString(properties.brandName(), x, y);
            y += 56;

            g.setColor(INK);
            g.setFont(new Font(Font.SERIF, Font.BOLD, 50));
            y = drawWrapped(g, product.name(), x, y, textW, 56);
            y += 18;

            g.setColor(WINE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            g.drawString(product.id() + "  •  " + product.category(), x, y);
            y += 42;

            g.setColor(MUTED);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
            y = drawWrapped(g, product.material(), x, y, textW, 30);
            y += 28;

            g.setColor(new Color(232, 216, 204));
            g.fillRoundRect(x, y, textW, 2, 2, 2);
            y += 38;

            g.setColor(INK);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            List<String> features = product.features().stream().limit(4).toList();
            for (String feature : features) {
                g.setColor(WINE);
                g.fillOval(x, y - 15, 14, 14);
                g.setColor(INK);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
                y = drawWrapped(g, feature, x + 28, y, textW - 28, 28);
                y += 16;
            }

            int priceY = panelY + panelH - 160;
            g.setColor(WINE);
            g.setFont(new Font(Font.SERIF, Font.BOLD, 34));
            String price = product.price().signum() > 0
                    ? "Wholesale ₹" + product.price().setScale(0, RoundingMode.HALF_UP) + " / " + product.priceUnit()
                    : "Wholesale price on enquiry";
            g.drawString(price, x, priceY);

            g.setColor(MUTED);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 19));
            g.drawString("MOQ " + product.moq() + " pcs  •  " +
                    (product.stock() > 0 ? product.stock() + " available" : "Restock approx. " + safeRestock(product) + " days"),
                    x, priceY + 38);

            g.setColor(WINE);
            g.setFont(new Font(Font.SERIF, Font.ITALIC, 23));
            g.drawString("Quality • Reliable • Wholesale", x, panelY + panelH - 48);

            g.dispose();
            return encodeJpeg(canvas, 0.9f);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to compose marketing poster.", e);
        }
    }

    private int safeRestock(Product product) {
        return product.restockDays() == null ? 7 : product.restockDays();
    }

    private void configure(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    private void drawCover(Graphics2D g, BufferedImage image, int x, int y, int w, int h) {
        double scale = Math.max((double) w / image.getWidth(), (double) h / image.getHeight());
        int dw = (int) Math.ceil(image.getWidth() * scale);
        int dh = (int) Math.ceil(image.getHeight() * scale);
        int dx = x + (w - dw) / 2;
        int dy = y + (h - dh) / 2;
        g.drawImage(image, dx, dy, dw, dh, null);
    }

    private int drawWrapped(Graphics2D g, String text, int x, int baseline, int width, int lineHeight) {
        if (text == null || text.isBlank()) {
            return baseline;
        }
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        int y = baseline;
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(candidate) > width && !line.isEmpty()) {
                g.drawString(line.toString(), x, y);
                y += lineHeight;
                line.setLength(0);
                line.append(word);
            } else {
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            g.drawString(line.toString(), x, y);
        }
        return y + lineHeight;
    }

    private byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        var writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("JPEG writer is unavailable.");
        }
        var writer = writers.next();
        try (var output = new ByteArrayOutputStream();
             var imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), param);
            writer.dispose();
            return output.toByteArray();
        }
    }
}
