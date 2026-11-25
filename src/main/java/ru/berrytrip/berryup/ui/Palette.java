package ru.berrytrip.berryup.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;

/** Палитра BerryUp: глубокий тёмный фон, ягодный градиент и общие радиусы скругления. */
public final class Palette {

    public static final Color BG = new Color(8, 9, 14);
    public static final Color BG_SOFT = new Color(11, 13, 19);
    public static final Color SURFACE = new Color(18, 21, 29);
    public static final Color SURFACE_ALT = new Color(24, 28, 38);
    public static final Color HOVER = new Color(34, 39, 53);
    public static final Color LINE = new Color(38, 43, 57);
    public static final Color FG = new Color(243, 245, 251);
    public static final Color MUTED = new Color(154, 163, 184);
    public static final Color DIM = new Color(108, 116, 136);
    public static final Color ACCENT = new Color(255, 77, 125);
    public static final Color ACCENT_HOVER = new Color(255, 112, 151);
    public static final Color ACCENT_DEEP = new Color(214, 47, 94);
    public static final Color ACCENT_2 = new Color(255, 145, 96);
    public static final Color VIOLET = new Color(139, 92, 246);
    public static final Color OK = new Color(72, 224, 164);
    public static final Color WARN = new Color(255, 194, 87);
    public static final Color ERR = new Color(255, 107, 122);
    public static final Color TELEGRAM = new Color(42, 171, 238);

    /** Текст на ярком ягодном градиенте. */
    public static final Color ON_ACCENT = new Color(26, 7, 13);

    private static final Map<String, Font> FONTS = new HashMap<String, Font>();

    private Palette() {
    }

    public static Font reg(float size) {
        return font("Segoe UI", Font.PLAIN, size);
    }

    public static Font semi(float size) {
        return font("Segoe UI Semibold", Font.PLAIN, size);
    }

    public static Font bold(float size) {
        return font("Segoe UI", Font.BOLD, size);
    }

    public static Font mono(float size) {
        return font("Consolas", Font.PLAIN, size);
    }

    private static Font font(String family, int style, float size) {
        // Полупиксельные размеры: при дробных кеглях Windows растеризует
        // текст с размытием, а на HiDPI это особенно заметно.
        float snapped = Math.round(size * 2f) / 2f;
        String key = family + "|" + style + "|" + snapped;
        synchronized (FONTS) {
            Font cached = FONTS.get(key);
            if (cached != null) return cached;
            Font font = new Font(family, style, 12).deriveFont(snapped);
            if (!font.getFamily().equalsIgnoreCase(family)) {
                font = new Font(Font.SANS_SERIF, style, 12).deriveFont(snapped);
            }
            FONTS.put(key, font);
            return font;
        }
    }

    /** Основной ягодный градиент для кнопок и акцентных элементов. */
    public static Paint accentGradient(float x, float y, float w, float h) {
        float end = Math.max(1f, w);
        float down = Math.max(1f, h);
        return new LinearGradientPaint(x, y, x + end, y + down,
                new float[]{0f, 0.55f, 1f},
                new Color[]{ACCENT, new Color(255, 118, 132), ACCENT_2});
    }

    /** Мягкий вертикальный градиент карточки. */
    public static Paint cardGradient(int w, int h) {
        return new GradientPaint(0, 0, mix(SURFACE, Color.WHITE, 0.028f), 0, Math.max(1, h), SURFACE);
    }

    /** Фон окна: два цветных свечения по краям поверх почти чёрного. */
    public static Paint backdrop(int w, int h) {
        return new LinearGradientPaint(0, 0, Math.max(1, w), Math.max(1, h),
                new float[]{0f, 0.5f, 1f},
                new Color[]{mix(BG, VIOLET, 0.07f), BG, mix(BG, ACCENT, 0.06f)});
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
        // LCD-сглаживание даёт резкие буквы на тёмном фоне; на экранах без
        // субпикселей Java сама откатится к обычному сглаживанию.
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
}
