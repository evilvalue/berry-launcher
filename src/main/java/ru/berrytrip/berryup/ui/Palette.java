package ru.berrytrip.berryup.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;

/** Палитра BerryUp: тёмный фон, розовый акцент и общие радиусы скругления. */
public final class Palette {

    public static final Color BG = new Color(13, 14, 18);
    public static final Color SURFACE = new Color(22, 24, 30);
    public static final Color SURFACE_ALT = new Color(28, 31, 39);
    public static final Color HOVER = new Color(36, 40, 50);
    public static final Color LINE = new Color(41, 45, 56);
    public static final Color FG = new Color(236, 239, 245);
    public static final Color MUTED = new Color(147, 156, 172);
    public static final Color DIM = new Color(104, 113, 129);
    public static final Color ACCENT = new Color(233, 84, 120);
    public static final Color ACCENT_HOVER = new Color(246, 112, 144);
    public static final Color ACCENT_DEEP = new Color(188, 51, 85);
    public static final Color OK = new Color(74, 222, 128);
    public static final Color WARN = new Color(250, 191, 36);
    public static final Color ERR = new Color(248, 113, 113);
    public static final Color TELEGRAM = new Color(42, 171, 238);

    private static final Map<String, Font> FONTS = new HashMap<String, Font>();

    private Palette() {
    }

    public static Font reg(float size) {
        return font("Segoe UI", Font.PLAIN, size);
    }

    public static Font semi(float size) {
        return font("Segoe UI Semibold", Font.PLAIN, size);
    }

    public static Font mono(float size) {
        return font("Consolas", Font.PLAIN, size);
    }

    private static Font font(String family, int style, float size) {
        String key = family + "|" + style + "|" + size;
        synchronized (FONTS) {
            Font cached = FONTS.get(key);
            if (cached != null) return cached;
            Font font = new Font(family, style, 12).deriveFont(size);
            if (!font.getFamily().equalsIgnoreCase(family)) {
                font = new Font(Font.SANS_SERIF, style, 12).deriveFont(size);
            }
            FONTS.put(key, font);
            return font;
        }
    }

    public static Shape round(int x, int y, int w, int h, int radius) {
        return new RoundRectangle2D.Float(x, y, w, h, radius * 2, radius * 2);
    }

    public static void fillRound(Graphics2D g, int x, int y, int w, int h, int radius, Color color) {
        g.setColor(color);
        g.fill(round(x, y, w, h, radius));
    }

    public static void strokeRound(Graphics2D g, int x, int y, int w, int h, int radius, Color color) {
        g.setColor(color);
        g.draw(round(x, y, w, h, radius));
    }

    public static Graphics2D quality(java.awt.Graphics g) {
        Graphics2D copy = (Graphics2D) g.create();
        copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        copy.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
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
