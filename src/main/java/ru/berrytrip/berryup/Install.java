package ru.berrytrip.berryup;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Установка BerryUp «как программа»: при первом запуске jar переезжает в
 * %LOCALAPPDATA%\BerryUp, рядом появляется иконка, а на рабочем столе и в меню
 * «Пуск» — ярлыки с нормальной Java (из Prism, а не случайной Java 8, из-за
 * которой на масштабе 125–150% всё мылилось).
 *
 * Обновления тоже становятся проще: лаунчер всегда запущен из одного файла,
 * поэтому самоподмена jar работает без «скачай и замени вручную».
 */
public final class Install {

    private static final String SHORTCUT_SCRIPT =
            "param([string]$Target, [string]$Jar, [string]$WorkDir, [string]$Icon)\n"
                    + "$ErrorActionPreference = 'Stop'\n"
                    + "$shell = New-Object -ComObject WScript.Shell\n"
                    + "$links = @(\n"
                    + "  (Join-Path ([Environment]::GetFolderPath('Desktop')) 'BerryUp.lnk'),\n"
                    + "  (Join-Path ([Environment]::GetFolderPath('Programs')) 'BerryUp.lnk')\n"
                    + ")\n"
                    + "foreach ($path in $links) {\n"
                    + "  $link = $shell.CreateShortcut($path)\n"
                    + "  $link.TargetPath = $Target\n"
                    + "  $link.Arguments = '-Dsun.java2d.uiScale.enabled=true -jar \"' + $Jar + '\"'\n"
                    + "  $link.WorkingDirectory = $WorkDir\n"
                    + "  if (Test-Path $Icon) { $link.IconLocation = $Icon + ',0' }\n"
                    + "  $link.Description = 'BerryTrip launcher'\n"
                    + "  $link.Save()\n"
                    + "  Write-Output $path\n"
                    + "}\n";

    private Install() {
    }

    /** Папка установки: %LOCALAPPDATA%\BerryUp. */
    public static File dir() {
        return new File(Cfg.LOCAL, "BerryUp");
    }

    /** Единственный «настоящий» jar лаунчера после установки. */
    public static File jar() {
        return new File(dir(), "BerryUp.jar");
    }

    public static File iconFile() {
        return new File(dir(), "berry.ico");
    }

    public static boolean installed() {
        File own = Updater.ownJar();
        return own != null && own.getAbsolutePath().equalsIgnoreCase(jar().getAbsolutePath());
    }

    /**
     * Первый запуск из загрузок/рабочего стола: копируем себя в папку установки,
     * делаем ярлыки и открываем уже установленную копию. true — этот процесс
     * должен завершиться, дальше работает установленный BerryUp.
     */
    public static boolean ensure() {
        if (!Cfg.isWindows() || "1".equals(System.getenv("BERRYUP_NO_INSTALL"))) return false;
        File own = Updater.ownJar();
        if (own == null) return false; // запуск из классов при разработке

        if (own.getAbsolutePath().equalsIgnoreCase(jar().getAbsolutePath())) {
            if (!"1".equals(Store.get("installed", ""))) {
                Store.set("installed", "1");
                Store.set("install.path", own.getAbsolutePath());
                Store.save();
            }
            return false;
        }

        File target = install(own);
        if (target == null) return false;
        try {
            relaunch(target, null);
        } catch (Exception error) {
            Log.error("Установка: не удалось открыть установленную копию — " + error.getMessage());
            return false;
        }
        scheduleDelete(own);
        return true;
    }

    /** Копирует jar на место, пишет иконку и ярлыки. Возвращает установленный файл. */
    public static File install(File source) {
        if (source == null) return null;
        try {
            if (!dir().isDirectory() && !dir().mkdirs()) {
                Log.error("Установка: не удалось создать " + dir().getAbsolutePath());
                return null;
            }
            File target = jar();
            if (!source.getAbsolutePath().equalsIgnoreCase(target.getAbsolutePath())) {
                String installedVersion = Updater.jarVersion(target);
                if (target.isFile() && !installedVersion.isEmpty()
                        && UpdateCheck.compare(Cfg.VERSION, installedVersion) <= 0) {
                    // Пользователь мог запустить старый скачанный jar. Не откатываем
                    // уже установленную новую версию: запускаем её.
                    Log.info("Установка: оставляю установленную версию " + installedVersion
                            + " вместо запущенной " + Cfg.VERSION);
                    return target;
                }
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            writeIcon();
            createShortcuts();
            Store.set("installed", "1");
            Store.set("install.path", target.getAbsolutePath());
            Store.save();
            Log.ok("BerryUp установлен: " + target.getAbsolutePath());
            return target;
        } catch (Exception error) {
            Log.error("Установка не удалась: " + error.getMessage());
            return null;
        }
    }

    /** Ярлыки на рабочем столе и в меню «Пуск». */
    public static boolean createShortcuts() {
        if (!Cfg.isWindows()) return false;
        try {
            File target = jar().isFile() ? jar() : Updater.ownJar();
            if (target == null) return false;
            if (!dir().isDirectory() && !dir().mkdirs()) return false;
            writeIcon();

            File script = new File(dir(), "shortcut.ps1");
            Files.write(script.toPath(), SHORTCUT_SCRIPT.getBytes(StandardCharsets.UTF_8));

            List<String> command = new ArrayList<String>();
            command.add("powershell.exe");
            command.add("-NoProfile");
            command.add("-NonInteractive");
            command.add("-ExecutionPolicy");
            command.add("Bypass");
            command.add("-File");
            command.add(script.getAbsolutePath());
            command.add("-Target");
            command.add(javaExe().getAbsolutePath());
            command.add("-Jar");
            command.add(target.getAbsolutePath());
            command.add("-WorkDir");
            command.add(dir().getAbsolutePath());
            command.add("-Icon");
            command.add(iconFile().getAbsolutePath());

            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean done = process.waitFor(20, TimeUnit.SECONDS);
            if (!done) process.destroy();
            boolean ok = done && process.exitValue() == 0;
            if (ok) Log.ok("Ярлыки BerryUp созданы: рабочий стол и меню «Пуск»");
            else Log.error("Ярлыки не созданы (код " + (done ? process.exitValue() : "таймаут") + ")");
            return ok;
        } catch (Exception error) {
            Log.error("Ярлыки не созданы: " + error.getMessage());
            return false;
        }
    }

    /**
     * Java для ярлыка: сначала рантайм из Prism (обычно Java 21 — она умеет
     * HiDPI и не мылит шрифт на масштабе Windows), затем текущая.
     */
    public static File javaExe() {
        try {
            Prism prism = Prism.find();
            File prismJava = prism == null ? null : prism.findJava();
            if (prismJava != null && prismJava.isFile()) return prismJava;
        } catch (Exception ignored) {
        }
        File home = new File(System.getProperty("java.home", ""));
        File javaw = new File(new File(home, "bin"), "javaw.exe");
        if (javaw.isFile()) return javaw;
        File java = new File(new File(home, "bin"), "java.exe");
        if (java.isFile()) return java;
        return new File("javaw.exe");
    }

    /** Перезапуск установленного jar, при необходимости с нужным масштабом UI. */
    public static void relaunch(File jar, String scale) throws Exception {
        List<String> command = new ArrayList<String>();
        command.add(javaExe().getAbsolutePath());
        command.add("-Dsun.java2d.uiScale.enabled=true");
        if (scale != null && !scale.isEmpty() && !"auto".equals(scale)) {
            command.add("-Dsun.java2d.uiScale=" + scale);
            command.add("-Dberryup.scale.applied=1");
        }
        command.add("-jar");
        command.add(jar.getAbsolutePath());
        new ProcessBuilder(command).start();
    }

    /** Применение выбранного масштаба: перезапуск текущего jar. */
    public static void restart(String scale) throws Exception {
        File own = Updater.ownJar();
        if (own == null) throw new Exception("BerryUp запущен не из jar-файла");
        relaunch(own, scale);
    }

    /**
     * Масштаб из настроек применяется через перезапуск, но только если он
     * действительно отличается от текущего: иначе каждый запуск открывал бы
     * второй процесс и приложение стартовало бы вдвое дольше.
     */
    public static boolean applyScale() {
        String scale = Store.get("ui.scale", "");
        if (scale.isEmpty() || "auto".equals(scale)) return false;
        if (System.getProperty("berryup.scale.applied") != null) return false;
        if ("1".equals(System.getenv("BERRYUP_NO_SCALE"))) return false;
        File own = Updater.ownJar();
        if (own == null) return false;

        double target = parseScale(scale);
        double current = currentScale();
        if (target > 0 && current > 0 && Math.abs(target - current) < 0.02) return false;

        try {
            relaunch(own, scale);
            return true;
        } catch (Exception error) {
            Log.error("Масштаб не применился: " + error.getMessage());
            return false;
        }
    }

    private static double parseScale(String value) {
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (Exception error) {
            return -1;
        }
    }

    /** Фактический масштаб окна. На Java 8 метода нет — тогда -1. */
    private static double currentScale() {
        try {
            Object device = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice();
            Object config = device.getClass().getMethod("getDefaultConfiguration").invoke(device);
            Object transform = config.getClass().getMethod("getDefaultTransform").invoke(config);
            return ((java.awt.geom.AffineTransform) transform).getScaleX();
        } catch (Exception ignored) {
            return -1;
        }
    }

    /** Иконка для ярлыка: PNG на 256×256 внутри ICO (Windows Vista и новее). */
    public static void writeIcon() {
        try {
            File out = iconFile();
            if (out.isFile() && out.length() > 1000) return;
            BufferedImage source = null;
            try (InputStream stream = Install.class.getResourceAsStream("/berry.png")) {
                if (stream != null) source = ImageIO.read(stream);
            }
            if (source == null) return;

            BufferedImage canvas = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = canvas.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(source, 0, 0, 256, 256, null);
            g.dispose();

            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(canvas, "png", png);
            byte[] data = png.toByteArray();

            ByteBuffer ico = ByteBuffer.allocate(22 + data.length).order(ByteOrder.LITTLE_ENDIAN);
            ico.put((byte) 0).put((byte) 0);      // reserved
            ico.put((byte) 1).put((byte) 0);      // type: icon
            ico.put((byte) 1).put((byte) 0);      // 1 image
            ico.put((byte) 0);                    // width 0 = 256
            ico.put((byte) 0);                    // height 0 = 256
            ico.put((byte) 0);                    // palette
            ico.put((byte) 0);                    // reserved
            ico.putShort((short) 1);              // planes
            ico.putShort((short) 32);             // bpp
            ico.putInt(data.length);
            ico.putInt(22);
            ico.put(data);
            Files.write(out.toPath(), ico.array());
        } catch (Exception error) {
            Log.error("Иконка не создана: " + error.getMessage());
        }
    }

    /** Удаляет исходный jar из «Загрузок» после переезда — уже не нужен. */
    private static void scheduleDelete(File file) {
        try {
            new ProcessBuilder("cmd.exe", "/c",
                    "ping -n 4 127.0.0.1 >nul & del /f /q \"" + file.getAbsolutePath() + "\"")
                    .redirectErrorStream(true).start();
        } catch (Exception ignored) {
        }
    }
}
