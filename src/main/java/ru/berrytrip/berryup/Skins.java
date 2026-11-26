package ru.berrytrip.berryup;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Скины и плащи QuickSkin.
 *
 * <p>QuickSkin 5 хранит библиотеку в {@code %APPDATA%\QuickSkin}: выбранный скин
 * лежит в {@code skins}, плащи — в {@code capes}, а какой именно выбран — написано
 * в {@code config/quickskin-client.json} как активный ContentId (sha256 файла).
 * Лаунчер читает именно эти хэши, поэтому в превью показывается тот скин и тот
 * плащ, которые сейчас стоят в игре, а не последний файл в папке.</p>
 *
 * <p>Старые версии мода держали файлы внутри инстанса
 * ({@code quickskin/uploads/skins|capes}). Такой пул тоже читается, а установка
 * идёт в библиотеку, если она есть, иначе — в старый пул.</p>
 *
 * <p>Встроенные плащи ({@code known:old}, {@code known:twisted} и остальные)
 * не лежат на диске: они внутри jar мода, откуда лаунчер их и достаёт.</p>
 */
public final class Skins {

    /** Ограничение LocalAssetManager в QuickSkin. */
    public static final int MAX_BYTES = 33554432;
    public static final int MIN_SIZE = 64;
    public static final int MAX_SIZE = 2048;

    private static final String NONE_CAPE = "known:__NONE__";
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

    // ------------------------------------------------------------- пути

    /** Библиотека QuickSkin 5+: %APPDATA%\QuickSkin. */
    public static File libraryDir() {
        return new File(Cfg.ROAMING, "QuickSkin");
    }

    public static File librarySkins() {
        return new File(libraryDir(), "skins");
    }

    public static File libraryCapes() {
        return new File(libraryDir(), "capes");
    }

    /** Старый пул мода внутри инстанса. */
    public static File skinDir(Prism prism) {
        return prism == null ? null : new File(prism.minecraftDir(), "quickskin/uploads/skins");
    }

    public static File capeDir(Prism prism) {
        return prism == null ? null : new File(prism.minecraftDir(), "quickskin/uploads/capes");
    }

    private static File clientConfig(Prism prism) {
        return prism == null ? null : new File(prism.minecraftDir(), "config/quickskin-client.json");
    }

    private static File preferencesFile() {
        return new File(libraryDir(), "skin-preferences.json");
    }

    // ------------------------------------------------------------- активный вид

    /** Активный ContentId из quickskin-client.json: sha256-… или known:…. */
    public static String activeHash(Prism prism, boolean cape) {
        File config = clientConfig(prism);
        if (config == null || !config.isFile()) return null;
        try {
            JsonObject json = JsonParser.parseString(Sys.read(config)).getAsJsonObject();
            String key = cape ? "activeCapeHash" : "activeSkinHash";
            if (!json.has(key) || json.get(key).isJsonNull()) return null;
            String value = json.get(key).getAsString().trim();
            return value.isEmpty() ? null : value;
        } catch (Exception error) {
            return null;
        }
    }

    /** Последний скин из пула QuickSkin — запасной вариант, если хэш не нашёлся. */
    public static BufferedImage current(Prism prism) {
        Asset asset = currentAsset(prism, false);
        return asset == null ? null : asset.image;
    }

    public static BufferedImage currentCape(Prism prism) {
        Asset asset = currentAsset(prism, true);
        return asset == null ? null : asset.image;
    }

    /** activeModelType из QuickSkin: auto, classic или slim. */
    public static String model(Prism prism) {
        File config = clientConfig(prism);
        if (config == null || !config.isFile()) return "auto";
        try {
            JsonObject json = JsonParser.parseString(Sys.read(config)).getAsJsonObject();
            return normalizeModel(json.has("activeModelType")
                    ? json.get("activeModelType").getAsString() : "auto");
        } catch (Exception error) {
            return "auto";
        }
    }

    public static void setModel(Prism prism, String value) {
        String model = normalizeModel(value);
        updateConfig(prism, json -> json.addProperty("activeModelType", model));
        updatePreference(activeHash(prism, false), model, null);
    }

    public static String normalizeModel(String value) {
        String model = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return model.equals("classic") || model.equals("slim") ? model : "auto";
    }

    /**
     * Скин или плащ, который сейчас выбран в игре. Сначала ищем файл по активному
     * ContentId, встроенный плащ берём из jar мода, и только если ничего не нашли —
     * показываем самый свежий файл из пула.
     */
    public static Asset currentAsset(Prism prism, boolean cape) {
        if (prism == null || !prism.hasInstance()) return null;
        String hash = activeHash(prism, cape);
        if (hash == null) return newest(prism, cape);
        if (hash.startsWith("sha256-")) return findByHash(prism, hash, cape);
        if (cape && hash.startsWith("known:")) {
            return builtinCape(prism, hash.substring("known:".length()));
        }
        return null;
    }

    /** Подпись активного плаща для интерфейса: old, twisted… или пусто. */
    public static String activeCapeLabel(Prism prism) {
        String hash = activeHash(prism, true);
        if (hash == null || hash.equals(NONE_CAPE)) return "";
        if (hash.startsWith("known:")) {
            String id = hash.substring("known:".length());
            return NONE_CAPE.equals(hash) || id.isEmpty() ? "" : id;
        }
        return "свой файл";
    }

    // ------------------------------------------------------------- поиск по хэшу

    private static Asset findByHash(Prism prism, String hash, boolean cape) {
        String wanted = hash.substring("sha256-".length()).trim().toLowerCase(Locale.ROOT);
        if (wanted.isEmpty()) return null;
        for (File dir : searchDirs(prism, cape)) {
            Asset found = findByHash(dir, wanted, cape);
            if (found != null) return found;
        }
        return null;
    }

    private static Asset findByHash(File dir, String wanted, boolean cape) {
        if (dir == null || !dir.isDirectory()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (!file.isFile() || !isImage(file)) continue;
            try {
                if (!contentId(file, cape).equals(wanted)) continue;
                return read(file, cape);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static List<File> searchDirs(Prism prism, boolean cape) {
        List<File> dirs = new ArrayList<File>(4);
        dirs.add(cape ? libraryCapes() : librarySkins());
        dirs.add(cape ? capeDir(prism) : skinDir(prism));
        return dirs;
    }

    private static Asset newest(Prism prism, boolean cape) {
        List<File> dirs = searchDirs(prism, cape);
        File newest = null;
        for (File dir : dirs) {
            File[] files = dir == null ? null : dir.listFiles();
            if (files == null) continue;
            for (File file : files) {
                if (!file.isFile() || !isImage(file)) continue;
                if (newest == null || file.lastModified() > newest.lastModified()) newest = file;
            }
        }
        if (newest == null) return null;
        try {
            return read(newest, cape);
        } catch (Exception error) {
            return null;
        }
    }

    /** Встроенный плащ из assets/quickskin/textures/capes/<id>.png внутри jar мода. */
    private static Asset builtinCape(Prism prism, String id) {
        if (id == null || id.isEmpty() || "__NONE__".equals(id)) return null;
        if (!id.matches("[a-z0-9_]+")) return null;
        File jar = quickskinJar(prism);
        if (jar == null) return null;
        try (ZipFile zip = new ZipFile(jar)) {
            ZipEntry entry = zip.getEntry("assets/quickskin/textures/capes/" + id + ".png");
            if (entry == null) return null;
            BufferedImage image;
            try (InputStream stream = zip.getInputStream(entry)) {
                image = ImageIO.read(stream);
            }
            if (image == null) return null;
            return new Asset(jar, image, image.getWidth(), image.getHeight(), false, true);
        } catch (Exception error) {
            Log.error("QuickSkin: встроенный плащ " + id + " не прочитан — " + error.getMessage());
            return null;
        }
    }

    private static File quickskinJar(Prism prism) {
        List<File> dirs = new ArrayList<File>(2);
        dirs.add(prism.modsDir());
        File off = Tweaks.offDir(prism);
        if (off != null) dirs.add(off);
        for (File dir : dirs) {
            File[] files = dir == null ? null : dir.listFiles();
            if (files == null) continue;
            for (File file : files) {
                if (!file.isFile()) continue;
                String name = file.getName().toLowerCase(Locale.ROOT);
                if (!name.endsWith(".jar")) continue;
                if (name.contains("quick") && name.contains("skin")) return file;
            }
        }
        return null;
    }

    // ------------------------------------------------------------- проверка файлов

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

    // ------------------------------------------------------------- установка

    /**
     * Кладёт скин в библиотеку QuickSkin и делает его активным в игре.
     * Возвращает имя файла в папке скинов.
     */
    public static String applySkin(Prism prism, File source, String nick, String model) throws Exception {
        Asset asset = readSkin(source);
        File target = copy(prism, asset, false);
        String hash = "sha256-" + contentId(target, false);
        String normalized = normalizeModel(model);
        updateSkinConfig(prism, hash, nick, normalized);
        updatePreference(hash, normalized, null);
        return target.getName();
    }

    /**
     * Кладёт плащ в библиотеку QuickSkin и включает его в конфиге.
     * Возвращает имя файла в папке плащей.
     */
    public static String applyCape(Prism prism, File source) throws Exception {
        Asset asset = readCape(source);
        File target = copy(prism, asset, true);
        String hash = "sha256-" + contentId(target, true);
        updateCapeConfig(prism, hash);
        updatePreference(activeHash(prism, false), null, hash);
        return target.getName();
    }

    /** Убирает активный плащ так же, как это делает меню QuickSkin. */
    public static void clearCape(Prism prism) {
        updateCapeConfig(prism, NONE_CAPE);
        updatePreference(activeHash(prism, false), null, NONE_CAPE);
    }

    private static File copy(Prism prism, Asset asset, boolean cape) throws Exception {
        if (prism == null || !prism.hasInstance()) throw new Exception("сборка ещё не установлена");
        // QuickSkin 5+ читает общую библиотеку в %APPDATA%\QuickSkin.
        // Если её ещё нет (старая версия мода), кладём файл в пул инстанса.
        File dir = libraryDir().isDirectory()
                ? (cape ? libraryCapes() : librarySkins())
                : (cape ? capeDir(prism) : skinDir(prism));
        if (dir == null) throw new Exception("не удалось найти папку QuickSkin");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new Exception("не удалось создать " + dir.getAbsolutePath());
        }
        File target = new File(dir, fileName(asset.file.getName(), cape));
        if (target.isFile() && !contentId(target, cape).equals(contentId(asset.file, cape))) {
            // Файл с тем же именем, но другим содержимым не должен затирать чужой скин.
            target = new File(dir, uniqueName(fileName(asset.file.getName(), cape), dir));
        }
        Files.copy(asset.file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private static String uniqueName(String name, File dir) {
        String base = name.endsWith(".gif") ? name.substring(0, name.length() - 4) : name.substring(0, name.length() - 4);
        String ext = name.substring(name.length() - 4);
        for (int i = 2; i < 1000; i++) {
            File candidate = new File(dir, base + "-" + i + ext);
            if (!candidate.exists()) return candidate.getName();
        }
        return name;
    }

    private static void updateSkinConfig(Prism prism, String hash, String nick, String model) {
        updateConfig(prism, json -> {
            json.addProperty("activeSkinHash", hash);
            json.addProperty("playerOwnSkinHash", hash);
            json.addProperty("playerOwnSkinHidden", false);
            json.addProperty("activeModelType", model);
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
        File config = clientConfig(prism);
        if (config == null) return;
        try {
            JsonObject json = config.isFile()
                    ? JsonParser.parseString(Sys.read(config)).getAsJsonObject() : new JsonObject();
            if (config.isFile()) {
                Files.copy(config.toPath(),
                        new File(config.getParentFile(), "quickskin-client.json.bak").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } else if (!config.getParentFile().isDirectory() && !config.getParentFile().mkdirs()) {
                return;
            }
            edit.apply(json);
            Files.write(config.toPath(), json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) {
            Log.error("QuickSkin: конфиг не обновлён — " + error.getMessage());
        }
    }

    /**
     * skin-preferences.json помнит модель и плащ для каждого скина.
     * Без этой записи меню мода может вернуть старому скину старый плащ.
     */
    private static void updatePreference(String hash, String model, String cape) {
        if (hash == null || !hash.startsWith("sha256-")) return;
        File file = preferencesFile();
        try {
            JsonObject root = new JsonObject();
            if (file.isFile()) {
                try {
                    root = JsonParser.parseString(Sys.read(file)).getAsJsonObject();
                } catch (Exception ignored) {
                }
            }
            JsonObject preferences = root.has("preferences")
                    ? root.getAsJsonObject("preferences") : new JsonObject();
            JsonObject entry = preferences.has(hash)
                    ? preferences.getAsJsonObject(hash) : new JsonObject();
            if (model != null && !model.isEmpty()) {
                entry.addProperty("modelType", "slim".equals(model) ? "slim" : "classic");
            }
            if (!entry.has("favorite")) entry.addProperty("favorite", false);
            if (cape != null) entry.addProperty("cape", cape);
            preferences.add(hash, entry);
            root.add("preferences", preferences);
            if (!libraryDir().isDirectory() && !libraryDir().mkdirs()) return;
            String text = new GsonBuilder().setPrettyPrinting().create().toJson(root);
            Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) {
            Log.error("QuickSkin: skin-preferences.json не обновлён — " + error.getMessage());
        }
    }

    // ------------------------------------------------------------- мелочи

    private static boolean isImage(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".gif");
    }

    private static String fileName(String original, boolean cape) {
        String name = original == null ? (cape ? "cape.png" : "skin.png") : original.toLowerCase(Locale.ROOT);
        name = name.replaceAll("[^a-z0-9._-]+", "-");
        if (!name.endsWith(".png") && !name.endsWith(".gif")) name = name + ".png";
        if (name.length() < 5) name = cape ? "cape.png" : "skin.png";
        return name;
    }

    /** ContentId мода: sha256 файла, для плащей — с доменом quickskin:cape. */
    private static String contentId(File file, boolean cape) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        if (cape) digest.update(CAPE_DOMAIN);
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[65536];
            int read;
            while ((read = input.read(buffer)) > 0) digest.update(buffer, 0, read);
        }
        return hex(digest.digest());
    }

    private static String hex(byte[] bytes) {
        StringBuilder text = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) text.append(String.format("%02x", value));
        return text.toString();
    }
}
