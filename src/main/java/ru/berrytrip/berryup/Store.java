package ru.berrytrip.berryup;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Настройки лаунчера в ini-файле. Формат тот же, что у exe-версии,
 * поэтому старый config.ini читается без миграции.
 */
public final class Store {

    private static final Map<String, String> VALUES = new LinkedHashMap<String, String>();

    private Store() {
    }

    public static synchronized void load() {
        VALUES.clear();
        File path = Cfg.configPath();
        if (!path.isFile()) return;
        try {
            for (String line : Files.readAllLines(path.toPath(), StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int index = trimmed.indexOf('=');
                if (index <= 0) continue;
                VALUES.put(trimmed.substring(0, index).trim(), trimmed.substring(index + 1).trim());
            }
        } catch (Exception ignored) {
            // битый файл настроек не должен мешать запуску
        }
    }

    public static synchronized void save() {
        try {
            File dir = Cfg.configDir();
            if (!dir.isDirectory() && !dir.mkdirs()) return;
            List<String> lines = new ArrayList<String>();
            lines.add("# BerryUp: настройки лаунчера");
            for (Map.Entry<String, String> pair : VALUES.entrySet()) {
                lines.add(pair.getKey() + "=" + pair.getValue());
            }
            Files.write(Cfg.configPath().toPath(), lines, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    public static synchronized String get(String key, String fallback) {
        String value = VALUES.get(key);
        return value != null ? value : fallback;
    }

    public static synchronized void set(String key, String value) {
        VALUES.put(key, value == null ? "" : value);
    }

    public static boolean getBool(String key, boolean fallback) {
        String value = get(key, null);
        if (value == null) return fallback;
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    public static void setBool(String key, boolean value) {
        set(key, value ? "1" : "0");
    }

    public static int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(get(key, "").trim());
        } catch (Exception error) {
            return fallback;
        }
    }

    public static String exe() {
        return get("exe", "");
    }

    public static void setExe(String value) {
        set("exe", value);
    }

    public static String data() {
        return get("data", "");
    }

    public static void setData(String value) {
        set("data", value);
    }

    public static boolean autoUpdate() {
        return getBool("autoupdate", true);
    }

    public static void setAutoUpdate(boolean value) {
        setBool("autoupdate", value);
    }

    /** «Запомнить меня»: токен входа живёт в сборке до следующего запуска лаунчера. */
    public static boolean remember() {
        return getBool("auth.remember", true);
    }

    public static void setRemember(boolean value) {
        setBool("auth.remember", value);
    }

    public static boolean minimizeOnPlay() {
        return getBool("minimize", false);
    }

    public static void setMinimizeOnPlay(boolean value) {
        setBool("minimize", value);
    }

    public static int serverIndex() {
        int index = getInt("server", Cfg.SERVERS.length - 1);
        return Math.max(0, Math.min(Cfg.SERVERS.length - 1, index));
    }

    public static void setServerIndex(int index) {
        set("server", String.valueOf(Math.max(0, Math.min(Cfg.SERVERS.length - 1, index))));
    }

    public static String describePaths() {
        return String.format(Locale.ROOT, "настройки: %s", Cfg.configPath().getAbsolutePath());
    }
}
