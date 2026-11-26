package ru.berrytrip.berryup.ui;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 3D-просмотр скина и плаща целиком на Java2D.
 *
 * <p>Модель собирается из тех же PNG/GIF, что лежат в пуле QuickSkin: голова,
 * тело, руки, ноги и оверлеи по стандартной раскладке 64×64 (и HD), плюс плащ
 * 64×32, пристёгнутый к плечам и мягко покачивающийся. Каждая грань блока при
 * ортографической проекции остаётся параллелограммом, поэтому текстура ложится
 * точным аффинным преобразованием без искажений.</p>
 */
public class SkinView extends JComponent {

    /** Блок модели: размеры в «пикселях скина», центр и UV-угол текстуры. */
    private static final class Box {
        final float w, h, d, x, y;
        final int u, v, ou, ov;

        Box(float w, float h, float d, float x, float y, int u, int v, int ou, int ov) {
            this.w = w;
            this.h = h;
            this.d = d;
            this.x = x;
            this.y = y;
            this.u = u;
            this.v = v;
            this.ou = ou;
            this.ov = ov;
        }
    }

    /** Голова, тело, руки и ноги — стандартная раскладка скина 64×64. */
    private static final Box[] BOXES = new Box[]{
            new Box(8, 8, 8, 0, 28, 0, 0, 32, 0),
            new Box(8, 12, 4, 0, 18, 16, 16, 16, 32),
            new Box(4, 12, 4, -6, 18, 40, 16, 40, 32),
            new Box(4, 12, 4, 6, 18, 32, 48, 48, 48),
            new Box(4, 12, 4, -1.9f, 6, 0, 16, 0, 32),
            new Box(4, 12, 4, 1.9f, 6, 16, 48, 0, 48)
    };

    private static final float MODEL_HEIGHT = 32f;
    private static final float LIGHT_X = -0.42f;
    private static final float LIGHT_Y = 0.82f;
    private static final float LIGHT_Z = 0.56f;

    private BufferedImage skin;
    private BufferedImage cape;
    private float texel = 1f;
    private float capeTexel = 1f;
    // как в QuickSkin: спокойный разворот влево-вперёд, без автовращения
    private double yaw = 20;
    private double pitch = 0;
    private long lastDrag;
    private int dragX;
    private int dragY;
    private boolean dragging;
    private boolean dropActive;
    private boolean slim;

    public SkinView() {
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setSkin(defaultSkin());
        setToolTipText("Потяни мышью — модель повернётся");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragging = true;
                dragX = e.getX();
                dragY = e.getY();
                lastDrag = System.currentTimeMillis();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = false;
                lastDrag = System.currentTimeMillis();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                yaw += (e.getX() - dragX) * 0.7;
                // вертикаль инвертирована: тянем вверх — смотрим снизу
                pitch = Math.max(-24, Math.min(24, pitch - (e.getY() - dragY) * 0.35));
                dragX = e.getX();
                dragY = e.getY();
                lastDrag = System.currentTimeMillis();
                repaint();
            }
        });

        // медленное вращение, как в QuickSkin; после драга модель замирает на пару секунд
        new Timer(40, event -> {
            if (!isShowing() || dragging) return;
            if (System.currentTimeMillis() - lastDrag < 1600) {
                if (cape != null) repaint();
                return;
            }
            yaw += 0.16;
            repaint();
        }).start();
    }

    /** Скин 64×64, 64×32 (старый формат) или HD 128…2048. */
    public void setSkin(BufferedImage image) {
        if (image == null) {
            skin = defaultSkin();
            // дефолтный скин тоже может быть HD: 128×128 — это texel 2
            texel = Math.max(1f, skin.getWidth() / 64f);
        } else {
            BufferedImage fixed = image;
            boolean legacy = image.getWidth() == 64 && image.getHeight() == 32;
            if (image.getHeight() < 64) {
                fixed = new BufferedImage(image.getWidth(), 64, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = fixed.createGraphics();
                g.setComposite(AlphaComposite.Src);
                g.drawImage(image, 0, 0, null);
                g.dispose();
            }
            if (legacy) {
                // 64x32: у старого формата нет левых конечностей — Java
                // отображает их зеркалом правых. Делаем то же для превью.
                mirrorInto(fixed, 40, 16, 16, 16, 32, 48);
                mirrorInto(fixed, 0, 16, 16, 16, 16, 48);
            }
            skin = fixed;
            texel = Math.max(1f, fixed.getWidth() / 64f);
        }
        repaint();
    }

    /** Плащ 64×32 … 2048×1024; null — плаща нет. */
    public void setCape(BufferedImage image) {
        if (image == null || image.getWidth() < 64 || image.getHeight() < 32) {
            cape = null;
        } else {
            cape = image;
            capeTexel = Math.max(1f, image.getWidth() / 64f);
        }
        repaint();
    }

    public BufferedImage skin() {
        return skin;
    }

    /** Classic (4 px) или Slim (3 px) руки. Меняет только геометрию, не текстуру. */
    public void setSlim(boolean value) {
        if (slim == value) return;
        slim = value;
        repaint();
    }

    public boolean isSlim() {
        return slim;
    }

    /** Определение slim по тем же 24 пикселям, что использует игра. */
    public static boolean detectSlim(BufferedImage image) {
        if (image == null || image.getWidth() < 64 || image.getHeight() < 64) return false;
        float scale = image.getWidth() / 64f;
        int opaque = 0;
        for (int y = 20; y < 32; y++) {
            for (int x = 46; x < 48; x++) {
                int px = Math.min(image.getWidth() - 1, Math.round(x * scale));
                int py = Math.min(image.getHeight() - 1, Math.round(y * scale));
                if ((image.getRGB(px, py) >>> 24) != 0) opaque++;
            }
        }
        return opaque < 6;
    }

    private static void mirrorInto(BufferedImage image, int sx, int sy, int w, int h, int dx, int dy) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setRGB(dx + x, dy + y, image.getRGB(sx + (w - 1 - x), sy + y));
            }
        }
    }

    public BufferedImage cape() {
        return cape;
    }

    /** Подсветка сцены, пока над карточкой держат файл. */
    public void setDropActive(boolean active) {
        if (dropActive == active) return;
        dropActive = active;
        repaint();
    }

    /** Угол обзора: пригодится для превью и проверки отрисовки. */
    public void setAngles(double yawDegrees, double pitchDegrees) {
        yaw = yawDegrees;
        pitch = Math.max(-24, Math.min(24, pitchDegrees));
        lastDrag = System.currentTimeMillis();
        repaint();
    }

    private static BufferedImage steve;
    private static boolean steveLoaded;

    /** Скин Стива из ресурсов лаунчера (128×128, стандартная развёртка). */
    public static BufferedImage defaultSkin() {
        if (!steveLoaded) {
            steveLoaded = true;
            try (InputStream stream = SkinView.class.getResourceAsStream("/steve.png")) {
                if (stream != null) steve = ImageIO.read(stream);
            } catch (Exception ignored) {
            }
        }
        if (steve != null) return steve;
        return placeholderSkin();
    }

    /** Запасной силуэт в ягодных цветах, если ресурс скина не нашёлся. */
    private static BufferedImage placeholderSkin() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(0xF1C8A4));
        g.fillRect(0, 0, 32, 16);          // голова
        g.setColor(new Color(0xFF5C86));
        g.fillRect(16, 16, 24, 16);        // тело целиком, включая торцы
        g.fillRect(40, 16, 16, 16);        // правая рука
        g.fillRect(32, 48, 16, 16);        // левая рука
        g.setColor(new Color(0x2B2F3D));
        g.fillRect(0, 16, 16, 16);         // правая нога
        g.fillRect(16, 48, 16, 16);        // левая нога
        g.setColor(new Color(0x1A1D26));
        g.fillRect(10, 11, 2, 2);          // глаза
        g.fillRect(14, 11, 2, 2);
        g.fillRect(11, 14, 2, 1);
        g.dispose();
        return image;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = Palette.quality(graphics);
        int width = getWidth();
        int height = getHeight();
        if (width <= 4 || height <= 4) {
            g.dispose();
            return;
        }
        if (skin == null) skin = defaultSkin();

        // Модель без сцены и фона: карточка лаунчера видна сквозь превью.
        java.awt.Shape oldClip = g.getClip();
        g.clip(Palette.round(0, 0, width, height, 0));

        float scale = (float) Math.min(width / 25.0, height / 39.0);
        int originX = width / 2;
        int originY = Math.round(height * 0.47f);

        List<Face> faces = new ArrayList<Face>(BOXES.length * 14);
        buildBody(faces, false);
        buildBody(faces, true);
        buildCape(faces);

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        double radYaw = Math.toRadians(yaw);
        double radPitch = Math.toRadians(-pitch);
        double cy = Math.cos(radYaw);
        double sy = Math.sin(radYaw);
        double cp = Math.cos(radPitch);
        double sp = Math.sin(radPitch);

        for (Face face : faces) {
            double[] xs = new double[4];
            double[] ys = new double[4];
            double depth = 0;
            for (int i = 0; i < 4; i++) {
                double px = face.p[i][0];
                double py = face.p[i][1] - MODEL_HEIGHT / 2.0;
                double pz = face.p[i][2];
                double x1 = px * cy + pz * sy;
                double z1 = -px * sy + pz * cy;
                double y1 = py * cp - z1 * sp;
                double z2 = py * sp + z1 * cp;
                xs[i] = originX + x1 * scale;
                ys[i] = originY - y1 * scale;
                depth += z2;
            }
            face.depth = depth / 4.0;
            face.sx = xs;
            face.sy = ys;

            // нормаль после поворота: грань видна, если смотрит на зрителя
            double nx1 = face.nx * cy + face.nz * sy;
            double nz1 = -face.nx * sy + face.nz * cy;
            double ny1 = face.ny * cp - nz1 * sp;
            double nz2 = face.ny * sp + nz1 * cp;
            face.visible = nz2 > 0.02;
            face.shade = 0.68 + 0.36 * Math.max(0,
                    nx1 * LIGHT_X + ny1 * LIGHT_Y + nz2 * LIGHT_Z);
        }

        // ближние грани поверх дальних
        // painter's algorithm: сначала дальние грани, ближние рисуются поверх
        Collections.sort(faces, new Comparator<Face>() {
            @Override
            public int compare(Face a, Face b) {
                return Double.compare(a.depth, b.depth);
            }
        });

        for (Face face : faces) {
            if (!face.visible) continue;
            int u = Math.round(face.u * face.texel);
            int v = Math.round(face.v * face.texel);
            int tw = Math.round(face.w * face.texel);
            int th = Math.round(face.h * face.texel);
            if (u < 0 || v < 0 || tw <= 0 || th <= 0) continue;
            if (u + tw > face.image.getWidth() || v + th > face.image.getHeight()) continue;

            BufferedImage tile = face.image.getSubimage(u, v, tw, th);
            // Тень накладываем только на непрозрачные пиксели. Иначе
            // прозрачные места второго слоя превращаются в серые плиты.
            tile = shaded(tile, Math.max(0.0, Math.min(0.42, 1.0 - face.shade)));
            double x0 = face.sx[0];
            double y0 = face.sy[0];
            double x1 = face.sx[1];
            double y1 = face.sy[1];
            double x2 = face.sx[2];
            double y2 = face.sy[2];
            AffineTransform at = new AffineTransform(
                    (x1 - x0) / tw, (y1 - y0) / tw,
                    (x2 - x0) / th, (y2 - y0) / th,
                    x0, y0);
            g.drawImage(tile, at, null);
        }

        if (dropActive) {
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    1f, new float[]{6f, 6f}, 0f));
            g.setColor(Palette.ACCENT);
            g.drawRect(6, 6, width - 13, height - 13);
            g.setFont(Palette.semi(11.5f));
            String title = "Отпусти файл";
            java.awt.FontMetrics fm = g.getFontMetrics();
            int titleX = (width - fm.stringWidth(title)) / 2;
            g.setColor(new Color(20, 23, 30, 230));
            g.fillRect(titleX - 10, height - 48, fm.stringWidth(title) + 20, 24);
            g.setColor(Palette.ACCENT);
            g.drawRect(titleX - 10, height - 48, fm.stringWidth(title) + 20, 24);
            g.drawString(title, titleX, height - 32);
            g.setFont(Palette.reg(9f));
            g.setColor(Palette.MUTED);
            String hint = "скин или плащ · PNG / GIF";
            g.drawString(hint, (width - g.getFontMetrics().stringWidth(hint)) / 2, height - 14);
        }

        g.setClip(oldClip);
        g.dispose();
    }

    private void buildBody(List<Face> faces, boolean overlay) {
        for (Box box : BOXES) {
            // У головы второй слой +0.5 со всех сторон, у тела и конечностей +0.25.
            float inflate = overlay ? (box == BOXES[0] ? 0.5f : 0.25f) : 0f;
            boolean arm = box == BOXES[2] || box == BOXES[3];
            float baseW = arm && slim ? 3f : box.w;
            double baseX = arm && slim ? (box.x < 0 ? -5.5 : 5.5) : box.x;
            // UV берём по исходным размерам блока, а раздуваем только
            // геометрию второго слоя. Иначе UV сдвигается на величину inflate.
            float uw = baseW;
            float uh = box.h;
            float ud = box.d;
            float w = uw + inflate * 2;
            float h = uh + inflate * 2;
            float d = ud + inflate * 2;
            double x0 = baseX - w / 2.0, x1 = baseX + w / 2.0;
            double y0 = box.y - h / 2.0, y1 = box.y + h / 2.0;
            double z0 = -d / 2.0, z1 = d / 2.0;
            float u = overlay ? box.ou : box.u;
            float v = overlay ? box.ov : box.v;

            add(faces, skin, texel, u + ud, v + ud, uw, uh, 0, 0, 1,
                    x0, y1, z1, x1, y1, z1, x0, y0, z1, x1, y0, z1);
            add(faces, skin, texel, u + 2 * ud + uw, v + ud, uw, uh, 0, 0, -1,
                    x1, y1, z0, x0, y1, z0, x1, y0, z0, x0, y0, z0);
            // левая сторона текстуры = передняя грань (стандарт Minecraft)
            add(faces, skin, texel, u, v + ud, ud, uh, -1, 0, 0,
                    x0, y1, z0, x0, y1, z1, x0, y0, z0, x0, y0, z1);
            add(faces, skin, texel, u + ud + uw, v + ud, ud, uh, 1, 0, 0,
                    x1, y1, z1, x1, y1, z0, x1, y0, z1, x1, y0, z0);
            add(faces, skin, texel, u + ud, v, uw, ud, 0, 1, 0,
                    x0, y1, z0, x1, y1, z0, x0, y1, z1, x1, y1, z1);
            // низ: верх текстуры — передняя кромка, низ — задняя
            add(faces, skin, texel, u + ud + uw, v, uw, ud, 0, -1, 0,
                    x0, y0, z1, x1, y0, z1, x0, y0, z0, x1, y0, z0);
        }
    }

    /**
     * Плащ: тонкая коробка 10×16×1 за спиной, подвешенная к линии плеч.
     * Лёгкое покачивание — небольшой наклон всей плоскости назад.
     */
    private void buildCape(List<Face> faces) {
        if (cape == null) return;
        double sway = Math.toRadians(3.5 + Math.sin(System.currentTimeMillis() / 720.0) * 3.0);
        double top = 24.0;
        // строго за спиной: полотно не пересекается с телом, иначе сортировка граней врёт
        double z = -3.0;

        double x0 = -5, x1 = 5;
        double y0 = 8, y1 = 24;
        double zOuter = z - 0.4, zInner = z + 0.4;
        double drop = Math.tan(sway) * (top - y0);
        double bOuter = zOuter - drop;
        double bInner = zInner - drop;

        // наружная сторона плаща (видна со спины)
        add(faces, cape, capeTexel, 1, 1, 10, 16, 0, 0, -1,
                x1, y1, zOuter, x0, y1, zOuter, x1, y0, bOuter, x0, y0, bOuter);
        // внутренняя сторона (к телу)
        add(faces, cape, capeTexel, 12, 1, 10, 16, 0, 0, 1,
                x0, y1, zInner, x1, y1, zInner, x0, y0, bInner, x1, y0, bInner);
        // боковины и торцы — тонкие полоски текстуры плаща
        add(faces, cape, capeTexel, 0, 1, 1, 16, -1, 0, 0,
                x0, y1, zOuter, x0, y1, zInner, x0, y0, bOuter, x0, y0, bInner);
        add(faces, cape, capeTexel, 11, 1, 1, 16, 1, 0, 0,
                x1, y1, zInner, x1, y1, zOuter, x1, y0, bInner, x1, y0, bOuter);
        // верхний торец у плеч
        add(faces, cape, capeTexel, 1, 0, 10, 1, 0, 1, 0,
                x0, y1, zInner, x1, y1, zInner, x0, y1, zOuter, x1, y1, zOuter);
        // нижний край
        add(faces, cape, capeTexel, 11, 0, 10, 1, 0, -1, 0,
                x1, y0, bOuter, x0, y0, bOuter, x1, y0, bInner, x0, y0, bInner);
    }

    private static BufferedImage shaded(BufferedImage source, double amount) {
        if (amount <= 0.001) return source;
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = source.getRGB(x, y);
                int alpha = argb >>> 24;
                if (alpha == 0) continue;
                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = argb & 0xFF;
                red += (20 - red) * amount;
                green += (23 - green) * amount;
                blue += (38 - blue) * amount;
                result.setRGB(x, y, (alpha << 24)
                        | (Math.max(0, Math.min(255, (int) Math.round(red))) << 16)
                        | (Math.max(0, Math.min(255, (int) Math.round(green))) << 8)
                        | Math.max(0, Math.min(255, (int) Math.round(blue))));
            }
        }
        return result;
    }

    private static void add(List<Face> faces, BufferedImage image, float texel,
                            float u, float v, float w, float h,
                            float nx, float ny, float nz,
                            double ax, double ay, double az,
                            double bx, double by, double bz,
                            double cx, double cy, double cz,
                            double dx, double dy, double dz) {
        faces.add(new Face(image, texel, u, v, w, h, nx, ny, nz,
                new double[]{ax, ay, az}, new double[]{bx, by, bz},
                new double[]{cx, cy, cz}, new double[]{dx, dy, dz}));
    }

    private static int round(double value) {
        return (int) Math.round(value);
    }

    /** Одна грань: четыре угла (ЛВ, ПВ, ЛН, ПН), UV-прямоугольник и нормаль. */
    private static final class Face {
        final BufferedImage image;
        final float texel;
        final float u, v, w, h;
        final float nx, ny, nz;
        final double[][] p;
        double depth;
        double shade = 1;
        boolean visible;
        double[] sx;
        double[] sy;

        Face(BufferedImage image, float texel, float u, float v, float w, float h,
             float nx, float ny, float nz,
             double[] a, double[] b, double[] c, double[] d) {
            this.image = image;
            this.texel = texel;
            this.u = u;
            this.v = v;
            this.w = w;
            this.h = h;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.p = new double[][]{a, b, c, d};
        }
    }
}
