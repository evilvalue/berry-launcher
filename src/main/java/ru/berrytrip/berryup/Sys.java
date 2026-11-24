package ru.berrytrip.berryup;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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
