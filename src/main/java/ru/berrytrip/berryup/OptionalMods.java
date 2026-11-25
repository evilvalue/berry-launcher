package ru.berrytrip.berryup;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Locale;

/** Скачивание и проверка необязательных клиентских модов, которых нет в серверном паке. */
public final class OptionalMods {

    private OptionalMods() {
    }

    public static File cacheDir() {
        return new File(Cfg.configDir(), "optional");
    }

    public static File cached(Tweaks.Mod mod) {
        return mod.filename == null ? null : new File(cacheDir(), mod.filename);
    }

    public static File inMods(Prism prism, Tweaks.Mod mod) {
        return prism == null || mod.filename == null ? null : new File(prism.modsDir(), mod.filename);
    }

    public static File inOff(Prism prism, Tweaks.Mod mod) {
        File off = Tweaks.offDir(prism);
        return off == null || mod.filename == null ? null : new File(off, mod.filename);
    }

    /** Мод уже лежит в моде, отключённой папке или в локальном кэше. */
    public static boolean present(Prism prism, Tweaks.Mod mod) {
        File cached = cached(mod);
        File mods = inMods(prism, mod);
        File off = inOff(prism, mod);
        return (cached != null && cached.isFile())
                || (mods != null && mods.isFile())
                || (off != null && off.isFile());
    }

    /**
     * Кладёт мод в стандартную папку mods, скачивая его только при необходимости.
     * Файл сначала проверяется по размеру и SHA-512 из Modrinth.
     */
    public static void ensure(Prism prism, Tweaks.Mod mod) throws Exception {
        ensure(prism, mod, null);
    }

    public static void ensure(Prism prism, Tweaks.Mod mod, Http.Progress progress) throws Exception {
        if (prism == null || !prism.hasInstance()) throw new Exception("сборка ещё не установлена");
        if (mod.url == null || mod.filename == null) throw new Exception("для мода не задан адрес загрузки");
        File mods = prism.modsDir();
        if (!mods.isDirectory() && !mods.mkdirs()) throw new Exception("не удалось создать папку mods");
        File target = new File(mods, mod.filename);
        if (valid(target, mod)) {
            removeOld(mods, mod);
            File offDir = Tweaks.offDir(prism);
            if (offDir != null) removeOld(offDir, mod);
            return;
        }

        File off = Tweaks.offDir(prism);
        File offJar = new File(off, mod.filename);
        if (valid(offJar, mod)) {
            move(offJar, target);
            removeOld(mods, mod);
            removeOld(off, mod);
            return;
        }

        File cache = cached(mod);
        if (!valid(cache, mod)) {
            File dir = cacheDir();
            if (!dir.isDirectory() && !dir.mkdirs()) throw new Exception("не удалось создать кэш модов");
            File temp = new File(dir, mod.filename + ".part");
            try {
                try {
                    Http.downloadFast(mod.url, temp, progress);
                    if (!valid(temp, mod)) throw new Exception("файл с основного зеркала не совпал по SHA-512");
                } catch (Exception primary) {
                    if (mod.urlFallback == null) throw primary;
                    Files.deleteIfExists(temp.toPath());
                    Http.downloadFast(mod.urlFallback, temp, progress);
                    if (!valid(temp, mod)) throw new Exception("скачанный файл не совпал по SHA-512");
                }
                Files.move(temp.toPath(), cache.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp.toPath());
            }
        }
        Files.copy(cache.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        removeOld(mods, mod);
        if (off != null) removeOld(off, mod);
    }

    /** Убирает старые версии этого мода, чтобы NeoForge не увидел два одинаковых мода. */
    private static void removeOld(File dir, Tweaks.Mod mod) {
        File[] files = dir.listFiles();
        if (files == null) return;
        String keep = mod.filename.toLowerCase(Locale.ROOT);
        for (File file : files) {
            if (!file.isFile()) continue;
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (name.equals(keep)) continue;
            if (name.startsWith("distanthorizons") && name.endsWith(".jar")) {
                try {
                    Files.deleteIfExists(file.toPath());
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static boolean valid(File file, Tweaks.Mod mod) {
        if (file == null || !file.isFile()) return false;
        if (mod.size > 0 && file.length() != mod.size) return false;
        if (mod.sha512 == null || mod.sha512.isEmpty()) return true;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            try (InputStream input = new FileInputStream(file)) {
                byte[] buffer = new byte[16384];
                int read;
                while ((read = input.read(buffer)) > 0) digest.update(buffer, 0, read);
            }
            return hex(digest.digest()).equalsIgnoreCase(mod.sha512);
        } catch (Exception error) {
            return false;
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) out.append(String.format(Locale.ROOT, "%02x", value));
        return out.toString();
    }

    private static void move(File from, File to) throws Exception {
        File parent = to.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new Exception("не удалось создать " + parent.getAbsolutePath());
        }
        try {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception atomic) {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
