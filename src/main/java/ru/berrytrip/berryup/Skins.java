package ru.berrytrip.berryup;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;

/**
 * Скины QuickSkin. Файл кладётся в quickskin/uploads/skins — мод сканирует эту
 * папку и показывает скин в своём меню. В quickskin-client.json прописывается
 * хеш, чтобы мод попробовал применить его сразу.
 */
public final class Skins {

    private Skins() {
    }

    public static File directory(Prism prism) {
        return new File(prism.minecraftDir(), "quickskin/uploads/skins");
    }

    /** Возвращает имя файла, который лёг в папку скинов. */
    public static String apply(Prism prism, File source, String nick) throws Exception {
        if (prism == null || !prism.hasInstance()) {
            throw new Exception("сборка ещё не установлена");
        }
        byte[] bytes = Files.readAllBytes(source.toPath());
        File dir = directory(prism);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new Exception("не удалось создать " + dir.getAbsolutePath());
        }
        File target = new File(dir, fileName(source.getName()));
        Files.write(target.toPath(), bytes);
        updateConfig(prism, "sha256-" + sha256(bytes), nick);
        return target.getName();
    }

    private static void updateConfig(Prism prism, String hash, String nick) {
        File config = new File(prism.minecraftDir(), "config/quickskin-client.json");
        if (!config.isFile()) return;
        try {
            JsonObject json = JsonParser.parseString(Sys.read(config)).getAsJsonObject();
            Files.copy(config.toPath(),
                    new File(config.getParentFile(), "quickskin-client.json.bak").toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            json.addProperty("activeSkinHash", hash);
            json.addProperty("playerOwnSkinHash", hash);
            json.addProperty("playerOwnSkinHidden", false);
            if (nick != null && !nick.isEmpty()) {
                String account = UUID.nameUUIDFromBytes(
                        ("OfflinePlayer:" + nick).getBytes(StandardCharsets.UTF_8)) + ":" + nick;
                json.addProperty("playerOwnSkinAccount", account);
                JsonObject accounts = json.has("accountSkins")
                        ? json.getAsJsonObject("accountSkins") : new JsonObject();
                JsonObject skins = accounts.has("skins")
                        ? accounts.getAsJsonObject("skins") : new JsonObject();
                skins.addProperty(account, hash);
                accounts.add("skins", skins);
                accounts.addProperty("migrated", true);
                json.add("accountSkins", accounts);
            }
            Files.write(config.toPath(), json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) {
            Log.error("QuickSkin: конфиг не обновлён — " + error.getMessage());
        }
    }

    private static String fileName(String original) {
        String name = original == null ? "skin.png" : original.toLowerCase(Locale.ROOT);
        name = name.replaceAll("[^a-z0-9._-]+", "-");
        if (!name.endsWith(".png")) name = name + ".png";
        if (name.length() < 5) name = "skin.png";
        return name;
    }

    private static String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        StringBuilder text = new StringBuilder(hash.length * 2);
        for (byte value : hash) text.append(String.format("%02x", value));
        return text.toString();
    }
}
