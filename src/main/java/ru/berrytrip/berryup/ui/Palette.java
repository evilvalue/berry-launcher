package ru.berrytrip.berryup.ui;

import java.awt.Color;
import java.awt.Font;
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
 * Палитра BerryUp: только тёмная пиксельная тема и Monocraft — шрифт,
 * собранный по мотивам Minecraft (OFL, поддерживает кириллицу).
 *
 * <p>Пиксельный стиль — это жёсткие углы, рамки в 2 пикселя, тени без
 * размытия и ограниченная палитра. Поэтому все «скругления» ниже рисуются
 * обычными прямоугольниками, а тень — смещённым тёмным квадратом.</p>
 */
public final class Palette {

    public static final Color BG = new Color(0x0E1015);
    public static final Color BG_SOFT = new Color(0x12151B);
    public static final Color SURFACE = new Color(0x171B23);
    public static final Color SURFACE_ALT = new Color(0x20252F);
    public static final Color HOVER = new Color(0x2A303C);
    public static final Color LINE = new Color(0x39404F);
    public static final Color LINE_SOFT = new Color(0x282E3A);
    public static final Color SHADOW = new Color(0x07080C);

    public static final Color FG = new Color(0xF2F4F8);
    public static final Color MUTED = new Color(0xA9B2C3);
    public static final Color DIM = new Color(0x747E90);

    public static final Color ACCENT = new Color(0xFF5D86);
    public static final Color ACCENT_HOVER = new Color(0xFF7C9E);
    public static final Color ACCENT_DEEP = new Color(0xB62E52);
    public static final Color ACCENT_2 = new Color(0xFFA36B);
    public static final Color VIOLET = new Color(0x9B87FF);
    public static final Color OK = new Color(0x4BE39F);
    public static final Color WARN = new Color(0xF5B942);
    public static final Color ERR = new Color(0xFF6E7B);
    public static final Color TELEGRAM = new Color(0x38AEE0);
    public static final Color ON_ACCENT = new Color(0x1A0B11);

    private static final Map<String, Font> FONTS = new HashMap<String, Font>();
    private static final Font PIXEL_REG = loadFont("/Monocraft.ttf");
    private static final Font PIXEL_SEMI = loadFont("/Monocraft-SemiBold.ttf");

    private Palette() {
    }

    public static boolean isDark() {
        return true;
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

    /** Карточки плоские: глубину даёт жёсткая тень, а не градиент. */
    public static Paint cardGradient(int w, int h) {
        return SURFACE;
    }

    /** Фон окна — один цвет; сетку и ягодки рисует Backdrop. */
    public static Paint backdrop(int w, int h) {
        return BG;
    }

    /** Пиксельная тень: смещённый прямоугольник без размытия. */
    public static void hardShadow(Graphics2D g, int x, int y, int w, int h, int offset) {
        int size = Math.max(1, offset);
        g.setColor(SHADOW);
        g.fillRect(x + size, y + size, w, h);
    }

    public static Shape round(int x, int y, int w, int h, int radius) {
        return new RoundRectangle2D.Float(x, y, w, h, 0, 0);
    }

    /** Радиус намеренно игнорируется: в пиксельном стиле углы прямые. */
    public static void fillRound(Graphics2D g, int x, int y, int w, int h, int radius, Color color) {
        g.setColor(color);
        g.fillRect(x, y, w, h);
    }

    public static void fillRound(Graphics2D g, int x, int y, int w, int h, int radius, Paint paint) {
        g.setPaint(paint);
        g.fillRect(x, y, w, h);
    }

    public static void strokeRound(Graphics2D g, int x, int y, int w, int h, int radius, Color color) {
        g.setColor(color);
        g.drawRect(x, y, w, h);
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
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
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
        hardShadow(g, x, y, w, h, 3);
    }
}
