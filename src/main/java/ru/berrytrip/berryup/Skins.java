package ru.berrytrip.berryup;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;

/**
 * Скины и плащи QuickSkin.
 *
 * <p>Мод держит собственный пул файлов: {@code quickskin/uploads/skins} и
 * {@code quickskin/uploads/capes}. Лаунчер кладёт выбранный файл именно туда,
 * поэтому он сразу появляется в меню QuickSkin. Форматы проверяются по тем же
 * правилам, что и в моде:</p>
 *
 * <ul>
 *   <li>файлы PNG или GIF, не больше 32 МБ;</li>
 *   <li>скины — 64×32 (старый формат) или квадрат 64…2048;</li>
 *   <li>плащи — 64×32 … 2048×1024 (2:1 на кадр, допускаются анимированные полосы).</li>
 * </ul>
 */
public final class Skins {

    /** Ограничение LocalAssetManager в QuickSkin. */
    public static final int MAX_BYTES = 33554432;
    public static final int MIN_SIZE = 64;
    public static final int MAX_SIZE = 2048;

    private static final byte[] CAPE_DOMAIN = "quickskin:cape\0".getBytes(StandardCharsets.UTF_8);

    private Skins() {
    }

    /** Проверенный файл: картинка для превью и данные для пула мода. */
    public static final class Asset {
        public final File file;
        public final BufferedImage image;
        public final int width;
        public final int height;
        public final boolean animated;
        public final boolean cape;

        Asset(File file, BufferedImage image, int width, int height, boolean animated, boolean cape) {
            this.file = file;
            this.image = image;
            this.width = width;
            this.height = height;
            this.animated = animated;
            this.cape = cape;
        }

        public String sizeText() {
            return width + "×" + height + (animated ? " · GIF" : "");
        }
    }

    public static File skinDir(Prism prism) {
        return new File(prism.minecraftDir(), "quickskin/uploads/skins");
    }

    public static File capeDir(Prism prism) {
        return new File(prism.minecraftDir(), "quickskin/uploads/capes");
    }

    /** Последний скин из пула QuickSkin — для 3D-превью при старте. */
    public static BufferedImage current(Prism prism) {
        Asset asset = currentAsset(prism, false);
        return asset == null ? null : asset.image;
    }

    public static BufferedImage currentCape(Prism prism) {
        Asset asset = currentAsset(prism, true);
        return asset == null ? null : asset.image;
    }

    public static Asset currentAsset(Prism prism, boolean cape) {
        if (prism == null || !prism.hasInstance()) return null;
        File[] files = (cape ? capeDir(prism) : skinDir(prism)).listFiles();
        if (files == null) return null;
        File newest = null;
        for (File file : files) {
            if (!file.isFile() || !isImage(file)) continue;
            if (newest == null || file.lastModified() > newest.lastModified()) newest = file;
        }
        if (newest == null) return null;
        try {
            return read(newest, cape);
        } catch (Exception error) {
            return null;
        }
    }

    /** Читает и проверяет скин, не копируя его в пул. */
    public static Asset readSkin(File file) throws Exception {
        return read(file, false);
    }

    /** Читает и проверяет плащ, не копируя его в пул. */
    public static Asset readCape(File file) throws Exception {
        return read(file, true);
    }

    private static Asset read(File file, boolean cape) throws Exception {
        if (file == null || !file.isFile()) throw new Exception("файл не найден");
        if (file.length() > MAX_BYTES) throw new Exception("файл больше 32 МБ — QuickSkin его не примет");
        if (!isImage(file)) throw new Exception("QuickSkin принимает только PNG и GIF");
        BufferedImage image = ImageIO.read(file);
        if (image == null) throw new Exception("не удалось прочитать картинку");
        int width = image.getWidth();
        int height = image.getHeight();
        if (cape) {
            if (!capeSize(width, height)) {
                throw new Exception("плащ должен быть 2:1: 64×32, 128×64, 256×128, 512×256, 1024×512 или 2048×1024");
            }
        } else if (!skinSize(width, height)) {
            throw new Exception("скин должен быть 64×32 или квадратом 64×64 … 2048×2048");
        }
        boolean animated = file.getName().toLowerCase(Locale.ROOT).endsWith(".gif");
        return new Asset(file, image, width, height, animated, cape);
    }

    public static boolean skinSize(int width, int height) {
        if (width == 64 && height == 32) return true;
        return width == height && powerOfTwo(width) && width >= MIN_SIZE && width <= MAX_SIZE;
    }

    public static boolean capeSize(int width, int height) {
        if (!powerOfTwo(width) || width < MIN_SIZE || width > MAX_SIZE) return false;
        int frame = width / 2;
        if (frame <= 0 || height % frame != 0) return false;
        int frames = height / frame;
        return frames >= 1 && frames <= 256;
    }

    private static boolean powerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    /**
     * Кладёт скин в пул QuickSkin и прописывает его в конфиг мода.
     * Возвращает имя файла в папке скинов.
     */
    public static String applySkin(Prism prism, File source, String nick) throws Exception {
        Asset asset = readSkin(source);
        File target = copy(prism, asset, false);
        String hash = "sha256-" + contentId(bytes(target), false);
        updateSkinConfig(prism, hash, nick);
        return target.getName();
    }

    /**
     * Кладёт плащ в пул QuickSkin и включает его в конфиге.
     * Возвращает имя файла в папке плащей.
     */
    public static String applyCape(Prism prism, File source) throws Exception {
        Asset asset = readCape(source);
        File target = copy(prism, asset, true);
        String hash = "sha256-" + contentId(bytes(target), true);
        updateCapeConfig(prism, hash);
        return target.getName();
    }

    /** Убирает активный плащ так же, как это делает меню QuickSkin. */
    public static void clearCape(Prism prism) {
        updateCapeConfig(prism, "known:__NONE__");
    }

    private static File copy(Prism prism, Asset asset, boolean cape) throws Exception {
        if (prism == null || !prism.hasInstance()) throw new Exception("сборка ещё не установлена");
        File dir = cape ? capeDir(prism) : skinDir(prism);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new Exception("не удалось создать " + dir.getAbsolutePath());
        }
        File target = new File(dir, fileName(asset.file.getName(), cape));
        Files.copy(asset.file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private static void updateSkinConfig(Prism prism, String hash, String nick) {
        updateConfig(prism, json -> {
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
        });
    }

    private static void updateCapeConfig(Prism prism, String hash) {
        updateConfig(prism, json -> json.addProperty("activeCapeHash", hash));
    }

    private interface ConfigEdit {
        void apply(JsonObject json) throws Exception;
    }

    private static void updateConfig(Prism prism, ConfigEdit edit) {
        if (prism == null) return;
        File config = new File(prism.minecraftDir(), "config/quickskin-client.json");
        if (!config.isFile()) return;
        try {
            JsonObject json = JsonParser.parseString(Sys.read(config)).getAsJsonObject();
            Files.copy(config.toPath(),
                    new File(config.getParentFile(), "quickskin-client.json.bak").toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            edit.apply(json);
            Files.write(config.toPath(), json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) {
            Log.error("QuickSkin: конфиг не обновлён — " + error.getMessage());
        }
    }

    private static boolean isImage(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".gif");
    }

    private static byte[] bytes(File file) throws Exception {
        return Files.readAllBytes(file.toPath());
    }

    private static String fileName(String original, boolean cape) {
        String name = original == null ? (cape ? "cape.png" : "skin.png") : original.toLowerCase(Locale.ROOT);
        name = name.replaceAll("[^a-z0-9._-]+", "-");
        if (!name.endsWith(".png") && !name.endsWith(".gif")) name = name + ".png";
        if (name.length() < 5) name = cape ? "cape.png" : "skin.png";
        return name;
    }

    /** ContentId мода: sha256 файла, для плащей — с доменом quickskin:cape. */
    private static String contentId(byte[] data, boolean cape) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        if (cape) digest.update(CAPE_DOMAIN);
        byte[] hash = digest.digest(data);
        StringBuilder text = new StringBuilder(hash.length * 2);
        for (byte value : hash) text.append(String.format("%02x", value));
        return text.toString();
    }
}
