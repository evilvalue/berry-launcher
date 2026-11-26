package ru.berrytrip.berryup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Всё, что делает лаунчер поверх Prism: импорт инстанса из instance.zip,
 * обновление сборки через packwiz и запуск игры.
 */
public final class Sync {

    public interface Progress {
        void stage(String text, double bar);
    }

    private Sync() {
    }

    /** Скачивает instance.zip и разворачивает его в instances/BerryTrip. */
    public static void install(Prism prism, Progress progress) throws Exception {
        if (prism == null) throw new Exception("Prism Launcher не найден");
        File target = new File(prism.instancesDir(), Cfg.BUILD);
        if (prism.hasInstance()) {
            if (progress != null) progress.stage("Сборка уже установлена", 1);
            return;
        }
        if (target.isDirectory() && target.list() != null && target.list().length > 0) {
            throw new Exception("папка instances/" + Cfg.BUILD + " занята — переименуй её и попробуй снова");
        }
        File temp = new File(System.getProperty("java.io.tmpdir", "."), "BerryUp-instance.zip");

        if (progress != null) progress.stage("Скачиваю сборку…", 0.02);
        Log.info("Скачивание сборки: " + Cfg.INSTANCE_URL);
        Http.download(Cfg.INSTANCE_URL, temp, new Http.Progress() {
            @Override
            public void update(long received, long total) {
                if (progress != null) progress.stage("Скачано " + mb(received) + " из " + mb(total), 0.05 + 0.55 * received / Math.max(1, total));
            }
        });

        if (progress != null) progress.stage("Распаковываю…", 0.7);
        unzip(temp, target);
        if (!temp.delete()) temp.deleteOnExit();

        prism.rescan();
        if (!prism.hasInstance()) throw new Exception("в архиве нет инстанса BerryTrip");
        Tweaks.applyAll(prism);
        DistantHorizons.apply(prism);
        if (progress != null) progress.stage("Сборка установлена", 1);
        Log.ok("Сборка установлена: " + target.getAbsolutePath());
    }

    /** Прогоняет packwiz-installer через Java инстанса. Возвращает код выхода. */
    public static int updatePack(Prism prism) throws Exception {
        if (prism == null) throw new Exception("Prism Launcher не найден");
        if (!prism.hasInstance()) throw new Exception("сборка ещё не установлена");
        File java = prism.findJava();
        if (java == null) throw new Exception("Java от Prism не найдена — запусти игру один раз из Prism");
        File bootstrap = prism.bootstrapJar();
        if (!bootstrap.isFile()) throw new Exception("нет packwiz-installer-bootstrap.jar в папке сборки");

        // packwiz перезаписывает config/, options.txt и servers.dat. Снимаем
        // копию и возвращаем её после обновления: чужие настройки не трогаем.
        File snapshot = Configs.snapshot(prism);
        try {
            Tweaks.restore(prism);
            Log.info("Обновление сборки через packwiz…");
            ProcessBuilder builder = new ProcessBuilder(
                    java.getAbsolutePath(), "-jar", bootstrap.getName(), "-g", Cfg.PACK_URL);
            builder.directory(prism.minecraftDir());
            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) Log.info(trimmed);
            }
            int code = process.waitFor();
            Configs.restore(prism, snapshot);
            Tweaks.applyAll(prism);
            if (code == 0) Log.ok("Сборка обновлена, конфиги сохранены.");
            else Log.error("packwiz завершился с кодом " + code + ".");
            return code;
        } finally {
            Configs.discard(snapshot);
        }
    }

    /** Запускает Prism: обычный старт или сразу с входом на выбранный сервер. */
    public static Process launch(Prism prism, boolean connect, int serverIndex) throws IOException {
        if (prism == null) throw new IOException("Prism Launcher не найден");
        if (!prism.hasInstance()) throw new IOException("сборка ещё не установлена");

        Tweaks.applyAll(prism);

        java.util.List<String> command = new java.util.ArrayList<String>();
        command.add(prism.exe.getAbsolutePath());
        command.add("--launch");
        command.add(prism.instanceId());
        Cfg.Server server = null;
        if (connect) {
            server = Cfg.SERVERS[Math.max(0, Math.min(Cfg.SERVERS.length - 1, serverIndex))];
            command.add("--server");
            command.add(server.address());
        }

        Log.info("Запуск: " + join(command));
        ProcessBuilder builder = new ProcessBuilder(command);
        File dir = prism.exe.getParentFile();
        if (dir != null) builder.directory(dir);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        if (server != null) Log.ok("Запуск с подключением: " + server.name + " (" + server.endpoint() + ")");
        else Log.ok("Prism запускается, окно игры откроется само.");
        return process;
    }

    public static String launchCommand(Prism prism, boolean connect, int serverIndex) {
        StringBuilder text = new StringBuilder();
        text.append("--launch \"").append(prism == null ? Cfg.BUILD : prism.instanceId()).append("\"");
        if (connect) {
            Cfg.Server server = Cfg.SERVERS[Math.max(0, Math.min(Cfg.SERVERS.length - 1, serverIndex))];
            text.append(" --server \"").append(server.address()).append("\"");
        }
        return text.toString();
    }

    private static String join(java.util.List<String> parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) builder.append(' ');
            if (part.indexOf(' ') >= 0) builder.append('"').append(part).append('"');
            else builder.append(part);
        }
        return builder.toString();
    }

    private static String mb(long bytes) {
        return String.format(java.util.Locale.ROOT, "%.1f МБ", bytes / 1048576.0);
    }

    /** Распаковка с защитой от путей вида ../. */
    public static void unzip(File zip, File target) throws Exception {
        if (!target.isDirectory() && !target.mkdirs()) throw new Exception("не удалось создать " + target);
        ZipInputStream stream = new ZipInputStream(new FileInputStream(zip));
        try {
            ZipEntry entry;
            byte[] buffer = new byte[16384];
            while ((entry = stream.getNextEntry()) != null) {
                File file = new File(target, entry.getName());
                if (!file.getCanonicalPath().startsWith(target.getCanonicalPath())) {
                    throw new Exception("в архиве недопустимый путь: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    if (!file.isDirectory() && !file.mkdirs()) throw new Exception("не удалось создать " + file);
                    continue;
                }
                File parent = file.getParentFile();
                if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new Exception("не удалось создать " + parent);
                OutputStream out = new FileOutputStream(file);
                try {
                    int read;
                    while ((read = stream.read(buffer)) > 0) out.write(buffer, 0, read);
                } finally {
                    out.close();
                }
            }
        } finally {
            stream.close();
        }
    }

    /** Проверка, что в zip вообще есть instance.cfg — для тестов. */
    public static boolean looksLikeInstance(File zip) {
        if (zip == null || !zip.isFile()) return false;
        ZipInputStream stream = null;
        try {
            stream = new ZipInputStream(new FileInputStream(zip));
            ZipEntry entry;
            while ((entry = stream.getNextEntry()) != null) {
                if (entry.getName().equals("instance.cfg") || entry.getName().endsWith("/instance.cfg")) return true;
            }
        } catch (Exception ignored) {
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
