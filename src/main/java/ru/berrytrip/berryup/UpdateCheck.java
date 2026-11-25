package ru.berrytrip.berryup;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Проверка версии лаунчера и сборки по latest.json / pack.toml с GitHub Pages. */
public final class UpdateCheck {

    public static final class Info {
        public String version = "";
        public String url = "";
        public String jar = "";
        public String sha256 = "";
        public String notes = "";
        public long size;
    }

    private static String lastError = "";

    private UpdateCheck() {
    }

    public static String lastError() {
        return lastError;
    }

    public static Info check() {
        lastError = "";
        try {
            String text = Http.get(Cfg.updateUrl());
            JsonObject root = new JsonParser().parse(text).getAsJsonObject();
            Info info = new Info();
            info.version = opt(root, "version");
            info.url = opt(root, "url");
            info.jar = opt(root, "jar");
            if (info.jar.isEmpty()) info.jar = info.url;
            info.sha256 = opt(root, "sha256");
            info.notes = opt(root, "notes");
            if (root.has("size") && !root.get("size").isJsonNull()) {
                try {
                    info.size = root.get("size").getAsLong();
                } catch (Exception ignored) {
                }
            }
            if (info.version.isEmpty() || info.jar.isEmpty()) return null;
            return info;
        } catch (Exception error) {
            lastError = error.getMessage();
            return null;
        }
    }

    private static String opt(JsonObject root, String key) {
        if (!root.has(key) || root.get(key).isJsonNull()) return "";
        return root.get(key).getAsString();
    }

    public static boolean isNewer(Info info) {
        return info != null && compare(info.version, Cfg.VERSION) > 0;
    }

    /** Сравнивает версии по числам: 2.1.2 < 3.0.0. */
    public static int compare(String left, String right) {
        String[] a = left.split("[^0-9]+");
        String[] b = right.split("[^0-9]+");
        int length = Math.max(a.length, b.length);
        for (int i = 0; i < length; i++) {
            int x = i < a.length ? number(a[i]) : 0;
            int y = i < b.length ? number(b[i]) : 0;
            if (x != y) return x < y ? -1 : 1;
        }
        return 0;
    }

    private static int number(String text) {
        try {
            return Integer.parseInt(text);
        } catch (Exception error) {
            return 0;
        }
    }

    /** SHA-256 файла: по нему проверяется скачанный jar. */
    public static String sha256(java.io.File file) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        java.io.InputStream stream = new java.io.FileInputStream(file);
        try {
            byte[] buffer = new byte[16384];
            int read;
            while ((read = stream.read(buffer)) > 0) digest.update(buffer, 0, read);
        } finally {
            stream.close();
        }
        StringBuilder text = new StringBuilder();
        for (byte value : digest.digest()) text.append(String.format("%02x", value));
        return text.toString();
    }

    /** Версия сборки из pack.toml — показывается в диагностике. */
    public static String packVersion() {
        try {
            String text = Http.get(Cfg.PACK_URL);
            Matcher match = Pattern.compile("(?m)^\\s*version\\s*=\\s*\"([^\"]+)\"").matcher(text);
            return match.find() ? match.group(1) : "";
        } catch (Exception error) {
            return "";
        }
    }
}
