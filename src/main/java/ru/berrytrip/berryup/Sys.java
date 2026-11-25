package ru.berrytrip.berryup;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/** Открытие ссылок и папок — общее для всех экранов. */
public final class Sys {

    private Sys() {
    }

    public static void openUrl(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
        } catch (Exception ignored) {
        }
        try {
            if (Cfg.isWindows()) new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            else new ProcessBuilder("xdg-open", url).start();
        } catch (Exception error) {
            Log.error("Не удалось открыть ссылку: " + error.getMessage());
        }
    }

    /**
     * Открывает вход в Telegram напрямую через tg://, а не через t.me.
     *
     * <p>Telegram Desktop при повторном открытии https-ссылки иногда
     * показывает Start и отправляет боту просто /start без start-параметра.
     * tg://resolve с тем же start-параметром передаёт токен напрямую.</p>
     */
    public static void openTelegram(String webLink) {
        if (webLink == null || webLink.isEmpty()) return;
        String tg = telegramUri(webLink);
        if (tg != null && canOpenTelegram()) {
            try {
                if (Cfg.isWindows()) {
                    new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", tg).start();
                } else if (Desktop.isDesktopSupported()
                        && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(tg));
                } else {
                    new ProcessBuilder("xdg-open", tg).start();
                }
                return;
            } catch (Exception error) {
                Log.error("Не удалось открыть Telegram напрямую: " + error.getMessage());
            }
        }
        openUrl(webLink);
    }

    private static boolean canOpenTelegram() {
        if (!Cfg.isWindows()) return true;
        try {
            Process process = new ProcessBuilder("reg", "query",
                    "HKEY_CLASSES_ROOT\\tg\\shell\\open\\command").redirectErrorStream(true).start();
            boolean finished = process.waitFor(2, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception error) {
            return false;
        }
    }

    /** https://t.me/<bot>?start=<payload> -> tg://resolve?domain=<bot>&start=<payload>. */
    private static String telegramUri(String link) {
        try {
            URI uri = URI.create(link);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !"t.me".equalsIgnoreCase(uri.getHost())) {
                return null;
            }
            String bot = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/+", "");
            String query = uri.getRawQuery();
            if (bot.isEmpty() || bot.contains("/") || query == null || !query.startsWith("start=")) {
                return null;
            }
            String payload = query.substring("start=".length());
            if (payload.isEmpty()) return null;
            return "tg://resolve?domain=" + URLEncoder.encode(bot, "UTF-8")
                    + "&start=" + payload;
        } catch (Exception error) {
            return null;
        }
    }

    public static void openFolder(File dir) {
        if (dir == null) return;
        if (!dir.isDirectory()) {
            Log.error("Папка не найдена: " + dir.getAbsolutePath());
            return;
        }
        openFile(dir);
    }

    public static void openFile(File file) {
        if (file == null || !file.exists()) return;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
                return;
            }
        } catch (Exception ignored) {
        }
        try {
            if (Cfg.isWindows()) new ProcessBuilder("explorer", file.getAbsolutePath()).start();
            else new ProcessBuilder("xdg-open", file.getAbsolutePath()).start();
        } catch (Exception error) {
            Log.error("Не удалось открыть: " + error.getMessage());
        }
    }

    /** Копирование в буфер обмена с несколькими попытками, как в exe-версии. */
    public static boolean copy(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new java.awt.datatransfer.StringSelection(text), null);
                return true;
            } catch (Exception error) {
                try {
                    Thread.sleep(80);
                } catch (InterruptedException ignored) {
                }
            }
        }
        return false;
    }

    public static String read(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (Exception error) {
            return "";
        }
    }

    /** Короткий путь с многоточием посередине: видно начало и хвост. */
    public static String clipPath(String path, int max) {
        if (path == null) return "";
        if (max <= 8 || path.length() <= max) return path;
        int head = (max - 3) / 3;
        int tail = max - 3 - head;
        return path.substring(0, head) + "…" + path.substring(path.length() - tail);
    }
}
