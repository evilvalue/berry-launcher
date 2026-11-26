package ru.berrytrip.berryup;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

/**
 * Приведение Distant Horizons к рабочему состоянию.
 *
 * <p>DH называет папку сохранений по имени сервера. Русское имя
 * «Прямое подключение» превращается в percent-кодированную строку из 148
 * символов, и путь к SQLite становится длиннее 260 символов — Windows такое
 * имя открыть не даёт, DH пишет {@code SQLITE_CANTOPEN} и не грузит LOD.
 * Поэтому папки называются по IP ({@code IP_ONLY}): коротко и без кириллицы.</p>
 *
 * <p>Старую папку с процентами переносим в новую, чтобы уже собранные LOD
 * не потерялись.</p>
 */
public final class DistantHorizons {

    private static final String DATA_DIR = "Distant_Horizons_server_data";
    private static final String FOLDER_MODE = "serverfoldernamemode";

    private DistantHorizons() {
    }

    /** Чинит конфиг и папки, если DH уже запускался в этой сборке. */
    public static void apply(Prism prism) {
        if (prism == null || !prism.hasInstance()) return;
        File config = new File(new File(prism.minecraftDir(), "config"), "DistantHorizons.toml");
        boolean changed = config.isFile() && fixFolderMode(config);
        changed |= migrate(prism);
        if (changed) {
            Log.ok("Distant Horizons: папки LOD называются по IP — длинный путь больше не ломает базу.");
        }
    }

    private static boolean fixFolderMode(File config) {
        try {
            List<String> lines = Files.readAllLines(config.toPath(), StandardCharsets.UTF_8);
            boolean changed = false;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String trimmed = line.trim().toLowerCase(Locale.ROOT);
                if (!trimmed.startsWith(FOLDER_MODE)) continue;
                int equals = line.indexOf('=');
                if (equals < 0) continue;
                String value = line.substring(equals + 1).trim();
                if (value.equalsIgnoreCase("\"IP_ONLY\"")) return false;
                lines.set(i, line.substring(0, equals + 1) + " \"IP_ONLY\"");
                changed = true;
            }
            if (changed) Files.write(config.toPath(), lines, StandardCharsets.UTF_8);
            return changed;
        } catch (Exception error) {
            Log.error("Distant Horizons: конфиг не поправлен — " + error.getMessage());
            return false;
        }
    }

    /** Переносит старые папки с %-кодированием в короткую папку с IP. */
    private static boolean migrate(Prism prism) {
        File root = new File(prism.minecraftDir(), DATA_DIR);
        File[] children = root.listFiles();
        if (children == null) return false;
        File target = new File(root, Cfg.SERVER_HOST);
        boolean changed = false;
        for (File child : children) {
            if (!child.isDirectory() || child.getName().indexOf('%') < 0) continue;
            changed |= merge(child, target);
        }
        return changed;
    }

    private static boolean merge(File from, File to) {
        File[] entries = from.listFiles();
        if (entries == null) return false;
        boolean moved = false;
        if (!to.isDirectory() && !to.mkdirs()) return false;
        for (File entry : entries) {
            Path destination = new File(to, entry.getName()).toPath();
            if (Files.exists(destination)) continue;
            try {
                Files.move(entry.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
                moved = true;
            } catch (Exception error) {
                Log.error("Distant Horizons: не удалось перенести " + entry.getName()
                        + " — " + error.getMessage());
            }
        }
        if (moved) {
            File[] left = from.listFiles();
            if (left != null && left.length == 0) {
                try {
                    Files.deleteIfExists(from.toPath());
                } catch (Exception ignored) {
                }
            }
        }
        return moved;
    }
}
