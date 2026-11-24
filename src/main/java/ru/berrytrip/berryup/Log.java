package ru.berrytrip.berryup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/** Журнал лаунчера: строки с временем, копия в %TEMP%\BerryUp.log. */
public final class Log {

    public static final int INFO = 0;
    public static final int OK = 1;
    public static final int ERROR = 2;

    public interface Listener {
        void line(String text, int level);
    }

    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<Listener>();
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);

    private Log() {
    }

    public static void addListener(Listener listener) {
        if (listener != null) LISTENERS.add(listener);
    }

    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    public static void info(String text) {
        write(INFO, text);
    }

    public static void ok(String text) {
        write(OK, text);
    }

    public static void error(String text) {
        write(ERROR, text);
    }

    private static void write(int level, String text) {
        if (text == null) return;
        String stamp = TIME.format(new Date());
        appendFile(stamp + "  " + text);
        for (Listener listener : LISTENERS) {
            try {
                listener.line(stamp + "  " + text, level);
            } catch (Exception ignored) {
            }
        }
    }

    private static synchronized void appendFile(String line) {
        File file = Cfg.logPath();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8)) {
            writer.write(line);
            writer.write(System.lineSeparator());
        } catch (Exception ignored) {
        }
    }

    public static String path() {
        return Cfg.logPath().getAbsolutePath();
    }
}
