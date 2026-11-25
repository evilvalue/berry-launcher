package ru.berrytrip.berryup;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Обновление лаунчера. Работающий jar на Windows заменить нельзя, поэтому
 * новая версия сначала скачивается в %LOCALAPPDATA%/BerryUp/update, а затем
 * этот же jar запускается вторым процессом с --apply-update: он дожидается,
 * пока старое окно отпустит файл, подменяет BerryUp.jar и открывает уже
 * новую версию. Никаких скриптов рядом с лаунчером не нужно.
 */
public final class Updater {

    private static final long RETRY_MS = 300;
    private static final long TIMEOUT_MS = 60000;

    private Updater() {
    }

    public static File updateDir() {
        return new File(Cfg.LOCAL, "BerryUp/update");
    }

    /** Скачанный и проверенный jar, который заменит текущий файл. */
    public static File staged() {
        return new File(updateDir(), "BerryUp.jar");
    }

    public static boolean pending() {
        return staged().isFile();
    }

    /** Версия из MANIFEST jar — нужна, чтобы старый файл не затёр новую установку. */
    public static String jarVersion(File file) {
        if (file == null || !file.isFile()) return "";
        try (JarFile jar = new JarFile(file)) {
            Manifest manifest = jar.getManifest();
            if (manifest != null) {
                String value = manifest.getMainAttributes().getValue("Implementation-Version");
                if (value != null && !value.trim().isEmpty()) return value.trim();
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    /** Файл, из которого запущен BerryUp; null — если запущен из папки классов. */
    public static File ownJar() {
        try {
            File jar = new File(Updater.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (jar.isFile() && jar.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) return jar;
        } catch (Exception ignored) {
        }
        return null;
    }

    public static File jarDir() {
        File jar = ownJar();
        return jar != null ? jar.getParentFile() : new File(".");
    }

    /**
     * Скачивает jar из latest-java.json, сверяет SHA-256 и оставляет его
     * в очереди на замену. Возвращает размер файла.
     */
    public static long stage(UpdateCheck.Info info, Http.Progress progress) throws Exception {
        if (info == null || info.jar == null || info.jar.isEmpty()) {
            throw new Exception("в latest-java.json нет ссылки на BerryUp.jar");
        }
        if (!updateDir().isDirectory() && !updateDir().mkdirs()) {
            throw new Exception("не удалось создать " + updateDir().getAbsolutePath());
        }
        File part = new File(updateDir(), "BerryUp.jar.part");
        try {
            Http.download(info.jar, part, progress);
            if (part.length() == 0) throw new Exception("скачался пустой файл");
            if (info.size > 0 && part.length() != info.size) {
                throw new Exception("размер не совпал: ожидался " + info.size + ", скачан " + part.length());
            }
            String hash = UpdateCheck.sha256(part);
            if (!info.sha256.isEmpty() && !hash.equalsIgnoreCase(info.sha256)) {
                throw new Exception("хеш не совпал — файл повреждён");
            }
            Files.move(part.toPath(), staged().toPath(), StandardCopyOption.REPLACE_EXISTING);
            Log.ok("Обновление " + info.version + " скачано: " + staged().getAbsolutePath());
            return staged().length();
        } finally {
            if (part.exists() && !part.delete()) part.deleteOnExit();
        }
    }

    /** Запускает установщик обновления: второй процесс подменит jar и откроет новую версию. */
    public static void apply() throws Exception {
        File own = ownJar();
        if (own == null) throw new Exception("BerryUp запущен не из jar-файла");
        if (!staged().isFile()) throw new Exception("обновление ещё не скачано");
        List<String> command = new ArrayList<String>();
        command.add(javaExecutable("javaw"));
        command.add("-jar");
        command.add(staged().getAbsolutePath());
        command.add("--apply-update");
        command.add(own.getAbsolutePath());
        new ProcessBuilder(command).start();
        Log.ok("Установщик обновления запущен: " + own.getName() + " будет заменён");
    }

    /**
     * Второй запуск (--apply-update): ждёт, пока работающий BerryUp отпустит
     * файл, копирует новый jar на его место и открывает лаунчер заново.
     */
    public static void applyUpdate(String targetPath) {
        File target = new File(targetPath);
        File source = ownJar();
        if (source == null) {
            Log.error("Обновление: не найден собственный jar");
            return;
        }
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (true) {
            try {
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Log.ok("BerryUp.jar заменён: " + target.getAbsolutePath());
                break;
            } catch (Exception busy) {
                if (System.currentTimeMillis() > deadline) {
                    Log.error("Обновление: не удалось заменить " + target.getName() + ": " + busy.getMessage());
                    return;
                }
                try {
                    Thread.sleep(RETRY_MS);
                } catch (InterruptedException stop) {
                    return;
                }
            }
        }
        // BERRYUP_NO_RELAUNCH=1 оставляет проверку без открытия окна.
        if (!"1".equals(System.getenv("BERRYUP_NO_RELAUNCH"))) relaunch(target);
    }

    /** Убирает скачанный jar, если лаунчер уже работает из него (после обновления). */
    public static void cleanup() {
        File staged = staged();
        File own = ownJar();
        if (!staged.isFile() || own == null) return;
        if (staged.getAbsolutePath().equalsIgnoreCase(own.getAbsolutePath())) return;
        try {
            if (staged.length() != own.length()) return;
            if (!UpdateCheck.sha256(staged).equalsIgnoreCase(UpdateCheck.sha256(own))) return;
        } catch (Exception unreadable) {
            return;
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                Files.deleteIfExists(staged.toPath());
                return;
            } catch (Exception busy) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException stop) {
                    return;
                }
            }
        }
    }

    private static void relaunch(File jar) {
        try {
            List<String> command = new ArrayList<String>();
            command.add(javaExecutable("javaw"));
            command.add("-jar");
            command.add(jar.getAbsolutePath());
            new ProcessBuilder(command).start();
        } catch (Exception error) {
            Log.error("Обновление: не удалось открыть новую версию: " + error.getMessage());
        }
    }

    /** javaw/java того рантайма, на котором сейчас работает BerryUp (обычно Java из Prism). */
    private static String javaExecutable(String name) {
        File bin = new File(new File(System.getProperty("java.home", ""), "bin"),
                name + (Cfg.isWindows() ? ".exe" : ""));
        return bin.isFile() ? bin.getAbsolutePath() : name;
    }
}
