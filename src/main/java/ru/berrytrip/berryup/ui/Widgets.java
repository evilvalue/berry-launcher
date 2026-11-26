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

/**
 * Элементы интерфейса BerryUp. Всё нарисовано по правилам пиксель-арта:
 * прямые углы, рамки в 2 пикселя, жёсткая тень без размытия, ограниченная
 * палитра. Единственные непиксельные элементы — 3D-модель скина и логотип.
 */
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

    /** Пиксельная иконка из символьной карты: '.' — прозрачно, остальное — цвета. */
    public static void pixelArt(Graphics2D g, int x, int y, int cell, String[] rows,
                                Color[] colors, char[] keys) {
        for (int row = 0; row < rows.length; row++) {
            String line = rows[row];
            for (int col = 0; col < line.length(); col++) {
                char ch = line.charAt(col);
                if (ch == '.' || ch == ' ') continue;
                Color color = null;
                for (int i = 0; i < keys.length; i++) {
                    if (keys[i] == ch) {
                        color = colors[i];
                        break;
                    }
                }
                if (color == null) continue;
                g.setColor(color);
                g.fillRect(x + col * cell, y + row * cell, cell, cell);
            }
        }
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
            Palette.fillRound(g, 0, 0, w, h, 0, Palette.mix(Palette.SURFACE, color, 0.12f));
            g.setStroke(new BasicStroke(2f));
            Palette.strokeRound(g, 1, 1, w - 3, h - 3, 0, Palette.alpha(color, 120));
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
        private boolean outline = true;
        private Color tint;

        public Card() {
            this(new java.awt.BorderLayout());
        }

        public Card(LayoutManager layout) {
            super(layout);
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
            if (w <= 4 || h <= 4) {
                g.dispose();
                return;
            }
            int cw = w - 4;
            int ch = h - 4;
            Palette.hardShadow(g, 0, 0, cw, ch, 4);
            Color base = tint == null ? Palette.SURFACE : Palette.mix(Palette.SURFACE, tint, 0.09f);
            Palette.fillRound(g, 0, 0, cw, ch, 0, base);
            if (outline) {
                g.setStroke(new BasicStroke(2f));
                Palette.strokeRound(g, 1, 1, cw - 2, ch - 2, 0,
                        tint == null ? Palette.LINE : Palette.alpha(tint, 180));
            }
            // Внутренняя светлая грань — объём без градиентов.
            g.setColor(Palette.alpha(Palette.FG, 8));
            g.drawLine(3, 3, cw - 4, 3);
            g.drawLine(3, 3, 3, ch - 4);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    /** Фон окна: тёмная сетка и пара пиксельных ягод в углу. */
    public static class Backdrop extends JPanel {
        private static BufferedImage berry;

        public Backdrop(LayoutManager layout) {
            super(layout);
            setOpaque(true);
            setBackground(Palette.BG);
            if (berry == null) {
                try (InputStream stream = Backdrop.class.getResourceAsStream("/berry.png")) {
                    if (stream != null) berry = ImageIO.read(stream);
                } catch (Exception ignored) {
                }
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = Palette.quality(graphics);
            int w = getWidth();
            int h = getHeight();
            g.setPaint(Palette.backdrop(w, h));
            g.fillRect(0, 0, w, h);

            g.setColor(Palette.alpha(Palette.FG, 5));
            for (int x = 0; x < w; x += 32) g.drawLine(x, 0, x, h);
            for (int y = 0; y < h; y += 32) g.drawLine(0, y, w, y);

            if (berry != null) {
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.setComposite(java.awt.AlphaComposite.getInstance(
                        java.awt.AlphaComposite.SRC_OVER, 0.16f));
                g.drawImage(berry, w - 104, h - 110, 64, 64, null);
                g.setComposite(java.awt.AlphaComposite.getInstance(
                        java.awt.AlphaComposite.SRC_OVER, 0.10f));
                g.drawImage(berry, w - 156, h - 58, 30, 30, null);
                g.setComposite(java.awt.AlphaComposite.SrcOver);
            }
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

    /** Прозрачный scroll pane для страницы или колонки. */
    public static javax.swing.JScrollPane scroll(java.awt.Component view) {
        javax.swing.JScrollPane pane = new javax.swing.JScrollPane(new ScrollBody(view));
        pane.setBorder(null);
        pane.setOpaque(false);
        pane.getViewport().setOpaque(false);
        pane.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pane.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        pane.getVerticalScrollBar().setUnitIncrement(24);
        pane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 48));
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
            int extra = kind == Kind.NAV ? 26 : 36;
            return new Dimension(size.width + extra,
                    Math.max(kind == Kind.NAV ? 32 : 42, size.height + 14));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = Palette.quality(graphics);
            int w = getWidth();
            int h = getHeight();
            boolean enabled = isEnabled();
            boolean pressed = getModel().isPressed();
            int shadow = kind == Kind.GHOST || kind == Kind.NAV ? 0 : 3;
            int cw = w - shadow;
            int ch = h - shadow;

            Color fill = null;
            Color text;
            Color border = null;

            switch (kind) {
                case PRIMARY:
                    fill = pressed ? Palette.ACCENT_DEEP : (hover && enabled ? Palette.ACCENT_HOVER : Palette.ACCENT);
                    border = Palette.ACCENT_DEEP;
                    text = Palette.ON_ACCENT;
                    break;
                case SECONDARY:
                    fill = hover && enabled ? Palette.SURFACE_ALT : Palette.SURFACE;
                    border = hover && enabled ? Palette.ACCENT : Palette.LINE;
                    text = Palette.FG;
                    break;
                case DANGER:
                    fill = hover && enabled ? Palette.mix(Palette.SURFACE, Palette.ERR, 0.16f)
                            : Palette.mix(Palette.SURFACE, Palette.ERR, 0.08f);
                    border = Palette.alpha(Palette.ERR, 180);
                    text = Palette.ERR;
                    break;
                case NAV:
                    if (isSelected()) {
                        fill = Palette.SURFACE_ALT;
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
                    border = hover && enabled ? Palette.LINE : null;
                    text = hover && enabled ? Palette.FG : Palette.MUTED;
                    break;
            }

            if (!enabled) {
                text = Palette.DIM;
                if (fill != null) fill = Palette.SURFACE_ALT;
                border = Palette.LINE;
            }

            if (shadow > 0) Palette.hardShadow(g, 0, 0, cw, ch, shadow);
            if (fill != null) Palette.fillRound(g, 0, 0, cw, ch, 0, fill);
            if (border != null) {
                g.setStroke(new BasicStroke(2f));
                Palette.strokeRound(g, 1, 1, cw - 2, ch - 2, 0, border);
            }
            if (isFocusOwner()) {
                g.setStroke(new BasicStroke(2f));
                Palette.strokeRound(g, 3, 3, cw - 6, ch - 6, 0, Palette.alpha(Palette.ACCENT, 190));
            }

            g.setFont(getFont());
            g.setColor(text);
            java.awt.FontMetrics metrics = g.getFontMetrics();
            int x = (cw - metrics.stringWidth(getText())) / 2;
            int y = (ch - metrics.getHeight()) / 2 + metrics.getAscent();
            g.drawString(getText(), Math.max(6, x), y);
            g.dispose();
        }
    }

    /**
     * Кнопка для тёмной hero-сцены: срезанные углы, пиксельная тень и
     * опциональная иконка Telegram.
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
            java.awt.Shape shape = pixelShape(w - 3, h - 3, 4);

            Color textColor;
            if (tone == Tone.PRIMARY) {
                g.setColor(Palette.SHADOW);
                g.fill(pixelShape(w - 3, h - 3, 4));
                g.setColor(pressed ? Palette.ACCENT_DEEP : (hover && enabled ? Palette.ACCENT_HOVER : Palette.ACCENT));
                if (!enabled) g.setColor(Palette.mix(Palette.ACCENT, Palette.SHADOW, 0.6f));
                g.fill(shape);
                g.setColor(Palette.ACCENT_DEEP);
                g.draw(shape);
                textColor = enabled ? Palette.ON_ACCENT : Palette.alpha(Palette.ON_ACCENT, 130);
            } else if (tone == Tone.SECONDARY) {
                g.setColor(Palette.SHADOW);
                g.fill(pixelShape(w - 3, h - 3, 4));
                g.setColor(hover && enabled ? Palette.SURFACE_ALT : Palette.SURFACE);
                g.fill(shape);
                g.setColor(hover && enabled ? Palette.ACCENT : Palette.LINE);
                g.draw(shape);
                textColor = enabled ? Palette.FG : Palette.DIM;
            } else {
                if (hover && enabled) {
                    g.setColor(Palette.SURFACE_ALT);
                    g.fill(shape);
                }
                g.setColor(hover && enabled ? Palette.ACCENT : Palette.alpha(Palette.FG, 60));
                g.draw(shape);
                textColor = enabled ? Palette.MUTED : Palette.DIM;
            }

            g.setFont(getFont());
            java.awt.FontMetrics metrics = g.getFontMetrics();
            int icon = telegram ? 18 : 0;
            int total = metrics.stringWidth(getText()) + icon;
            int x = Math.max(10, (w - total) / 2);
            int y = (h - metrics.getHeight()) / 2 + metrics.getAscent();
            if (telegram) {
                g.setColor(textColor);
                drawTelegram(g, x, h / 2 - 6, 2);
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

            int trackW = 46;
            int trackH = 24;
            int tx = w - trackW - 2;
            int ty = hint == null || hint.isEmpty() ? (h - trackH) / 2 : 6;

            Color track = selected ? Palette.ACCENT : (hover ? Palette.HOVER
                    : Palette.mix(Palette.SURFACE_ALT, Palette.LINE, 0.6f));
            if (!isEnabled()) track = Palette.mix(Palette.SURFACE_ALT, Palette.LINE, 0.6f);
            Palette.fillRound(g, tx, ty, trackW, trackH, 0, track);
            g.setStroke(new BasicStroke(2f));
            Palette.strokeRound(g, tx + 1, ty + 1, trackW - 2, trackH - 2, 0,
                    selected ? Palette.ACCENT_DEEP : Palette.LINE);

            int knob = 16;
            int kx = selected ? tx + trackW - knob - 4 : tx + 4;
            int ky = ty + 4;
            g.setColor(Palette.SHADOW);
            g.fillRect(kx + 2, ky + 2, knob, knob);
            g.setColor(selected ? Palette.FG : Palette.MUTED);
            g.fillRect(kx, ky, knob, knob);
            g.setColor(selected ? Palette.ACCENT_DEEP : Palette.LINE);
            g.drawRect(kx, ky, knob, knob);

            if (focus) {
                g.setStroke(new BasicStroke(2f));
                Palette.strokeRound(g, 0, 0, w - 1, h - 1, 0, Palette.alpha(Palette.ACCENT, 150));
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
            setPreferredSize(new Dimension(300, 74));
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
            if (w <= 4 || h <= 4) {
                g.dispose();
                return;
            }
            int cw = w - 4;
            int ch = h - 4;
            Palette.hardShadow(g, 0, 0, cw, ch, 4);
            Palette.fillRound(g, 0, 0, cw, ch, 0,
                    hover ? Palette.mix(Palette.SURFACE, Palette.ACCENT, 0.05f) : Palette.SURFACE);
            g.setStroke(new BasicStroke(2f));
            Palette.strokeRound(g, 1, 1, cw - 2, ch - 2, 0,
                    hover || focus ? Palette.ACCENT : Palette.LINE);

            Color tone = glyph == Glyph.TELEGRAM ? Palette.TELEGRAM : Palette.ACCENT;
            int badge = 42;
            int bx = 14;
            int by = (ch - badge) / 2;
            Palette.fillRound(g, bx, by, badge, badge, 0,
                    Palette.mix(Palette.SURFACE, tone, 0.14f));
            g.setStroke(new BasicStroke(2f));
            Palette.strokeRound(g, bx + 1, by + 1, badge - 2, badge - 2, 0, Palette.alpha(tone, 170));

            g.setColor(tone);
            if (glyph == Glyph.TELEGRAM) {
                drawTelegram(g, bx + 10, by + 13, 2);
            } else {
                drawGlobe(g, bx + 12, by + 12, 2);
            }

            int textX = bx + badge + 14;
            int textW = Math.max(60, cw - textX - 26);
            g.setFont(Palette.semi(10.5f));
            g.setColor(Palette.FG);
            g.drawString(clip(caption, g, textW), textX, ch / 2 - 2);
            g.setFont(Palette.reg(9f));
            g.setColor(Palette.MUTED);
            g.drawString(clip(detail, g, textW), textX, ch / 2 + 16);

            int ax = cw - 22;
            int ay = ch / 2;
            g.setColor(hover || focus ? Palette.ACCENT : Palette.DIM);
            for (int i = 0; i < 3; i++) {
                g.fillRect(ax + i * 3, ay - 4 + i * 3, 3, 3);
            }
            g.dispose();
        }
    }

    private static void drawGlobe(Graphics2D g, int x, int y, int cell) {
        String[] rows = {
                ".#####.",
                "#..#..#",
                "#.###.#",
                "##...##",
                "#.###.#",
                "#..#..#",
                ".#####."
        };
        pixelArt(g, x, y, cell, rows, new Color[]{g.getColor()}, new char[]{'#'});
    }

    private static void drawTelegram(Graphics2D g, int x, int y, int cell) {
        String[] rows = {
                ".......#",
                "......##",
                ".....##.",
                "....##..",
                ".#.##...",
                "####....",
                ".##.....",
                "#......."
        };
        pixelArt(g, x, y, cell, rows, new Color[]{g.getColor()}, new char[]{'#'});
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
                g.fillRect(0, 0, size, size);
                g.setColor(Palette.ON_ACCENT);
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
            Palette.fillRound(g, 0, 0, w, h, 0, Palette.SURFACE_ALT);
            g.setStroke(new BasicStroke(2f));
            Palette.strokeRound(g, 1, 1, w - 3, h - 3, 0, Palette.LINE);
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
            int trackY = h / 2 - 5;
            int trackW = Math.max(1, w - pad * 2);
            Palette.fillRound(g, pad, trackY, trackW, 10, 0, Palette.SURFACE_ALT);
            g.setStroke(new BasicStroke(2f));
            Palette.strokeRound(g, pad + 1, trackY + 1, trackW - 2, 8, 0, Palette.LINE);
            int filled = (int) (trackW * (value - min) / (double) (max - min));
            if (filled > 0) {
                Palette.fillRound(g, pad + 2, trackY + 2, Math.max(4, filled - 2), 6, 0, Palette.ACCENT);
            }

            int step = Math.max(1, (max - min) / 16);
            g.setColor(Palette.alpha(Palette.FG, 16));
            for (int v = min; v <= max; v += step) {
                int x = pad + (int) (trackW * (v - min) / (double) (max - min));
                g.fillRect(x, trackY + 12, 1, 3);
            }

            int knob = 14;
            int kx = pad + filled - knob / 2;
            int ky = trackY - 2;
            g.setColor(Palette.SHADOW);
            g.fillRect(kx + 2, ky + 2, knob, knob);
            g.setColor(Palette.FG);
            g.fillRect(kx, ky, knob, knob);
            g.setColor(Palette.ACCENT);
            g.drawRect(kx, ky, knob, knob);
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

    /** Точка статуса сервера — квадратный пиксель. */
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
            int size = Math.min(8, Math.min(getWidth(), getHeight()));
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            g.setColor(Palette.alpha(color, 60));
            g.drawRect(x - 2, y - 2, size + 3, size + 3);
            g.setColor(color);
            g.fillRect(x, y, size, size);
            g.dispose();
        }
    }

}
