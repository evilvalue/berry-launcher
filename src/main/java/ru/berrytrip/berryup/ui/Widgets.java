package ru.berrytrip.berryup.ui;

import ru.berrytrip.berryup.Cfg;
import ru.berrytrip.berryup.Sys;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Мелкие элементы интерфейса, нарисованные вручную под тёмную палитру. */
public final class Widgets {

    private Widgets() {
    }

    // ------------------------------------------------------------- текст

    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setOpaque(false);
        return label;
    }

    public static JLabel caption(String text, Color parent) {
        JLabel label = label(text, Palette.semi(8f), Palette.DIM);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        return label;
    }

    /** Абзац с переносом строк — JLabel не умеет переносить текст. */
    public static JTextArea paragraph(String text, Font font, Color color) {
        JTextArea area = new JTextArea(text);
        area.setFont(font);
        area.setForeground(color);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(null);
        area.setRows(Math.max(1, text.split("\n").length));
        return area;
    }

    // ------------------------------------------------------------- карточка

    public static class Card extends JPanel {
        private final int radius;
        private boolean outline = true;

        public Card() {
            this(new java.awt.BorderLayout());
        }

        public Card(LayoutManager layout) {
            super(layout);
            radius = 18;
            setOpaque(false);
        }

        public Card outline(boolean value) {
            outline = value;
            repaint();
            return this;
        }

        public Card pad(int top, int left, int bottom, int right) {
            setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Palette.quality(graphics);
            int w = getWidth();
            int h = getHeight();
            Palette.fillRound(g, 0, 0, w, h, radius, Palette.cardGradient(w, h));
            if (outline) {
                g.setStroke(new BasicStroke(1f));
                Palette.strokeRound(g, 0, 0, w - 1, h - 1, radius, Palette.LINE);
                // тонкая световая кромка сверху: карточка выглядит объёмнее
                g.setColor(Palette.alpha(Color.WHITE, 14));
                g.drawLine(radius, 0, Math.max(radius, w - radius), 0);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    /** Фон всего окна: тёмный градиент со свечением по краям. */
    public static class Backdrop extends JPanel {
        public Backdrop(LayoutManager layout) {
            super(layout);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            // сначала родная заливка (панель непрозрачная), затем градиент поверх неё
            super.paintComponent(graphics);
            Graphics2D g = Palette.quality(graphics);
            int w = getWidth();
            int h = getHeight();
            g.setPaint(Palette.backdrop(w, h));
            g.fillRect(0, 0, w, h);
            glow(g, w - 40, 0, 430, Palette.ACCENT, 34);
            glow(g, -30, h + 40, 360, Palette.VIOLET, 26);
            g.dispose();
        }
    }

    /** Прокручиваемая страница, которая подстраивается под ширину окна. */
    public static class Page extends JPanel implements Scrollable {
        public Page(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(48, visible.height - 48);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    // ------------------------------------------------------------- кнопка

    public static class BerryButton extends JButton {
        public enum Kind {PRIMARY, SECONDARY, GHOST, DANGER, NAV}

        private final Kind kind;
        private boolean hover;

        public BerryButton(String text, Kind kind) {
            super(text);
            this.kind = kind;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(kind == Kind.NAV ? Palette.reg(9.5f) : Palette.semi(9.5f));
            setForeground(Palette.FG);
            setMargin(new java.awt.Insets(0, 0, 0, 0));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            int extra = kind == Kind.NAV ? 28 : 36;
            return new Dimension(size.width + extra, Math.max(kind == Kind.NAV ? 36 : 38, size.height + 12));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Palette.quality(graphics);
            int w = getWidth();
            int h = getHeight();
            boolean enabled = isEnabled();
            boolean pressed = getModel().isPressed();
            Color fill = null;
            Color text;

            switch (kind) {
                case PRIMARY:
                    fill = pressed ? Palette.ACCENT_DEEP : (hover && enabled ? Palette.ACCENT_HOVER : Palette.ACCENT);
                    text = Palette.ON_ACCENT;
                    break;
                case SECONDARY:
                    fill = hover && enabled ? Palette.HOVER : Palette.SURFACE_ALT;
                    text = Palette.FG;
                    break;
                case DANGER:
                    fill = hover && enabled ? new Color(66, 30, 38) : new Color(48, 26, 33);
                    text = Palette.ERR;
                    break;
                case NAV:
                    if (isSelected()) {
                        fill = Palette.alpha(Palette.ACCENT, 26);
                        text = Palette.ACCENT_HOVER;
                    } else {
                        fill = hover ? Palette.alpha(Color.WHITE, 12) : null;
                        text = hover ? Palette.FG : Palette.MUTED;
                    }
                    break;
                case GHOST:
                default:
                    fill = hover && enabled ? Palette.SURFACE_ALT : null;
                    text = hover && enabled ? Palette.FG : Palette.MUTED;
                    break;
            }

            if (!enabled) {
                text = Palette.DIM;
                if (fill != null) fill = Palette.SURFACE_ALT;
            }

            int radius = kind == Kind.NAV ? 9 : 12;
            if (kind == Kind.PRIMARY && enabled) {
                Palette.fillRound(g, 1, 2, w - 2, h - 2, radius, Palette.alpha(Palette.ACCENT, 46));
            }

            if (fill != null) {
                if (kind == Kind.PRIMARY && enabled && !pressed) {
                    Palette.fillRound(g, 0, 0, w, h - 2, radius, Palette.accentGradient(0, 0, w, h));
                } else {
                    Palette.fillRound(g, 0, 0, w, Math.max(1, h - (kind == Kind.PRIMARY ? 2 : 0)), radius, fill);
                }
                if (kind == Kind.SECONDARY || kind == Kind.DANGER) {
                    g.setStroke(new BasicStroke(1f));
                    Palette.strokeRound(g, 0, 0, w - 1, h - 1, radius,
                            kind == Kind.DANGER ? new Color(88, 40, 52) : Palette.LINE);
                }
            }

            if (isFocusOwner()) {
                g.setStroke(new BasicStroke(1.2f));
                Palette.strokeRound(g, 1, 1, w - 3, h - 3, radius, Palette.alpha(Palette.ACCENT, 150));
            }

            Font font = getFont();
            g.setFont(font);
            g.setColor(text);
            java.awt.FontMetrics metrics = g.getFontMetrics();
            int textWidth = metrics.stringWidth(getText());
            int x = (w - textWidth) / 2;
            int y = (h - (kind == Kind.PRIMARY ? 2 : 0) - metrics.getHeight()) / 2 + metrics.getAscent();
            g.drawString(getText(), Math.max(6, x), y);
            g.dispose();
        }
    }

    // ------------------------------------------------------------- тумблер

    public static class Toggle extends JComponent {
        private final String title;
        private final String hint;
        private boolean selected;
        private boolean hover;
        private boolean focus;
        private Consumer<Boolean> listener;

        public Toggle(String title, String hint, boolean selected) {
            this.title = title;
            this.hint = hint;
            this.selected = selected;
            setOpaque(false);
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(240, hint == null || hint.isEmpty() ? 34 : 42));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (!isEnabled()) return;
                    requestFocusInWindow();
                    setSelected(!Toggle.this.selected);
                }
            });
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (isEnabled()) setSelected(!Toggle.this.selected);
                        e.consume();
                    }
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    focus = true;
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    focus = false;
                    repaint();
                }
            });
        }

        public Toggle(String title, boolean selected) {
            this(title, null, selected);
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean value, boolean notify) {
            selected = value;
            repaint();
            if (notify && listener != null) listener.accept(value);
        }

        public void setSelected(boolean value) {
            setSelected(value, true);
        }

        public void onChange(Consumer<Boolean> listener) {
            this.listener = listener;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Palette.quality(graphics);
            int w = getWidth();
            int h = getHeight();

            int trackW = 38;
            int trackH = 22;
            int tx = w - trackW - 2;
            int ty = hint == null || hint.isEmpty() ? (h - trackH) / 2 : 8;

            Color track = selected ? Palette.ACCENT : (hover ? Palette.HOVER : Palette.SURFACE_ALT);
            if (!isEnabled()) track = Palette.SURFACE_ALT;
            Palette.fillRound(g, tx, ty, trackW, trackH, 11, track);
            if (!selected) {
                g.setStroke(new BasicStroke(1f));
                Palette.strokeRound(g, tx, ty, trackW - 1, trackH - 1, 11, Palette.LINE);
            }
            int knob = 16;
            int kx = selected ? tx + trackW - knob - 3 : tx + 3;
            g.setColor(new Color(255, 255, 255, selected ? 245 : 150));
            g.fillOval(kx, ty + 3, knob, knob);

            if (focus) {
                g.setStroke(new BasicStroke(1.2f));
                Palette.strokeRound(g, 0, 0, w - 1, h - 1, 9, Palette.alpha(Palette.ACCENT, 130));
            }

            int textW = Math.max(20, tx - 12);
            g.setFont(Palette.reg(9.5f));
            g.setColor(isEnabled() ? Palette.FG : Palette.DIM);
            g.drawString(clip(title, g, textW), 0, hint == null || hint.isEmpty() ? ty + 15 : 15);

            if (hint != null && !hint.isEmpty()) {
                g.setFont(Palette.reg(8f));
                g.setColor(Palette.DIM);
                g.drawString(clip(hint, g, textW), 0, 30);
            }
            g.dispose();
        }
    }

    // ------------------------------------------------------------- ссылка

    public enum Glyph {SITE, TELEGRAM}

    public static class LinkCard extends JComponent {
        private final String caption;
        private final String detail;
        private final String url;
        private final Glyph glyph;
        private boolean hover;
        private boolean focus;

        public LinkCard(String caption, String detail, String url, Glyph glyph) {
            this.caption = caption;
            this.detail = detail;
            this.url = url;
            this.glyph = glyph;
            setOpaque(false);
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(400, 64));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    open();
                }
            });
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
                        open();
                        e.consume();
                    }
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    focus = true;
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    focus = false;
                    repaint();
                }
            });
        }

        private void open() {
            Sys.openUrl(url);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Palette.quality(graphics);
            int w = getWidth();
            int h = getHeight();
            if (w <= 2 || h <= 2) {
                g.dispose();
                return;
            }

            Palette.fillRound(g, 0, 0, w, h, 16, hover ? Palette.HOVER : Palette.cardGradient(w, h));
            g.setStroke(new BasicStroke(1f));
            Palette.strokeRound(g, 0, 0, w - 1, h - 1, 16, hover || focus ? Palette.alpha(Palette.ACCENT, 130) : Palette.LINE);

            int badge = 38;
            int bx = 16;
            int by = (h - badge) / 2;
            if (glyph == Glyph.TELEGRAM) {
                Palette.fillRound(g, bx, by, badge, badge, 12, Palette.alpha(Palette.TELEGRAM, 60));
                g.setColor(Palette.TELEGRAM);
                g.setStroke(new BasicStroke(1f));
                Palette.strokeRound(g, bx, by, badge, badge, 12, Palette.alpha(Palette.TELEGRAM, 110));
            } else {
                Palette.fillRound(g, bx, by, badge, badge, 12, Palette.alpha(Palette.ACCENT, 40));
                g.setColor(Palette.ACCENT);
                g.setStroke(new BasicStroke(1f));
                Palette.strokeRound(g, bx, by, badge, badge, 12, Palette.alpha(Palette.ACCENT, 110));
            }
            g.setColor(glyph == Glyph.TELEGRAM ? Palette.TELEGRAM : Palette.ACCENT_HOVER);
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = bx + badge / 2;
            int cy = by + badge / 2;
            if (glyph == Glyph.TELEGRAM) {
                int[] xs = {cx - 9, cx + 9, cx + 3, cx - 2};
                int[] ys = {cy + 2, cy - 7, cy + 8, cy + 2};
                java.awt.Polygon plane = new java.awt.Polygon(xs, ys, 4);
                g.fillPolygon(plane);
            } else {
                int r = 9;
                g.drawOval(cx - r, cy - r, r * 2, r * 2);
                g.drawLine(cx - r, cy, cx + r, cy);
                g.drawOval(cx - r / 2, cy - r, r, r * 2);
            }

            int textX = bx + badge + 14;
            int textW = Math.max(60, w - textX - 44);
            g.setFont(Palette.semi(10.5f));
            g.setColor(Palette.FG);
            g.drawString(clip(caption, g, textW), textX, h / 2 - 3);
            g.setFont(Palette.reg(8.5f));
            g.setColor(Palette.MUTED);
            g.drawString(clip(detail, g, textW), textX, h / 2 + 15);

            int ax = w - 26;
            int ay = h / 2;
            g.setColor(hover ? Palette.ACCENT : Palette.DIM);
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(ax - 5, ay, ax + 4, ay);
            g.drawLine(ax, ay - 4, ax + 4, ay);
            g.drawLine(ax, ay + 4, ax + 4, ay);
            g.dispose();
        }
    }

    // ------------------------------------------------------------- логотип

    public static class Logo extends JComponent {
        private static BufferedImage image;

        public Logo(int size) {
            setOpaque(false);
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
            if (image == null) image = load();
        }

        private static BufferedImage load() {
            try (InputStream stream = Logo.class.getResourceAsStream("/berry.png")) {
                if (stream != null) return ImageIO.read(stream);
            } catch (Exception ignored) {
            }
            return null;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Palette.quality(graphics);
            int size = Math.min(getWidth(), getHeight());
            // мягкое ягодное свечение под иконкой
            for (int i = 3; i >= 1; i--) {
                g.setColor(Palette.alpha(Palette.ACCENT, 10));
                g.fillOval(-i * 2, size / 6 - i * 2, size + i * 4, size + i * 4);
            }
            if (image != null) {
                g.drawImage(image, 0, 0, size, size, null);
            } else {
                g.setColor(Palette.ACCENT);
                g.fillOval(0, 0, size, size);
                g.setColor(Palette.BG);
                g.setFont(Palette.semi(size * 0.45f));
                g.drawString("B", size / 3, (int) (size * 0.68));
            }
            g.dispose();
        }
    }

    // ------------------------------------------------------------- навигация

    public static class Segmented extends JPanel {
        public interface Listener {
            void selected(int index);
        }

        private final List<BerryButton> items = new ArrayList<BerryButton>();
        private Listener listener;
        private int selected;

        public Segmented(String... names) {
            setOpaque(false);
            setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            for (int i = 0; i < names.length; i++) {
                final int index = i;
                BerryButton button = new BerryButton(names[i], BerryButton.Kind.NAV);
                button.addActionListener(e -> select(index, true));
                add(button);
                items.add(button);
            }
            select(0, false);
        }

        public void onSelect(Listener listener) {
            this.listener = listener;
        }

        public int selected() {
            return selected;
        }

        public void select(int index, boolean notify) {
            selected = Math.max(0, Math.min(items.size() - 1, index));
            for (int i = 0; i < items.size(); i++) items.get(i).setSelected(i == selected);
            if (notify && listener != null) listener.selected(selected);
        }

        public List<BerryButton> items() {
            return items;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Palette.quality(graphics);
            int w = getWidth();
            int h = getHeight();
            Palette.fillRound(g, 0, 0, w, h, 13, Palette.alpha(Palette.BG_SOFT, 230));
            g.setStroke(new BasicStroke(1f));
            Palette.strokeRound(g, 0, 0, w - 1, h - 1, 13, Palette.LINE);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    // ------------------------------------------------------------- слайдер

    public static class MemorySlider extends JComponent {
        private final int min;
        private final int max;
        private int value;
        private boolean dragging;
        private Consumer<Integer> listener;

        public MemorySlider(int min, int max, int value) {
            this.min = min;
            this.max = Math.max(min + 1, max);
            this.value = Math.max(min, Math.min(this.max, value));
            setOpaque(false);
            setFocusable(true);
            setPreferredSize(new Dimension(200, 28));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    dragging = true;
                    setFromMouse(e.getX());
                    requestFocusInWindow();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    dragging = false;
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragging) setFromMouse(e.getX());
                }
            });
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_LEFT) setValue(value - 1);
                    if (e.getKeyCode() == KeyEvent.VK_RIGHT) setValue(value + 1);
                }
            });
        }

        public void onChange(Consumer<Integer> listener) {
            this.listener = listener;
        }

        public int value() {
            return value;
        }

        public void setValue(int value) {
            int next = Math.max(min, Math.min(max, value));
            if (next == this.value) return;
            this.value = next;
            repaint();
            if (listener != null) listener.accept(next);
        }

        private void setFromMouse(int x) {
            int pad = 12;
            int width = Math.max(1, getWidth() - pad * 2);
            double ratio = (double) (x - pad) / width;
            setValue((int) Math.round(min + ratio * (max - min)));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Palette.quality(graphics);
            int w = getWidth();
            int h = getHeight();
            int pad = 12;
            int trackY = h / 2 - 2;
            int trackW = Math.max(1, w - pad * 2);
            Palette.fillRound(g, pad, trackY, trackW, 4, 2, Palette.SURFACE_ALT);
            int filled = (int) (trackW * (value - min) / (double) (max - min));
            Palette.fillRound(g, pad, trackY, Math.max(4, filled), 4, 2, Palette.ACCENT);
            int knob = 16;
            int kx = pad + filled - knob / 2;
            g.setColor(Palette.BG);
            g.fillOval(kx - 2, trackY - 4, knob + 4, knob + 4);
            g.setColor(Palette.ACCENT);
            g.fillOval(kx, trackY - 2, knob, knob);
            g.dispose();
        }
    }

    /** Мягкое радиальное свечение без видимой границы круга. */
    static void glow(Graphics2D g, int cx, int cy, int radius, Color color, int alpha) {
        g.setPaint(new java.awt.RadialGradientPaint(
                new java.awt.geom.Point2D.Float(cx, cy), Math.max(1, radius),
                new float[]{0f, 1f},
                new Color[]{Palette.alpha(color, alpha), Palette.alpha(color, 0)}));
        g.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
    }

    static String clip(String text, Graphics2D g, int width) {
        if (text == null) return "";
        if (g.getFontMetrics().stringWidth(text) <= width) return text;
        String ellipsis = "…";
        int index = text.length();
        while (index > 0 && g.getFontMetrics().stringWidth(text.substring(0, index) + ellipsis) > width) index--;
        return text.substring(0, index) + ellipsis;
    }

    /** Точка статуса сервера. */
    public static class Dot extends JComponent {
        private Color color = Palette.DIM;

        public Dot() {
            setOpaque(false);
            setPreferredSize(new Dimension(10, 10));
            setMaximumSize(new Dimension(10, 10));
        }

        public void setColor(Color color) {
            this.color = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Palette.quality(graphics);
            int size = Math.min(10, Math.min(getWidth(), getHeight()));
            g.setColor(color);
            g.fillOval(0, (getHeight() - size) / 2, size, size);
            g.dispose();
        }
    }

    public static void main(String[] args) {
        // быстрый визуальный прогон виджетов без запуска лаунчера
        javax.swing.SwingUtilities.invokeLater(() -> {
            JPanel panel = new JPanel();
            panel.setBackground(Palette.BG);
            panel.setLayout(new FlowLayout());
            panel.add(new BerryButton("Играть", BerryButton.Kind.PRIMARY));
            panel.add(new BerryButton("Обновить сборку", BerryButton.Kind.SECONDARY));
            panel.add(new LinkCard("Сайт сборки", "evilvalue.github.io/berry", Cfg.SITE, Glyph.SITE));
            javax.swing.JFrame frame = new javax.swing.JFrame("widgets");
            frame.setContentPane(panel);
            frame.setSize(600, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
