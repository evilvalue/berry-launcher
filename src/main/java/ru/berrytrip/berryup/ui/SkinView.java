package ru.berrytrip.berryup.ui;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.AlphaComposite;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 3D-просмотр скина в духе QuickSkin, но целиком на Java2D: модель игрока
 * собирается из той же PNG 64×64 (или HD 128×128), что лежит в моде.
 *
 * Каждая грань блока при ортографической проекции остаётся параллелограммом,
 * поэтому текстура ложится точным аффинным преобразованием без искажений.
 * Грани отбрасываются по нормали и рисуются от дальних к ближним, сверху —
 * мягкая тень-подставка и подсветка граней, чтобы модель читалась объёмно.
 */
public class SkinView extends JComponent {

    /** Блок модели: размеры в «пикселях скина», центр и UV-угол текстуры. */
    private static final class Box {
        final float w, h, d, x, y, z;
        final int u, v, ou, ov;

        Box(float w, float h, float d, float x, float y, int u, int v, int ou, int ov) {
            this.w = w;
            this.h = h;
            this.d = d;
            this.x = x;
            this.y = y;
            this.z = 0;
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
            new Box(4, 12, 4, -2, 6, 0, 16, 0, 32),
            new Box(4, 12, 4, 2, 6, 16, 48, 0, 48)
    };

    private static final float MODEL_HEIGHT = 32f;
    private static final float LIGHT_X = -0.42f;
    private static final float LIGHT_Y = 0.82f;
    private static final float LIGHT_Z = 0.56f;

    private BufferedImage skin;
    private float texel = 1f;
    private double yaw = -28;
    private double pitch = -8;
    private long lastDrag;
    private int dragX;
    private int dragY;
    private boolean dragging;

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
                pitch = Math.max(-24, Math.min(24, pitch + (e.getY() - dragY) * 0.35));
                dragX = e.getX();
                dragY = e.getY();
                lastDrag = System.currentTimeMillis();
                repaint();
            }
        });

        // лёгкое вращение, пока модель не трогают
        new Timer(40, event -> {
            if (!isShowing() || dragging) return;
            if (System.currentTimeMillis() - lastDrag < 1800) return;
            yaw += 0.5;
            repaint();
        }).start();
    }

    /** Скин 64×64, 64×32 (старый формат) или HD 128×128. */
    public void setSkin(BufferedImage image) {
        if (image == null) {
            skin = defaultSkin();
            texel = 1f;
        } else {
            BufferedImage fixed = image;
            if (image.getHeight() < 64) {
                fixed = new BufferedImage(image.getWidth(), 64, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = fixed.createGraphics();
                g.setComposite(AlphaComposite.Src);
                g.drawImage(image, 0, 0, null);
                g.dispose();
            }
            skin = fixed;
            texel = Math.max(1f, fixed.getWidth() / 64f);
        }
        repaint();
    }

    public BufferedImage skin() {
        return skin;
    }

    /** Угол обзора: пригодится для превью и проверки отрисовки. */
    public void setAngles(double yawDegrees, double pitchDegrees) {
        yaw = yawDegrees;
        pitch = Math.max(-24, Math.min(24, pitchDegrees));
        lastDrag = System.currentTimeMillis();
        repaint();
    }

    /** Силуэт в ягодных цветах — пока свой скин не выбран. */
    public static BufferedImage defaultSkin() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(0xF1C8A4));
        g.fillRect(0, 0, 32, 16);          // голова
        g.setColor(new Color(0xFF5C86));
        g.fillRect(16, 16, 24, 16);        // тело и правая рука
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
        if (width <= 4 || height <= 4 || skin == null) {
            g.dispose();
            return;
        }

        // подставка и мягкое свечение
        float scale = (float) Math.min(width / 24.0, height / 40.0);
        int originX = width / 2;
        int originY = height / 2;
        int feetY = Math.round(originY + (MODEL_HEIGHT / 2f) * scale);
        int shadowW = Math.max(12, Math.round(15 * scale));
        int shadowH = Math.max(4, Math.round(4 * scale));
        g.setPaint(new RadialGradientPaint(
                new Point2D.Float(originX, feetY),
                Math.max(8, shadowW * 0.8f),
                new float[]{0f, 1f},
                new Color[]{Palette.alpha(Palette.ACCENT, 42), Palette.alpha(Palette.ACCENT, 0)}));
        g.fill(new Ellipse2D.Float(originX - shadowW, feetY - shadowH * 1.6f,
                shadowW * 2f, shadowH * 3.2f));
        g.setPaint(new RadialGradientPaint(
                new Point2D.Float(originX, feetY),
                Math.max(6, shadowW * 0.6f),
                new float[]{0f, 1f},
                new Color[]{new Color(0, 0, 0, 70), new Color(0, 0, 0, 0)}));
        g.fill(new Ellipse2D.Float(originX - shadowW * 0.62f, feetY - shadowH * 0.8f,
                shadowW * 1.24f, shadowH * 1.6f));

        List<Face> faces = new ArrayList<Face>(BOXES.length * 12);
        build(faces, false);
        build(faces, true);

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        double radYaw = Math.toRadians(yaw);
        // отрицательный наклон камеры = смотрим сверху, как в QuickSkin
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
            face.shade = 0.62 + 0.38 * Math.max(0,
                    nx1 * LIGHT_X + ny1 * LIGHT_Y + nz2 * LIGHT_Z);
        }

        // ближние грани поверх дальних
        Collections.sort(faces, new Comparator<Face>() {
            @Override
            public int compare(Face a, Face b) {
                return Double.compare(b.depth, a.depth);
            }
        });

        for (Face face : faces) {
            if (!face.visible) continue;
            int u = Math.round(face.u * texel);
            int v = Math.round(face.v * texel);
            int tw = Math.round(face.w * texel);
            int th = Math.round(face.h * texel);
            if (u < 0 || v < 0 || tw <= 0 || th <= 0) continue;
            if (u + tw > skin.getWidth() || v + th > skin.getHeight()) continue;

            BufferedImage tile = skin.getSubimage(u, v, tw, th);
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

            int shade = (int) Math.round((1.0 - face.shade) * 150);
            if (shade > 3) {
                Polygon quad = new Polygon(
                        new int[]{round(face.sx[0]), round(face.sx[1]), round(face.sx[3]), round(face.sx[2])},
                        new int[]{round(face.sy[0]), round(face.sy[1]), round(face.sy[3]), round(face.sy[2])},
                        4);
                g.setClip(quad);
                g.setColor(new Color(0, 0, 0, Math.min(150, shade)));
                g.fill(quad);
                g.setClip(null);
            }
        }

        g.dispose();
    }

    private void build(List<Face> faces, boolean overlay) {
        float inflate = overlay ? 0.5f : 0f;
        for (Box box : BOXES) {
            if (overlay && isEmpty(box.ou, box.ov)) continue;
            float w = box.w + inflate * 2;
            float h = box.h + inflate * 2;
            float d = box.d + inflate * 2;
            double x0 = box.x - w / 2.0, x1 = box.x + w / 2.0;
            double y0 = box.y - h / 2.0, y1 = box.y + h / 2.0;
            double z0 = box.z - d / 2.0, z1 = box.z + d / 2.0;
            float u = overlay ? box.ou : box.u;
            float v = overlay ? box.ov : box.v;

            // перед (+z)
            add(faces, u + d, v + d, w, h, 0, 0, 1,
                    x0, y1, z1, x1, y1, z1, x0, y0, z1, x1, y0, z1);
            // спина (-z)
            add(faces, u + 2 * d + w, v + d, w, h, 0, 0, -1,
                    x1, y1, z0, x0, y1, z0, x1, y0, z0, x0, y0, z0);
            // левая сторона (-x)
            add(faces, u, v + d, d, h, -1, 0, 0,
                    x0, y1, z1, x0, y1, z0, x0, y0, z1, x0, y0, z0);
            // правая сторона (+x)
            add(faces, u + d + w, v + d, d, h, 1, 0, 0,
                    x1, y1, z0, x1, y1, z1, x1, y0, z0, x1, y0, z1);
            // верх (+y)
            add(faces, u + d, v, w, d, 0, 1, 0,
                    x0, y1, z0, x1, y1, z0, x0, y1, z1, x1, y1, z1);
            // низ (-y)
            add(faces, u + d + w, v, w, d, 0, -1, 0,
                    x0, y0, z1, x1, y0, z1, x0, y0, z0, x1, y0, z0);
        }
    }

    private boolean isEmpty(int u, int v) {
        int x = Math.round(u * texel);
        int y = Math.round(v * texel);
        int size = Math.round(16 * texel);
        if (x < 0 || y < 0 || x + size > skin.getWidth() || y + size > skin.getHeight()) return true;
        for (int py = y; py < y + size; py += Math.max(1, (int) texel)) {
            for (int px = x; px < x + size; px += Math.max(1, (int) texel)) {
                if ((skin.getRGB(px, py) >>> 24) != 0) return false;
            }
        }
        return true;
    }

    private static void add(List<Face> faces, float u, float v, float w, float h,
                            float nx, float ny, float nz,
                            double ax, double ay, double az,
                            double bx, double by, double bz,
                            double cx, double cy, double cz,
                            double dx, double dy, double dz) {
        faces.add(new Face(u, v, w, h, nx, ny, nz,
                new double[]{ax, ay, az}, new double[]{bx, by, bz},
                new double[]{cx, cy, cz}, new double[]{dx, dy, dz}));
    }

    private static int round(double value) {
        return (int) Math.round(value);
    }

    /** Одна грань: четыре угла (ЛВ, ПВ, ЛН, ПН), UV-прямоугольник и нормаль. */
    private static final class Face {
        final float u, v, w, h;
        final float nx, ny, nz;
        final double[][] p;
        double depth;
        double shade = 1;
        boolean visible;
        double[] sx;
        double[] sy;

        Face(float u, float v, float w, float h, float nx, float ny, float nz,
             double[] a, double[] b, double[] c, double[] d) {
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
