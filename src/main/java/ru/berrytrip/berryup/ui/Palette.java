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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Палитра BerryUp. Две темы — светлая и тёмная — и пиксельный Monocraft,
 * собранный по мотивам шрифта Minecraft (OFL, поддерживает кириллицу).
 */
public final class Palette {

    public static Color BG;
    public static Color BG_SOFT;
    public static Color SURFACE;
    public static Color SURFACE_ALT;
    public static Color HOVER;
    public static Color LINE;
    public static Color FG;
    public static Color MUTED;
    public static Color DIM;
    public static Color ACCENT;
    public static Color ACCENT_HOVER;
    public static Color ACCENT_DEEP;
    public static Color ACCENT_2;
    public static Color VIOLET;
    public static Color OK;
    public static Color WARN;
    public static Color ERR;
    public static Color TELEGRAM;
    public static Color ON_ACCENT;

    private static boolean dark;
    private static final Map<String, Font> FONTS = new HashMap<String, Font>();
    private static final Font PIXEL_REG = loadFont("/Monocraft.ttf");
    private static final Font PIXEL_SEMI = loadFont("/Monocraft-SemiBold.ttf");

    static {
        applyTheme(false);
    }

    private Palette() {
    }

    public static boolean isDark() {
        return dark;
    }

    /** Переключает цвета. Компоненты читают их во время отрисовки, поэтому окно пересоздаётся. */
    public static void applyTheme(boolean darkTheme) {
        dark = darkTheme;
        if (darkTheme) {
            BG = new Color(0x14161B);
            BG_SOFT = new Color(0x191C22);
            SURFACE = new Color(0x1E2229);
            SURFACE_ALT = new Color(0x262B34);
            HOVER = new Color(0x303641);
            LINE = new Color(0x383F4B);
            FG = new Color(0xF1F3F8);
            MUTED = new Color(0xA9B0BE);
            DIM = new Color(0x7A8292);
            ACCENT = new Color(0xFF5D86);
            ACCENT_HOVER = new Color(0xFF7CA0);
            ACCENT_DEEP = new Color(0xE53A67);
            ACCENT_2 = new Color(0xFFA36B);
            VIOLET = new Color(0x9B87FF);
            OK = new Color(0x43DD9A);
            WARN = new Color(0xF2B544);
            ERR = new Color(0xFF6E7B);
            TELEGRAM = new Color(0x38AEE0);
            ON_ACCENT = Color.WHITE;
        } else {
            BG = new Color(0xF3F4F8);
            BG_SOFT = new Color(0xF8F9FC);
            SURFACE = new Color(0xFFFFFF);
            SURFACE_ALT = new Color(0xF2F3F8);
            HOVER = new Color(0xE8EAF2);
            LINE = new Color(0xE1E4ED);
            FG = new Color(0x15171F);
            MUTED = new Color(0x5E6473);
            DIM = new Color(0x969CAB);
            ACCENT = new Color(0xE63A63);
            ACCENT_HOVER = new Color(0xF1527A);
            ACCENT_DEEP = new Color(0xC22850);
            ACCENT_2 = new Color(0xFF7E5F);
            VIOLET = new Color(0x6E5BF6);
            OK = new Color(0x0F9D68);
            WARN = new Color(0xB8790A);
            ERR = new Color(0xD23545);
            TELEGRAM = new Color(0x1F9AD6);
            ON_ACCENT = Color.WHITE;
        }
    }

    public static Font reg(float size) {
        return pixel(PIXEL_REG, size);
    }

    public static Font semi(float size) {
        return pixel(PIXEL_SEMI, size);
    }

    public static Font bold(float size) {
        return pixel(PIXEL_SEMI, size);
    }

    /** Monocraft моноширинный, поэтому журналу отдельный шрифт не нужен. */
    public static Font mono(float size) {
        return pixel(PIXEL_REG, size);
    }

    private static Font pixel(Font base, float size) {
        // Monocraft шире обычных шрифтов, поэтому кегль уменьшаем и держим целым
        float px = Math.max(10f, Math.round(size * 0.88f));
        String key = (base == PIXEL_SEMI ? "s|" : "r|") + px;
        synchronized (FONTS) {
            Font cached = FONTS.get(key);
            if (cached != null) return cached;
            Font font;
            if (base != null) {
                font = base.deriveFont(Font.PLAIN, px);
            } else {
                font = new Font(pick("Segoe UI", "Inter"), Font.PLAIN, 12).deriveFont(px);
            }
            FONTS.put(key, font);
            return font;
        }
    }

    private static Font loadFont(String resource) {
        try (InputStream stream = Palette.class.getResourceAsStream(resource)) {
            if (stream != null) return Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (Exception ignored) {
        }
        return null;
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

    /** Акцентный градиент для логотипа и тонких подложек. */
    public static Paint accentGradient(float x, float y, float w, float h) {
        float end = Math.max(1f, w);
        float down = Math.max(1f, h);
        return new LinearGradientPaint(x, y, x + end, y + down,
                new float[]{0f, 1f},
                new Color[]{ACCENT, ACCENT_2});
    }

    public static Paint cardGradient(int w, int h) {
        if (dark) {
            return new GradientPaint(0, 0, new Color(0x20242C), 0, Math.max(1, h), new Color(0x1B1F26));
        }
        return new GradientPaint(0, 0, Color.WHITE, 0, Math.max(1, h), new Color(0xFDFDFF));
    }

    public static Paint backdrop(int w, int h) {
        if (dark) {
            return new GradientPaint(0, 0, new Color(0x17191E), 0, Math.max(1, h),
                    new Color(0x111318));
        }
        return new GradientPaint(0, 0, new Color(0xF7F8FA), 0, Math.max(1, h),
                new Color(0xEDEFF4));
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
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
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

    public static void glow(Graphics2D g, float cx, float cy, float radius, Color color, int alpha) {
        g.setPaint(new RadialGradientPaint(
                new Point2D.Float(cx, cy), Math.max(1, radius),
                new float[]{0f, 1f},
                new Color[]{alpha(color, alpha), alpha(color, 0)}));
        g.fill(new java.awt.geom.Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2));
    }

    public static void shadow(Graphics2D g, int x, int y, int w, int h, int radius) {
        for (int layer = 3; layer >= 1; layer--) {
            g.setColor(dark ? new Color(0, 0, 0, 26) : new Color(20, 24, 40, 5));
            g.fill(round(x + layer, y + layer + 1, Math.max(1, w - layer * 2), Math.max(1, h - layer * 2),
                    radius));
        }
    }
}
