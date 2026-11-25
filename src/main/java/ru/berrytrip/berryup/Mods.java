package ru.berrytrip.berryup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;

/**
 * berryauth лежит внутри BerryUp.jar и копируется в mods инстанса: без него
 * клиент не пришлёт токен и не сохранит новый после /verify.
 *
 * <p>Файл сверяется с копией внутри BerryUp: если packwiz принёс другую версию
 * или в mods остался старый jar с другим именем, лаунчер приводит папку в порядок.</p>
 */
public final class Mods {

    public static final String NAME = "berryauth-1.1.0.jar";

    private Mods() {
    }

    public static boolean ensure(Prism prism) {
        if (prism == null || !prism.hasInstance()) {
            return false;
        }
        File mods = prism.modsDir();
        if (!mods.isDirectory() && !mods.mkdirs()) {
            return false;
        }
        byte[] embedded = readEmbedded();
        if (embedded == null) {
            Log.error("В BerryUp нет berryauth.jar");
            return false;
        }
        File target = new File(mods, NAME);
        try {
            if (target.isFile() && Arrays.equals(Files.readAllBytes(target.toPath()), embedded)) {
                removeStale(mods, target);
                return true;
            }
            File temp = new File(mods, NAME + ".tmp");
            Files.write(temp.toPath(), embedded);
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            removeStale(mods, target);
            Log.ok("Мод входа установлен в сборку: " + NAME);
            return true;
        } catch (Exception error) {
            Log.error("Не удалось положить мод входа: " + error.getMessage());
            return false;
        }
    }

    /** Убирает старые berryauth-*.jar, иначе два мода входа конфликтуют. */
    private static void removeStale(File mods, File keep) {
        File[] files = mods.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (!file.isFile() || !name.startsWith("berryauth") || !name.endsWith(".jar")) {
                continue;
            }
            if (file.getName().equalsIgnoreCase(keep.getName())) {
                continue;
            }
            if (file.delete()) {
                Log.info("Удалён старый мод входа: " + file.getName());
            }
        }
    }

    private static byte[] readEmbedded() {
        try (InputStream input = Mods.class.getResourceAsStream("/berryauth.jar")) {
            if (input == null) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            byte[] bytes = out.toByteArray();
            return bytes.length > 10_000 ? bytes : null;
        } catch (Exception error) {
            Log.error("Не удалось прочитать мод входа: " + error.getMessage());
            return null;
        }
    }
}
