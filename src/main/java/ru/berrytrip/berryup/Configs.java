package ru.berrytrip.berryup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Конфиги инстанса: обновление сборки не должно затирать настройки игрока.
 *
 * <p>packwiz считает любой изменённый файл из pack.toml «сломанным» и качает
 * исходный. Поэтому вокруг обновления снимается снимок config/, options.txt,
 * servers.dat и defaultconfigs, а после — возвращается на место. Кнопка
 * «Сбросить конфиги» наоборот удаляет пользовательские файлы и распаковывает
 * исходный набор из launcher/configs.zip. Сохранения, моды и вход не трогаем.</p>
 */
public final class Configs {

    private static final String[] FILES = {
            "options.txt", "servers.dat", "servers.dat_old", "keybindings.txt"
    };
    private static final String[] DIRS = {"config", "defaultconfigs"};
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final int KEEP_BACKUPS = 5;

    private Configs() {
    }

    /** Снимок перед обновлением: временная папка с копией конфигов. */
    public static File snapshot(Prism prism) throws Exception {
        if (prism == null || !prism.hasInstance()) return null;
        File minecraft = prism.minecraftDir();
        File dir = Files.createTempDirectory("berryup-configs-").toFile();
        copyInto(minecraft, dir);
        return dir;
    }

    /** Возвращает снимок поверх обновлённых файлов, ничего не удаляя. */
    public static void restore(Prism prism, File snapshot) throws Exception {
        if (snapshot == null || !snapshot.isDirectory()) return;
        File minecraft = prism.minecraftDir();
        restoreFrom(snapshot, minecraft);
    }

    public static void discard(File snapshot) {
        if (snapshot != null) delete(snapshot);
    }

    /**
     * Сброс к состоянию только что скачанного инстанса.
     *
     * @return папка, куда сохранены текущие конфиги
     */
    public static File reset(Prism prism, Sync.Progress progress) throws Exception {
        if (prism == null || !prism.hasInstance()) {
            throw new Exception("сборка ещё не установлена");
        }
        File minecraft = prism.minecraftDir();
        File backup = new File(prism.instanceDir(),
                "berry-config-backup" + File.separator + LocalDateTime.now().format(STAMP));
        if (!backup.isDirectory() && !backup.mkdirs()) {
            throw new Exception("не удалось создать " + backup.getAbsolutePath());
        }
        stage(progress, "Сохраняю текущие конфиги…", 0.05);
        copyInto(minecraft, backup);

        // Токен входа — не конфиг: после сброса игрок остаётся в аккаунте.
        byte[] token = readToken(minecraft);

        stage(progress, "Убираю старые конфиги…", 0.2);
        for (String dir : DIRS) delete(new File(minecraft, dir));
        for (String file : FILES) delete(new File(minecraft, file));

        File zip = File.createTempFile("berryup-configs-", ".zip");
        try {
            stage(progress, "Скачиваю исходные конфиги…", 0.3);
            Http.download(Cfg.configsUrl(), zip, (received, total) -> {
                double ratio = total > 0 ? received / (double) total : 0;
                stage(progress, "Скачиваю исходные конфиги… " + Math.round(ratio * 100) + "%",
                        0.3 + 0.45 * ratio);
            });
            stage(progress, "Распаковываю…", 0.8);
            Sync.unzip(zip, minecraft);
        } finally {
            if (!zip.delete()) zip.deleteOnExit();
        }
        if (token != null) writeToken(minecraft, token);

        stage(progress, "Применяю настройки лаунчера…", 0.9);
        Tweaks.applyAll(prism);
        prune(prism);
        stage(progress, "Конфиги сброшены", 1);
        Log.ok("Конфиги сброшены к исходным. Копия: " + backup.getAbsolutePath());
        return backup;
    }

    private static void stage(Sync.Progress progress, String text, double value) {
        if (progress != null) progress.stage(text, value);
    }

    private static byte[] readToken(File minecraft) {
        File token = new File(minecraft, "config/berryauth-token.json");
        if (!token.isFile()) return null;
        try {
            return Files.readAllBytes(token.toPath());
        } catch (Exception error) {
            Log.error("Не удалось сохранить вход: " + error.getMessage());
            return null;
        }
    }

    private static void writeToken(File minecraft, byte[] bytes) {
        File token = new File(minecraft, "config/berryauth-token.json");
        File parent = token.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) return;
        try (OutputStream out = new FileOutputStream(token)) {
            out.write(bytes);
        } catch (Exception error) {
            Log.error("Не удалось вернуть вход: " + error.getMessage());
        }
    }

    private static void copyInto(File minecraft, File target) throws Exception {
        for (String dir : DIRS) {
            File source = new File(minecraft, dir);
            if (source.isDirectory()) copyTree(source, new File(target, dir));
        }
        for (String file : FILES) {
            File source = new File(minecraft, file);
            if (source.isFile()) copy(source, new File(target, file));
        }
    }

    private static void restoreFrom(File snapshot, File minecraft) throws Exception {
        for (String dir : DIRS) {
            File source = new File(snapshot, dir);
            if (source.isDirectory()) copyTree(source, new File(minecraft, dir));
        }
        for (String file : FILES) {
            File source = new File(snapshot, file);
            if (source.isFile()) copy(source, new File(minecraft, file));
        }
    }

    private static void copyTree(File source, File target) throws Exception {
        if (!target.isDirectory() && !target.mkdirs()) {
            throw new Exception("не удалось создать " + target.getAbsolutePath());
        }
        File[] children = source.listFiles();
        if (children == null) return;
        for (File child : children) {
            File destination = new File(target, child.getName());
            if (child.isDirectory()) copyTree(child, destination);
            else if (child.isFile()) copy(child, destination);
        }
    }

    private static void copy(File source, File target) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("не удалось создать " + parent.getAbsolutePath());
        }
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void delete(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) delete(child);
        }
        if (!file.delete()) file.deleteOnExit();
    }

    /** Старые копии не храним вечно: последние пять достаточно. */
    private static void prune(Prism prism) {
        File root = new File(prism.instanceDir(), "berry-config-backup");
        File[] dirs = root.listFiles(File::isDirectory);
        if (dirs == null || dirs.length <= KEEP_BACKUPS) return;
        List<File> sorted = new ArrayList<File>(Arrays.asList(dirs));
        sorted.sort(Comparator.comparing(File::getName));
        for (int i = 0; i < sorted.size() - KEEP_BACKUPS; i++) delete(sorted.get(i));
    }
}
