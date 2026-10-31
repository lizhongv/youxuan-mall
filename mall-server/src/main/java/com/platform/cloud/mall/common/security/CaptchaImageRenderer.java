package com.platform.cloud.mall.common.security;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SecureRandom;
import java.util.Random;

/**
 * 图形验证码绘制：数字用「七段」笔画画出，再叠加随机旋转、错切、干扰线与噪点。
 * <p>
 * 不用 hutool 的 LineCaptcha：它依赖系统字体渲染文字，部署用的 eclipse-temurin:*-alpine 镜像没有 fontconfig 与字体，
 * 绘制文字会直接抛异常。笔画只用 drawLine，headless 环境下无需任何字体。
 * 强度定位是「挡住无脑脚本、抬高批量成本」，配合 IP 限流与失败锁定使用，不追求对抗专业 OCR。
 */
public final class CaptchaImageRenderer {

    /** 宽高与常见登录框输入框高度匹配 */
    static final int WIDTH = 120;
    static final int HEIGHT = 40;

    /** 七段编码：a 上、b 右上、c 右下、d 下、e 左下、f 左上、g 中，bit0..bit6 */
    private static final int[] SEGMENTS = {
            0b0111111, 0b0000110, 0b1011011, 0b1001111, 0b1100110,
            0b1101101, 0b1111101, 0b0000111, 0b1111111, 0b1101111};

    /** 单个字形的笔画框 */
    private static final int GLYPH_W = 14;
    private static final int GLYPH_H = 24;

    private static final Random RANDOM = new SecureRandom();

    private CaptchaImageRenderer() {
    }

    /** 只支持数字；返回 PNG 字节 */
    public static byte[] renderPng(String digits) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(235 + RANDOM.nextInt(20), 235 + RANDOM.nextInt(20), 235 + RANDOM.nextInt(20)));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            drawNoiseLines(g, 6);
            int step = (WIDTH - 16) / Math.max(1, digits.length());
            for (int i = 0; i < digits.length(); i++) {
                int d = digits.charAt(i) - '0';
                if (d < 0 || d > 9) {
                    throw new IllegalArgumentException("验证码只支持数字");
                }
                drawGlyph(g, d, 10 + i * step + RANDOM.nextInt(5), 6 + RANDOM.nextInt(6));
            }
            drawNoiseLines(g, 3);
            for (int i = 0; i < 80; i++) {
                img.setRGB(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), randomColor(60, 200).getRGB());
            }
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        try {
            ImageIO.write(img, "png", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static void drawGlyph(Graphics2D g, int digit, int x, int y) {
        AffineTransform saved = g.getTransform();
        g.translate(x + GLYPH_W / 2.0, y + GLYPH_H / 2.0);
        g.rotate((RANDOM.nextDouble() - 0.5) * 0.6);
        g.shear((RANDOM.nextDouble() - 0.5) * 0.4, 0);
        g.translate(-GLYPH_W / 2.0, -GLYPH_H / 2.0);
        g.setColor(randomColor(20, 120));
        g.setStroke(new BasicStroke(2.4f + RANDOM.nextFloat() * 1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int mask = SEGMENTS[digit];
        int w = GLYPH_W;
        int h = GLYPH_H;
        int m = h / 2;
        // 端点加 ±1 像素抖动，避免每个笔画都是完全规则的直线
        if ((mask & 1) != 0) line(g, 0, 0, w, 0);
        if ((mask & 2) != 0) line(g, w, 0, w, m);
        if ((mask & 4) != 0) line(g, w, m, w, h);
        if ((mask & 8) != 0) line(g, 0, h, w, h);
        if ((mask & 16) != 0) line(g, 0, m, 0, h);
        if ((mask & 32) != 0) line(g, 0, 0, 0, m);
        if ((mask & 64) != 0) line(g, 0, m, w, m);
        g.setTransform(saved);
    }

    private static void line(Graphics2D g, int x1, int y1, int x2, int y2) {
        g.drawLine(x1 + jitter(), y1 + jitter(), x2 + jitter(), y2 + jitter());
    }

    private static int jitter() {
        return RANDOM.nextInt(3) - 1;
    }

    private static void drawNoiseLines(Graphics2D g, int count) {
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i < count; i++) {
            g.setColor(randomColor(100, 200));
            g.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
        }
    }

    private static Color randomColor(int min, int max) {
        int span = max - min;
        return new Color(min + RANDOM.nextInt(span), min + RANDOM.nextInt(span), min + RANDOM.nextInt(span));
    }
}
