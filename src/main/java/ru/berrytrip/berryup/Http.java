package ru.berrytrip.berryup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Небольшой HTTP-клиент: только GET и скачивание файла с прогрессом. */
public final class Http {

    public interface Progress {
        void update(long received, long total);
    }

    private Http() {
    }

    public static String get(String url) throws Exception {
        HttpURLConnection connection = open(url);
        try {
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) throw new Exception("HTTP " + code);
            InputStream stream = connection.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = stream.read(chunk)) > 0) buffer.write(chunk, 0, read);
            stream.close();
            String text = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            if (!text.isEmpty() && text.charAt(0) == '\uFEFF') text = text.substring(1);
            return text;
        } finally {
            connection.disconnect();
        }
    }

    /** Ответ HTTP целиком: код нужен для понятных ошибок входа. */
    public static final class Result {
        public final int code;
        public final String body;

        Result(int code, String body) {
            this.code = code;
            this.body = body == null ? "" : body;
        }
    }

    /** GET/POST без исключения на 4xx: тело ошибки читает вызывающий. */
    public static Result call(String method, String url, String json) throws Exception {
        HttpURLConnection connection = open(url);
        try {
            // вход не должен подвешивать окно на минуту, если сервер молчит
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(12000);
            connection.setRequestMethod(method);
            if (json != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                OutputStream out = connection.getOutputStream();
                out.write(json.getBytes(StandardCharsets.UTF_8));
                out.close();
            }
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            if (stream != null) {
                byte[] chunk = new byte[8192];
                int read;
                while ((read = stream.read(chunk)) > 0) buffer.write(chunk, 0, read);
                stream.close();
            }
            String text = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            if (!text.isEmpty() && text.charAt(0) == '\uFEFF') text = text.substring(1);
            return new Result(code, text);
        } finally {
            connection.disconnect();
        }
    }

    /** POST JSON; ответ читается так же, как у GET. */
    public static String post(String url, String json) throws Exception {
        HttpURLConnection connection = open(url);
        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            OutputStream out = connection.getOutputStream();
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.close();
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            if (stream != null) {
                byte[] chunk = new byte[8192];
                int read;
                while ((read = stream.read(chunk)) > 0) buffer.write(chunk, 0, read);
                stream.close();
            }
            if (code < 200 || code >= 300) throw new Exception("HTTP " + code);
            String text = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            if (!text.isEmpty() && text.charAt(0) == '\uFEFF') text = text.substring(1);
            return text;
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Скачивает крупный файл несколькими Range-запросами. Одиночное соединение
     * на некоторых провайдерах отдаёт 5–20 КБ/с, восемь — заметно быстрее.
     * Если сервер не поддерживает Range, бросает исключение для запасного URL.
     */
    public static void downloadFast(String url, File target, Progress progress) throws Exception {
        long total = contentLength(url);
        if (total < 8L * 1024 * 1024) {
            download(url, target, progress);
            return;
        }
        int parts = 8;
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new Exception("не удалось создать папку " + parent.getAbsolutePath());
        }
        List<File> chunks = new ArrayList<File>();
        for (int i = 0; i < parts; i++) chunks.add(new File(parent, target.getName() + ".part" + i));
        ExecutorService pool = Executors.newFixedThreadPool(parts);
        AtomicLong received = new AtomicLong();
        AtomicInteger shown = new AtomicInteger(-1);
        try {
            long size = (total + parts - 1) / parts;
            List<Future<?>> futures = new ArrayList<Future<?>>();
            for (int i = 0; i < parts; i++) {
                final long start = i * size;
                final long end = Math.min(total - 1, start + size - 1);
                final File chunk = chunks.get(i);
                if (start > end) continue;
                futures.add(pool.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            downloadRange(url, chunk, start, end, received, shown, total, progress);
                        } catch (Exception error) {
                            throw new RuntimeException(error);
                        }
                    }
                }));
            }
            for (Future<?> future : futures) future.get();
            OutputStream out = new FileOutputStream(target);
            try {
                byte[] buffer = new byte[16384];
                for (File chunk : chunks) {
                    InputStream input = new java.io.FileInputStream(chunk);
                    try {
                        int read;
                        while ((read = input.read(buffer)) > 0) out.write(buffer, 0, read);
                    } finally {
                        input.close();
                    }
                }
            } finally {
                out.close();
            }
            if (progress != null) progress.update(total, total);
        } finally {
            pool.shutdownNow();
            for (File chunk : chunks) {
                try {
                    java.nio.file.Files.deleteIfExists(chunk.toPath());
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static void downloadRange(String url, File target, long start, long end,
                                      AtomicLong received, AtomicInteger shown, long total,
                                      Progress progress) throws Exception {
        HttpURLConnection connection = open(url);
        try {
            connection.setRequestProperty("Range", "bytes=" + start + "-" + end);
            int code = connection.getResponseCode();
            if (code != 206) throw new Exception("сервер не отдал часть файла: HTTP " + code);
            InputStream stream = connection.getInputStream();
            OutputStream out = new FileOutputStream(target);
            try {
                byte[] chunk = new byte[16384];
                int read;
                while ((read = stream.read(chunk)) > 0) {
                    out.write(chunk, 0, read);
                    long done = received.addAndGet(read);
                    int percent = (int) (done * 100 / total);
                    if (progress != null) {
                        synchronized (shown) {
                            if (percent / 4 > shown.get()) {
                                shown.set(percent / 4);
                                progress.update(done, total);
                            }
                        }
                    }
                }
            } finally {
                out.close();
                stream.close();
            }
        } finally {
            connection.disconnect();
        }
    }

    private static long contentLength(String url) throws Exception {
        HttpURLConnection connection = open(url);
        try {
            connection.setRequestMethod("HEAD");
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) throw new Exception("HTTP " + code);
            return connection.getContentLengthLong();
        } finally {
            connection.disconnect();
        }
    }

    public static void download(String url, File target, Progress progress) throws Exception {
        HttpURLConnection connection = open(url);
        try {
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) throw new Exception("HTTP " + code);
            long total = connection.getContentLengthLong();
            File parent = target.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                throw new Exception("не удалось создать папку " + parent.getAbsolutePath());
            }
            InputStream stream = connection.getInputStream();
            OutputStream out = new FileOutputStream(target);
            try {
                byte[] chunk = new byte[16384];
                long received = 0;
                int step = -1;
                int read;
                while ((read = stream.read(chunk)) > 0) {
                    out.write(chunk, 0, read);
                    received += read;
                    if (progress != null && total > 0) {
                        int percent = (int) (received * 100 / total);
                        if (percent / 4 != step) {
                            step = percent / 4;
                            progress.update(received, total);
                        }
                    }
                }
                if (progress != null && total > 0) progress.update(received, total);
            } finally {
                out.close();
                stream.close();
            }
        } finally {
            connection.disconnect();
        }
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("User-Agent", Cfg.USER_AGENT);
        connection.setInstanceFollowRedirects(true);
        return connection;
    }
}
