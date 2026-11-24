package ru.berrytrip.berryup;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Обновление лаунчера. Работающий jar на Windows заменить нельзя,
 * поэтому новая версия кладётся в %LOCALAPPDATA%/BerryUp/update,
 * а BerryUp.cmd подменяет файл перед следующим запуском.
 */
public final class Updater {

    private Updater() {
    }

    public static File updateDir() {
        return new File(Cfg.LOCAL, "BerryUp/update");
    }

    /** Скачанный и проверенный jar, который BerryUp.cmd перенесёт на место. */
    public static File staged() {
        return new File(updateDir(), "BerryUp.jar");
    }

    public static boolean pending() {
        return staged().isFile();
    }

    public static void clear() {
        File staged = staged();
        if (staged.isFile()) {
            try {
                Files.deleteIfExists(staged.toPath());
            } catch (Exception ignored) {
            }
        }
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
            Log.ok("Обновление " + info.version + " скачано, " + part.getName() + " → " + staged().getName());
            return staged().length();
        } finally {
            if (part.exists() && !part.delete()) part.deleteOnExit();
        }
    }

    /** Папка, где лежат BerryUp.jar и BerryUp.cmd. */
    public static File jarDir() {
        try {
            File jar = new File(Updater.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (jar.isFile()) return jar.getParentFile();
        } catch (Exception ignored) {
        }
        return new File(".");
    }

    public static File cmdScript() {
        return new File(jarDir(), "BerryUp.cmd");
    }

    /** Запущен ли BerryUp из cmd-скрипта: тогда обновление применяется само. */
    public static boolean launchedFromCmd() {
        String line = System.getenv("CMDCMDLINE");
        return line != null && line.toLowerCase(java.util.Locale.ROOT).contains("berryup.cmd");
    }
}
