package com.smartcommunity.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CaptchaService {

    private static final int WIDTH = 130;
    private static final int HEIGHT = 48;
    private static final int CODE_LENGTH = 4;
    private static final long EXPIRE_MS = 5 * 60 * 1000; // 5 分钟过期
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final Map<String, CaptchaEntry> cache = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /**
     * 生成验证码，返回 { key, imageBase64 }
     */
    public Map<String, String> generate() {
        // 清理过期
        cache.entrySet().removeIf(e -> System.currentTimeMillis() - e.getValue().createTime > EXPIRE_MS);

        String code = randomCode(CODE_LENGTH);
        String key = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        cache.put(key, new CaptchaEntry(code));

        BufferedImage image = drawImage(code);
        String base64 = toBase64(image);

        return Map.of("captchaKey", key, "captchaImage", "data:image/png;base64," + base64);
    }

    /**
     * 验证验证码，验证通过后删除
     */
    public boolean validate(String key, String code) {
        if (key == null || code == null) return false;
        CaptchaEntry entry = cache.remove(key);
        if (entry == null) return false;
        if (System.currentTimeMillis() - entry.createTime > EXPIRE_MS) return false;
        return entry.code.equalsIgnoreCase(code.trim());
    }

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private BufferedImage drawImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 背景
        g.setColor(new Color(240, 245, 250));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 干扰线
        g.setColor(new Color(180, 200, 230));
        for (int i = 0; i < 6; i++) {
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        // 干扰点
        for (int i = 0; i < 30; i++) {
            g.setColor(new Color(150 + random.nextInt(80), 160 + random.nextInt(60), 190 + random.nextInt(40)));
            g.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
        }

        // 文字
        Font[] fonts = { new Font("Arial", Font.BOLD, 28), new Font("Arial", Font.ITALIC, 28) };
        Color[] colors = { new Color(40, 80, 160), new Color(160, 40, 40), new Color(30, 120, 80), new Color(100, 40, 140) };

        for (int i = 0; i < code.length(); i++) {
            g.setFont(fonts[random.nextInt(fonts.length)]);
            g.setColor(colors[random.nextInt(colors.length)]);
            int x = 10 + i * 28 + random.nextInt(8) - 4;
            int y = 30 + random.nextInt(10) - 5;
            double angle = (random.nextDouble() - 0.5) * 0.3;
            g.rotate(angle, x, y);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.rotate(-angle, x, y);
        }

        g.dispose();
        return image;
    }

    private String toBase64(BufferedImage image) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", bos);
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("验证码图片生成失败", e);
        }
    }

    private static class CaptchaEntry {
        final String code;
        final long createTime;
        CaptchaEntry(String code) { this.code = code; this.createTime = System.currentTimeMillis(); }
    }
}
