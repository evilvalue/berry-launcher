package ru.berrytrip.berryup.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Светлая палитра BerryUp: бумажный фон, белые карточки с мягкой тенью,
 * ягодный акцент и спокойная типографика. Никаких неоновых градиентов —
 * интерфейс ближе к современным веб-приложениям, чем к игровому оверлею.
 */
public final class Palette {

    public static final Color BG = new Color(0xF3F4F8);
    public static final Color BG_SOFT = new Color(0xF8F9FC);
    public static final Color SURFACE = new Color(0xFFFFFF);
    public static final Color SURFACE_ALT = new Color(0xF2F3F8);
    public static final Color HOVER = new Color(0xE8EAF2);
    public static final Color LINE = new Color(0xE1E4ED);
    public static final Color FG = new Color(0x15171F);
    public static final Color MUTED = new Color(0x5E6473);
    public static final Color DIM = new Color(0x969CAB);
    public static final Color ACCENT = new Color(0xE63A63);
    public static final Color ACCENT_HOVER = new Color(0xF1527A);
    public static final Color ACCENT_DEEP = new Color(0xC22850);
    public static final Color ACCENT_2 = new Color(0xFF7E5F);
    public static final Color VIOLET = new Color(0x6E5BF6);
    public static final Color OK = new Color(0x0F9D68);
    public static final Color WARN = new Color(0xB8790A);
    public static final Color ERR = new Color(0xD23545);
    public static final Color TELEGRAM = new Color(0x1F9AD6);

    /** Текст на ярком ягодном фоне. */
    public static final Color ON_ACCENT = Color.WHITE;

    private static final Map<String, Font> FONTS = new HashMap<String, Font>();
    private static final String UI_FAMILY = pick("Segoe UI Variable Text", "Segoe UI", "Inter");
    private static final String SEMI_FAMILY = pick("Segoe UI Semibold", "Segoe UI Variable Text", "Segoe UI");

    private Palette() {
    }

    public static Font reg(float size) {
        return font(UI_FAMILY, Font.PLAIN, size);
    }

    public static Font semi(float size) {
        return font(SEMI_FAMILY, Font.PLAIN, size);
    }

    public static Font bold(float size) {
        return font(UI_FAMILY, Font.BOLD, size);
    }

    public static Font mono(float size) {
        return font("Consolas", Font.PLAIN, size);
    }

    /** Небольшая прибавка к кеглю: старые размеры были рассчитаны на тёмную тему. */
    private static Font font(String family, int style, float size) {
        float px = Math.round((size + 1.0f) * 2f) / 2f;
        String key = family + "|" + style + "|" + px;
        synchronized (FONTS) {
            Font cached = FONTS.get(key);
            if (cached != null) return cached;
            Font font = new Font(family, style, 12).deriveFont(px);
            if (!font.getFamily().equalsIgnoreCase(family)) {
                font = new Font(Font.SANS_SERIF, style, 12).deriveFont(px);
            }
            FONTS.put(key, font);
            return font;
        }
    }

    private static String pick(String... families) {
        try {
            java.util.List<String> available = java.util.Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
            for (String family : families) {
                if (available.contains(family)) return family;
            }
        } catch (Exception ignored) {
        }
        return Font.SANS_SERIF;
    }

    /** Акцентный градиент остался только для логотипа и тонких подложек. */
    public static Paint accentGradient(float x, float y, float w, float h) {
        float end = Math.max(1f, w);
        float down = Math.max(1f, h);
        return new LinearGradientPaint(x, y, x + end, y + down,
                new float[]{0f, 1f},
                new Color[]{ACCENT, ACCENT_2});
    }

    /** Карточка: почти плоский белый с еле заметным уходом в холодный тон. */
    public static Paint cardGradient(int w, int h) {
        return new GradientPaint(0, 0, Color.WHITE, 0, Math.max(1, h), new Color(0xFDFDFF));
    }

    /** Фон окна: тёплый светлый градиент и два очень мягких цветных пятна. */
    public static Paint backdrop(int w, int h) {
        return new LinearGradientPaint(0, 0, Math.max(1, w), Math.max(1, h),
                new float[]{0f, 0.55f, 1f},
                new Color[]{new Color(0xF7F8FC), new Color(0xF2F3F8), new Color(0xEFF1F8)});
    }

    public static Shape round(int x, int y, int w, int h, int radius) {
        return new RoundRectangle2D.Float(x, y, w, h, radius * 2, radius * 2);
    }

    public static void fillRound(Graphics2D g, int x, int y, int w, int h, int radius, Color color) {
        g.setColor(color);
        g.fill(round(x, y, w, h, radius));
    }

    public static void fillRound(Graphics2D g, int x, int y, int w, int h, int radius, Paint paint) {
        g.setPaint(paint);
        g.fill(round(x, y, w, h, radius));
    }

    public static void strokeRound(Graphics2D g, int x, int y, int w, int h, int radius, Color color) {
        g.setColor(color);
        g.draw(round(x, y, w, h, radius));
    }

    public static Graphics2D quality(java.awt.Graphics g) {
        Graphics2D copy = (Graphics2D) g.create();
        copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        copy.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        copy.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        copy.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        copy.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        return copy;
    }

    public static Color alpha(Color color, int alpha) {
        int value = Math.max(0, Math.min(255, alpha));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), value);
    }

    public static Color mix(Color from, Color to, float amount) {
        if (amount <= 0f) return from;
        if (amount >= 1f) return to;
        return new Color(
                Math.round(from.getRed() + (to.getRed() - from.getRed()) * amount),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * amount),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * amount));
    }

    /** Мягкое радиальное свечение — для фона и подставки под моделью. */
    public static void glow(Graphics2D g, float cx, float cy, float radius, Color color, int alpha) {
        g.setPaint(new RadialGradientPaint(
                new Point2D.Float(cx, cy), Math.max(1, radius),
                new float[]{0f, 1f},
                new Color[]{alpha(color, alpha), alpha(color, 0)}));
        g.fill(new java.awt.geom.Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2));
    }

    /** Тень карточки: три почти прозрачных слоя вместо размытия по Гауссу. */
    public static void shadow(Graphics2D g, int x, int y, int w, int h, int radius) {
        for (int layer = 3; layer >= 1; layer--) {
            g.setColor(new Color(20, 24, 40, 5));
            g.fill(round(x + layer, y + layer + 1, Math.max(1, w - layer * 2), Math.max(1, h - layer * 2),
                    radius));
        }
    }
}
