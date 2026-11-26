package ru.berrytrip.berryup.ui;

import ru.berrytrip.berryup.Sys;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
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

/** Элементы интерфейса BerryUp: светлые карточки, плоские кнопки, аккуратные переключатели. */
public final class Widgets {

    private Widgets() {
    }

    // ------------------------------------------------------------- текст

    /** Жёсткий вертикальный отступ: в отличие от Box-страта не растягивается. */
    public static java.awt.Component vgap(int height) {
        return Box.createRigidArea(new Dimension(0, height));
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setOpaque(false);
        return label;
    }

    public static JLabel caption(String text, Color parent) {
        JLabel label = label(text, Palette.semi(8.5f), Palette.DIM);
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

    /** Небольшая цветная плашка: «готово к игре», «GIF», «3D» и т. п. */
    public static class Chip extends JComponent {
        private String text;
        private Color color;

        public Chip(String text, Color color) {
            this.text = text;
            this.color = color;
            setOpaque(false);
            setFont(Palette.semi(8.6f));
            setForeground(color);
        }

        public void setText(String text) {
            this.text = text;
            revalidate();
            repaint();
        }

        public void setColor(Color color) {
            this.color = color;
            setForeground(color);
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            java.awt.FontMetrics metrics = getFontMetrics(getFont());
            return new Dimension(metrics.stringWidth(text) + 22, 24);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Palette.quality(graphics);
            int w = getWidth();
            int h = getHeight();
            Palette.fillRound(g, 0, 0, w, h, 3, Palette.mix(Palette.SURFACE, color, 0.10f));
            g.setStroke(new BasicStroke(1f));
            Palette.strokeRound(g, 0, 0, w - 1, h - 1, 3, Palette.alpha(color, 60));
            g.setFont(getFont());
            g.setColor(color);
            java.awt.FontMetrics metrics = g.getFontMetrics();
            g.drawString(text, (w - metrics.stringWidth(text)) / 2,
                    (h - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    }

    // ------------------------------------------------------------- карточка

    public static class Card extends JPanel {
        private final int radius;
        private boolean outline = true;
        private Color tint;

        public Card() {
            this(new java.awt.BorderLayout());
        }

        public Card(LayoutManager layout) {
            super(layout);
            radius = 6;
            setOpaque(false);
        }

        public Card outline(boolean value) {
            outline = value;
            repaint();
            return this;
        }

        /** Лёгкая цветная подложка карточки: предупреждение, акцент, успех. */
        public Card tint(Color value) {
            tint = value;
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
            if (w <= 2 || h <= 2) {
                g.dispose();
                return;
            }
            java.awt.Paint paint = tint == null
                    ? Palette.cardGradient(w, h)
                    : new java.awt.GradientPaint(0, 0, Palette.mix(Palette.SURFACE, tint, 0.07f),
                    0, h, Palette.mix(Palette.SURFACE, tint, 0.02f));
            Palette.fillRound(g, 0, 0, w, h, radius, paint);
            if (outline) {
                g.setStroke(new BasicStroke(1f));
                Palette.strokeRound(g, 0, 0, w - 1, h - 1, radius,
                        tint == null ? Palette.LINE : Palette.alpha(tint, 70));
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    /** Фон окна: спокойный нейтральный градиент без цветных пятен. */
    public static class Backdrop extends JPanel {
        public Backdrop(LayoutManager layout) {
            super(layout);
            setOpaque(true);
            setBackground(Palette.BG);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = Palette.quality(graphics);
            int w = getWidth();
            int h = getHeight();
            g.setPaint(Palette.backdrop(w, h));
            g.fillRect(0, 0, w, h);
            g.dispose();
        }
    }

    /**
     * Тело прокручиваемой страницы: растягивается по ширине окна и берёт
     * высоту содержимого, чтобы при маленьком окне появилась прокрутка.
     */
    public static class ScrollBody extends JPanel implements Scrollable {
        public ScrollBody(java.awt.Component view) {
            super(new java.awt.BorderLayout());
            setOpaque(false);
            add(view, java.awt.BorderLayout.NORTH);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 22;
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

    /** Прозрачный scroll pane для страницы или колонки. */
    public static javax.swing.JScrollPane scroll(java.awt.Component view) {
        javax.swing.JScrollPane pane = new javax.swing.JScrollPane(new ScrollBody(view));
        pane.setBorder(null);
        pane.setOpaque(false);
        pane.getViewport().setOpaque(false);
        pane.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pane.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        pane.getVerticalScrollBar().setUnitIncrement(22);
        pane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 48));
        return pane;
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
            return 22;
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
            int extra = kind == Kind.NAV ? 26 : 34;
            return new Dimension(size.width + extra, Math.max(kind == Kind.NAV ? 34 : 40, size.height + 14));
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
            Color border = null;

            switch (kind) {
                case PRIMARY:
                    fill = pressed ? Palette.ACCENT_DEEP : (hover && enabled ? Palette.ACCENT_HOVER : Palette.ACCENT);
                    text = Palette.ON_ACCENT;
                    break;
                case SECONDARY:
                    fill = hover && enabled ? Palette.SURFACE_ALT : Palette.SURFACE;
                    border = hover && enabled ? Palette.alpha(Palette.ACCENT, 90) : Palette.LINE;
                    text = Palette.FG;
                    break;
                case DANGER:
                    fill = hover && enabled ? Palette.mix(Palette.SURFACE, Palette.ERR, 0.12f)
                            : Palette.mix(Palette.SURFACE, Palette.ERR, 0.07f);
                    text = Palette.ERR;
                    break;
                case NAV:
                    if (isSelected()) {
                        fill = Palette.SURFACE;
                        border = Palette.LINE;
                        text = Palette.FG;
                    } else {
                        fill = hover ? Palette.HOVER : null;
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
                border = Palette.LINE;
            }

            int radius = 3;
            if (fill != null) {
                Palette.fillRound(g, 0, 0, w, h, radius, fill);
            }
            if (border != null) {
                g.setStroke(new BasicStroke(1f));
                Palette.strokeRound(g, 0, 0, w - 1, h - 1, radius, border);
            }
            if (isFocusOwner()) {
                g.setStroke(new BasicStroke(1.4f));
                Palette.strokeRound(g, 1, 1, w - 3, h - 3, radius, Palette.alpha(Palette.ACCENT, 140));
            }

            g.setFont(getFont());
            g.setColor(text);
            java.awt.FontMetrics metrics = g.getFontMetrics();
            int x = (w - metrics.stringWidth(getText())) / 2;
            int y = (h - metrics.getHeight()) / 2 + metrics.getAscent();
            g.drawString(getText(), Math.max(6, x), y);
            g.dispose();
        }
    }

    /**
     * Кнопка для тёмной hero-сцены: срезанные углы вместо скруглений,
     * пиксельная тень и опциональная иконка Telegram.
     */
    public static class PixelButton extends JButton {
        public enum Tone {PRIMARY, SECONDARY, GHOST}

        private final Tone tone;
        private boolean hover;
        private boolean compact;
        private boolean telegram;

        public PixelButton(String text, Tone tone) {
            super(text);
            this.tone = tone;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(Palette.semi(10.5f));
            setForeground(Color.WHITE);
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

        public PixelButton telegram() {
            telegram = true;
            return this;
        }

        public PixelButton compact() {
            compact = true;
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(size.width + 36, compact ? 38 : 50);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, compact ? 38 : 50);
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
            boolean enabled = isEnabled();
            boolean pressed = getModel().isPressed();
            java.awt.Shape shape = pixelShape(w, h - 2, 4);

            Color textColor;
            if (tone == Tone.PRIMARY) {
                g.setColor(new Color(0, 0, 0, 80));
                g.fill(pixelShape(3, h - 2, 4));
                g.setPaint(Palette.accentGradient(0, 0, w, h));
                if (!enabled) {
                    g.setColor(Palette.mix(Palette.ACCENT, new Color(0x241C28), 0.62f));
                } else if (pressed) {
                    g.setColor(Palette.ACCENT_DEEP);
                } else if (hover) {
                    g.setColor(Palette.ACCENT_HOVER);
                }
                g.fill(shape);
                g.setColor(Palette.alpha(Color.WHITE, 55));
                g.draw(shape);
                textColor = enabled ? Palette.ON_ACCENT : Palette.alpha(Color.WHITE, 130);
            } else if (tone == Tone.SECONDARY) {
                g.setColor(hover && enabled
                        ? Palette.alpha(Color.WHITE, 24)
                        : Palette.alpha(Color.WHITE, 12));
                g.fill(shape);
                g.setColor(hover && enabled
                        ? Palette.alpha(Palette.ACCENT, 130)
                        : Palette.alpha(Color.WHITE, 54));
                g.draw(shape);
                textColor = enabled ? new Color(0xF4EDF7) : Palette.alpha(Color.WHITE, 100);
            } else {
                if (hover && enabled) {
                    g.setColor(Palette.alpha(Color.WHITE, 10));
                    g.fill(shape);
                }
                g.setColor(hover && enabled ? Palette.alpha(Palette.ACCENT, 90) : Palette.alpha(Color.WHITE, 30));
                g.draw(shape);
                textColor = enabled ? Palette.alpha(Color.WHITE, 175) : Palette.alpha(Color.WHITE, 80);
            }

            g.setFont(getFont());
            java.awt.FontMetrics metrics = g.getFontMetrics();
            int icon = telegram ? 18 : 0;
            int total = metrics.stringWidth(getText()) + icon;
            int x = Math.max(10, (w - total) / 2);
            int y = (h - metrics.getHeight()) / 2 + metrics.getAscent();
            if (telegram) {
                int cx = x + 7;
                int cy = h / 2 - 1;
                g.setColor(textColor);
                g.fillPolygon(new java.awt.Polygon(
                        new int[]{cx - 6, cx + 7, cx + 1, cx - 3},
                        new int[]{cy + 1, cy - 5, cy + 6, cy + 2}, 4));
                x += icon;
            }
            g.setColor(textColor);
            g.drawString(getText(), x, y);
            g.dispose();
        }

        private static java.awt.Shape pixelShape(int w, int h, int cut) {
            return new java.awt.Polygon(
                    new int[]{cut, w - cut, w, w, w - cut, cut, 0, 0},
                    new int[]{0, 0, cut, h - cut, h, h, h - cut, cut}, 8);
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
            setPreferredSize(new Dimension(260, hint == null || hint.isEmpty() ? 34 : 40));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, hint == null || hint.isEmpty() ? 34 : 40));
            // Минимум как у обычного вида: BoxLayout иначе сжимает тумблеры
            // до нуля, и подписи наезжают друг на друга.
            setMinimumSize(new Dimension(120, hint == null || hint.isEmpty() ? 34 : 40));
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

            int trackW = 42;
            int trackH = 24;
            int tx = w - trackW - 2;
            int ty = hint == null || hint.isEmpty() ? (h - trackH) / 2 : 8;

            Color track = selected ? Palette.ACCENT : (hover ? Palette.HOVER
                    : Palette.mix(Palette.SURFACE_ALT, Palette.LINE, 0.6f));
            if (!isEnabled()) track = Palette.mix(Palette.SURFACE_ALT, Palette.LINE, 0.6f);
            Palette.fillRound(g, tx, ty, trackW, trackH, 12, track);
            if (!selected) {
                g.setStroke(new BasicStroke(1f));
                Palette.strokeRound(g, tx, ty, trackW - 1, trackH - 1, 12, Palette.LINE);
            }
            int knob = 18;
            int kx = selected ? tx + trackW - knob - 3 : tx + 3;
            g.setColor(new Color(0, 0, 0, 22));
            g.fillOval(kx, ty + 4, knob, knob);
            g.setColor(Color.WHITE);
            g.fillOval(kx, ty + 3, knob, knob);

            if (focus) {
                g.setStroke(new BasicStroke(1.4f));
                Palette.strokeRound(g, 0, 0, w - 1, h - 1, 10, Palette.alpha(Palette.ACCENT, 120));
            }

            int textW = Math.max(20, tx - 12);
            g.setFont(Palette.reg(10f));
            g.setColor(isEnabled() ? Palette.FG : Palette.DIM);
            g.drawString(clip(title, g, textW), 0, hint == null || hint.isEmpty() ? ty + 16 : 15);

            if (hint != null && !hint.isEmpty()) {
                g.setFont(Palette.reg(8.8f));
                g.setColor(Palette.DIM);
                g.drawString(clip(hint, g, textW), 0, 31);
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
            setPreferredSize(new Dimension(300, 72));
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

            Palette.fillRound(g, 0, 0, w, h, 18,
                    hover ? Palette.mix(Palette.SURFACE, Palette.ACCENT, 0.04f) : Palette.SURFACE);
            g.setStroke(new BasicStroke(1f));
            Palette.strokeRound(g, 0, 0, w - 1, h - 1, 18,
                    hover || focus ? Palette.alpha(Palette.ACCENT, 120) : Palette.LINE);

            Color tone = glyph == Glyph.TELEGRAM ? Palette.TELEGRAM : Palette.ACCENT;
            int badge = 42;
            int bx = 14;
            int by = (h - badge) / 2;
            Palette.fillRound(g, bx, by, badge, badge, 13, Palette.mix(Palette.SURFACE, tone, 0.12f));

            g.setColor(tone);
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = bx + badge / 2;
            int cy = by + badge / 2;
            if (glyph == Glyph.TELEGRAM) {
                int[] xs = {cx - 8, cx + 8, cx + 3, cx - 2};
                int[] ys = {cy + 1, cy - 6, cy + 6, cy + 1};
                g.fillPolygon(new java.awt.Polygon(xs, ys, 4));
            } else {
                int r = 8;
                g.drawOval(cx - r, cy - r, r * 2, r * 2);
                g.drawLine(cx - r, cy, cx + r, cy);
                g.drawOval(cx - r / 2, cy - r, r, r * 2);
            }

            int textX = bx + badge + 13;
            int textW = Math.max(60, w - textX - 34);
            g.setFont(Palette.semi(10.5f));
            g.setColor(Palette.FG);
            g.drawString(clip(caption, g, textW), textX, h / 2 - 2);
            g.setFont(Palette.reg(9f));
            g.setColor(Palette.MUTED);
            g.drawString(clip(detail, g, textW), textX, h / 2 + 16);

            int ax = w - 22;
            int ay = h / 2;
            g.setColor(hover || focus ? Palette.ACCENT : Palette.DIM);
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
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            int size = Math.min(getWidth(), getHeight());
            if (image != null) {
                g.drawImage(image, 0, 0, size, size, null);
            } else {
                g.setColor(Palette.ACCENT);
                g.fillRoundRect(0, 0, size, size, size / 3, size / 3);
                g.setColor(Color.WHITE);
                g.setFont(Palette.bold(size * 0.45f));
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
            Palette.fillRound(g, 0, 0, w, h, 4, Palette.SURFACE_ALT);
            g.setStroke(new BasicStroke(1f));
            Palette.strokeRound(g, 0, 0, w - 1, h - 1, 4, Palette.LINE);
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
            setPreferredSize(new Dimension(200, 30));
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
            int trackY = h / 2 - 3;
            int trackW = Math.max(1, w - pad * 2);
            Palette.fillRound(g, pad, trackY, trackW, 6, 3,
                    Palette.mix(Palette.SURFACE_ALT, Palette.LINE, 0.7f));
            int filled = (int) (trackW * (value - min) / (double) (max - min));
            Palette.fillRound(g, pad, trackY, Math.max(6, filled), 6, 3, Palette.ACCENT);
            int knob = 18;
            int kx = pad + filled - knob / 2;
            g.setColor(new Color(0, 0, 0, 20));
            g.fillOval(kx, trackY - 4, knob, knob);
            g.setColor(Color.WHITE);
            g.fillOval(kx, trackY - 6, knob, knob);
            g.setColor(Palette.ACCENT);
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(kx, trackY - 6, knob, knob);
            g.dispose();
        }
    }

    /** Мягкое радиальное свечение (совместимость со старыми экранами). */
    static void glow(Graphics2D g, int cx, int cy, int radius, Color color, int alpha) {
        Palette.glow(g, cx, cy, radius, color, alpha);
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
            int size = Math.min(9, Math.min(getWidth(), getHeight()));
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            g.setColor(Palette.alpha(color, 40));
            g.fillOval(x - 3, y - 3, size + 6, size + 6);
            g.setColor(color);
            g.fillOval(x, y, size, size);
            g.dispose();
        }
    }

}
