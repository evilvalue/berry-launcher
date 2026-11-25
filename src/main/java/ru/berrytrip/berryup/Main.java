package ru.berrytrip.berryup;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import ru.berrytrip.berryup.ui.MainFrame;
import ru.berrytrip.berryup.ui.Palette;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Точка входа BerryUp. */
public final class Main {

    private static java.nio.channels.FileLock lock;
    private static java.nio.channels.FileChannel lockChannel;

    private Main() {
    }

    /**
     * Один BerryUp на компьютере. Ждём пару секунд: при обновлении новая
     * версия стартует вторым процессом, пока старая ещё выходит.
     */
    private static boolean acquireLock() {
        try {
            lockChannel = java.nio.channels.FileChannel.open(
                    new File(System.getProperty("java.io.tmpdir", "."), "BerryUp.lock").toPath(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE);
            for (int attempt = 0; attempt < 12; attempt++) {
                try {
                    lock = lockChannel.tryLock();
                } catch (Exception busy) {
                    lock = null;
                }
                if (lock != null) return true;
                Thread.sleep(500);
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0].toLowerCase(java.util.Locale.ROOT) : "";

        if (mode.equals("--apply-update")) {
            if (args.length > 1) Updater.applyUpdate(args[1]);
            return;
        }
        if (mode.equals("--install")) {
            Store.load();
            java.io.File own = Updater.ownJar();
            java.io.File target = own == null ? null : Install.install(own);
            System.out.println(target == null ? "не удалось установить" : target.getAbsolutePath());
            System.exit(target == null ? 1 : 0);
            return;
        }
        if (mode.equals("--shortcut")) {
            Store.load();
            System.exit(Install.createShortcuts() ? 0 : 1);
            return;
        }
        if (mode.equals("--selftest")) {
            selftest();
            return;
        }
        if (mode.equals("--ping")) {
            String host = args.length > 1 ? args[1] : Cfg.SERVER_HOST;
            int port = Cfg.SERVER_PORT;
            if (args.length > 2) {
                try {
                    port = Integer.parseInt(args[2]);
                } catch (Exception ignored) {
                }
            }
            System.exit(ping(host, port));
            return;
        }
        if (mode.equals("--render")) {
            System.exit(render(args.length > 1 ? args[1] : ""));
            return;
        }
        if (mode.equals("--updatecheck")) {
            UpdateCheck.Info info = UpdateCheck.check();
            System.out.println("{ \"current\": " + q(Cfg.VERSION)
                    + ", \"available\": " + q(info == null ? null : info.version)
                    + ", \"newer\": " + UpdateCheck.isNewer(info)
                    + ", \"jar\": " + q(info == null ? null : info.jar)
                    + ", \"size\": " + (info == null ? 0 : info.size)
                    + ", \"sha256\": " + q(info == null ? null : info.sha256)
                    + ", \"staged\": " + Updater.pending()
                    + ", \"error\": " + q(UpdateCheck.lastError()) + " }");
            System.exit(info == null ? 1 : (UpdateCheck.isNewer(info) ? 0 : 2));
            return;
        }
        if (mode.equals("--help") || mode.equals("-h")) {
            System.out.println("BerryUp " + Cfg.VERSION);
            System.out.println("  BerryUp.jar              запустить лаунчер");
            System.out.println("  --install                установить в %LOCALAPPDATA%\\BerryUp и сделать ярлыки");
            System.out.println("  --shortcut               пересоздать ярлыки (рабочий стол и меню «Пуск»)");
            System.out.println("  --selftest               что найдено на этом компьютере (JSON)");
            System.out.println("  --ping [host] [port]     пинг сервера по протоколу Minecraft");
            System.out.println("  --render <папка>         отрисовать вкладки в PNG");
            System.out.println("  --updatecheck            сверить версию с latest-java.json");
            return;
        }

        Store.load();
        boolean dark = Store.getBool("theme.dark", false);
        Palette.applyTheme(dark);
        setupLook(dark);

        final boolean[] allowed = {true};
        try {
            SwingUtilities.invokeAndWait(() -> allowed[0] = acquireLock());
        } catch (Exception ignored) {
        }
        if (!allowed[0]) {
            javax.swing.JOptionPane.showMessageDialog(null,
                    "BerryUp уже запущен — окно открыто в панели задач.",
                    Cfg.APP_NAME, javax.swing.JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Первый запуск из «Загрузок»: переезжаем в %LOCALAPPDATA%\BerryUp,
        // делаем ярлыки и дальше работаем уже оттуда.
        Tweaks.load();
        if (Install.ensure()) return;
        if (Install.applyScale()) return;

        Updater.cleanup();

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }

    /** Что найдено: без окна и без сети, для проверки и для отчётов. */
    private static void selftest() {
        Store.load();
        Tweaks.load();
        Prism prism = Prism.find();
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"version\": ").append(q(Cfg.VERSION)).append(",\n");
        json.append("  \"java\": ").append(q(System.getProperty("java.version"))).append(",\n");
        json.append("  \"launcher\": ").append(q(prism == null ? null : prism.kind)).append(",\n");
        json.append("  \"exe\": ").append(q(prism == null ? null : prism.exe.getAbsolutePath())).append(",\n");
        json.append("  \"data_dir\": ").append(q(prism == null ? null : prism.dataDir.getAbsolutePath())).append(",\n");
        json.append("  \"instance_dir\": ").append(q(prism == null ? null : prism.instanceDir().getAbsolutePath())).append(",\n");
        json.append("  \"instance_installed\": ").append(prism != null && prism.hasInstance()).append(",\n");
        json.append("  \"instance_id\": ").append(q(prism == null ? null : prism.instanceId())).append(",\n");
        json.append("  \"mods\": ").append(prism == null ? 0 : prism.modCount()).append(",\n");
        json.append("  \"mods_off\": ").append(Tweaks.offCount(prism)).append(",\n");
        json.append("  \"bootstrap\": ").append(prism != null && prism.bootstrapJar().isFile()).append(",\n");
        java.io.File java = prism == null ? null : prism.findJava();
        json.append("  \"instance_java\": ").append(q(java == null ? null : java.getAbsolutePath())).append(",\n");
        json.append("  \"instance_java_version\": ").append(q(java == null ? null : Prism.javaVersion(java))).append(",\n");
        json.append("  \"ram_gb\": ").append(Ram.gb()).append(",\n");
        json.append("  \"memory_gb\": ").append(Tweaks.effectiveMemory()).append(",\n");
        json.append("  \"config_dir\": ").append(q(Cfg.configDir().getAbsolutePath())).append(",\n");
        json.append("  \"servers\": [");
        for (int i = 0; i < Cfg.SERVERS.length; i++) {
            if (i > 0) json.append(", ");
            json.append("{ \"name\": ").append(q(Cfg.SERVERS[i].name))
                    .append(", \"address\": ").append(q(Cfg.SERVERS[i].endpoint())).append(" }");
        }
        json.append("]\n}");
        System.out.println(json);
    }

    private static int ping(String host, int port) {
        Ping.Status status = Ping.query(host, port, 5000);
        System.out.println("{ \"ok\": " + status.ok
                + ", \"online\": " + status.online
                + ", \"max\": " + status.max
                + ", \"ping_ms\": " + status.ping
                + ", \"version\": " + q(status.version)
                + ", \"motd\": " + q(status.motd)
                + ", \"error\": " + q(status.error) + " }");
        return status.ok ? 0 : 4;
    }

    /** Общая настройка FlatLaf и скруглений для светлой и тёмной темы. */
    public static void setupLook(boolean dark) {
        System.setProperty("flatlaf.useNativeLibrary", "false");
        System.setProperty("flatlaf.useWindowDecorations", "false");
        if (dark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 0);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("ScrollBar.width", 8);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(1, 1, 1, 1));
        UIManager.put("ToolTip.background", dark ? new java.awt.Color(0x1E2229) : new java.awt.Color(0xFFFFFF));
        UIManager.put("ToolTip.foreground", dark ? new java.awt.Color(0xF1F3F8) : new java.awt.Color(0x15171F));
        UIManager.put("TextField.background", dark ? new java.awt.Color(0x262B34) : new java.awt.Color(0xF2F3F8));
        UIManager.put("TextField.foreground", dark ? new java.awt.Color(0xF1F3F8) : new java.awt.Color(0x15171F));
        UIManager.put("ComboBox.background", dark ? new java.awt.Color(0x262B34) : new java.awt.Color(0xFFFFFF));
        UIManager.put("ComboBox.foreground", dark ? new java.awt.Color(0xF1F3F8) : new java.awt.Color(0x15171F));
        UIManager.put("OptionPane.background", dark ? new java.awt.Color(0x1E2229) : new java.awt.Color(0xFFFFFF));
    }

    /** Рисует вкладки в PNG, как --render у exe-версии: удобно проверить вёрстку. */
    private static int render(String directory) throws Exception {
        if (directory == null || directory.isEmpty()) return 2;
        final File dir = new File(directory);
        if (!dir.isDirectory() && !dir.mkdirs()) return 2;
        System.setProperty("berryup.render", "1");
        Store.load();
        String theme = System.getProperty("berryup.render.theme", "");
        boolean dark = "dark".equals(theme)
                || (theme.isEmpty() && Store.getBool("theme.dark", false));
        Palette.applyTheme(dark);
        setupLook(dark);

        final MainFrame[] holder = new MainFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            MainFrame frame = new MainFrame();
            frame.setSize(Integer.getInteger("berryup.render.width", 1200),
                    Integer.getInteger("berryup.render.height", 800));
            frame.setLocation(-4000, -4000);
            frame.setVisible(true);
            holder[0] = frame;
        });
        Thread.sleep(600);

        String[] names = {"main", "account", "settings", "diag", "about"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            SwingUtilities.invokeAndWait(() -> {
                holder[0].setTab(index);
                holder[0].validate();
                holder[0].doLayout();
            });
            Thread.sleep(220);
            final File out = new File(dir, names[i] + ".png");
            SwingUtilities.invokeAndWait(() -> capture(holder[0], out));
        }
        SwingUtilities.invokeAndWait(() -> holder[0].dispose());
        System.out.println("rendered: " + dir.getAbsolutePath());
        return 0;
    }

    private static void capture(JFrame frame, File file) {
        int width = Math.max(1, frame.getWidth());
        int height = Math.max(1, frame.getHeight());
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        frame.paint(g);
        g.dispose();
        try {
            ImageIO.write(image, "png", file);
        } catch (Exception error) {
            System.err.println("capture failed: " + error.getMessage());
        }
    }

    private static String q(String value) {
        if (value == null) return "null";
        StringBuilder text = new StringBuilder("\"");
        for (char symbol : value.toCharArray()) {
            switch (symbol) {
                case '"':
                    text.append("\\\"");
                    break;
                case '\\':
                    text.append("\\\\");
                    break;
                case '\n':
                    text.append("\\n");
                    break;
                case '\r':
                    text.append("\\r");
                    break;
                case '\t':
                    text.append("\\t");
                    break;
                default:
                    if (symbol < ' ') text.append(String.format("\\u%04x", (int) symbol));
                    else text.append(symbol);
            }
        }
        return text.append('"').toString();
    }
}
