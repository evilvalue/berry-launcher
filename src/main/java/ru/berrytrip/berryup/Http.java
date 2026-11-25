package ru.berrytrip.berryup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

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
