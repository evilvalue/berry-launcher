package ru.berrytrip.berryup;

import java.io.File;
import java.util.Locale;

/**
 * BerryUp — надстройка над Prism Launcher (и форками вроде PineconeMC):
 * находит лаунчер, следит за сборкой BerryTrip и запускает её.
 * Здесь лежат адреса, версия и пути — всё остальное берёт их отсюда.
 */
public final class Cfg {

    public static final String APP_NAME = "BerryUp";
    public static final String BUILD = "BerryTrip";
    public static final String VERSION = "3.8.0";

    public static final String SITE = "https://evilvalue.github.io/berry/";
    public static final String TELEGRAM = "https://t.me/killberries";
    public static final String BOT_URL = "https://t.me/killberriesbot";
    public static final String REPO = "https://github.com/evilvalue/berry-launcher";
    public static final String RELEASES = "https://github.com/evilvalue/berry-launcher/releases/latest";
    public static final String UPDATE_URL = "https://evilvalue.github.io/berry/launcher/latest-java.json";
    public static final String JAR_URL = "https://evilvalue.github.io/berry/launcher/BerryUp.jar";
    public static final String PACK_URL = "https://evilvalue.github.io/berry/pack.toml";
    public static final String INSTANCE_URL = "https://evilvalue.github.io/berry/launcher/instance.zip";
    public static final String PRISM_URL = "https://prismlauncher.org/download/windows/";
    public static final String PINE_URL = "https://pineconemc.com/download/";

    public static final String SERVER_HOST = "135.181.126.167";
    public static final int SERVER_PORT = 25737;
    public static final int PROTOCOL = 767;
    public static final String MC_VERSION = "1.21.1";
    public static final String LOADER = "NeoForge 21.1.267";

    public static final String USER_AGENT =
            APP_NAME + "/" + VERSION + " (Windows; Minecraft " + MC_VERSION + ")";

    /** Три входа на один сервер: названия и адреса повторяют servers.dat инстанса. */
    public static final Server[] SERVERS = new Server[]{
            new Server("Москва", "lokingalyelil.mclan.ru", 25565),
            new Server("Anycast", "candragiwinho.okak.skin", 25565),
            new Server("Прямое подключение", "135.181.126.167", 25737)
    };

    public static final File HOME = new File(System.getProperty("user.home", "."));
    public static final File ROAMING = envDir("APPDATA", new File(HOME, "AppData/Roaming"));
    public static final File LOCAL = envDir("LOCALAPPDATA", new File(HOME, "AppData/Local"));
    public static final File PROGRAM_FILES = envDir("ProgramFiles", new File("C:/Program Files"));
    public static final File PROGRAM_FILES_X86 = envDir("ProgramFilesX86", new File("C:/Program Files (x86)"));

    private Cfg() {
    }

    /** BERRYUP_CONFIG_DIR позволяет прогнать лаунчер на временных настройках. */
    public static File configDir() {
        String custom = System.getenv("BERRYUP_CONFIG_DIR");
        return custom != null && !custom.trim().isEmpty() ? new File(custom) : new File(ROAMING, "BerryUp");
    }

    public static File configPath() {
        return new File(configDir(), "config.ini");
    }

    public static File tweaksPath() {
        return new File(configDir(), "tweaks.ini");
    }

    public static File logPath() {
        return new File(System.getProperty("java.io.tmpdir", "."), "BerryUp.log");
    }

    /** Адрес, по которому апдейтеры проверяют версию: переопределяется для тестов. */
    public static String updateUrl() {
        String custom = System.getenv("BERRYUP_UPDATE_URL");
        return custom != null && !custom.trim().isEmpty() ? custom : UPDATE_URL;
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static File envDir(String name, File fallback) {
        String value = System.getenv(name);
        return value != null && !value.trim().isEmpty() ? new File(value) : fallback;
    }

    /** Один вход на сервер: имя региона, хост и порт. */
    public static final class Server {
        public final String name;
        public final String host;
        public final int port;

        public Server(String name, String host, int port) {
            this.name = name;
            this.host = host;
            this.port = port;
        }

        public String address() {
            return port == 25565 ? host : host + ":" + port;
        }

        public String endpoint() {
            return host + ":" + port;
        }

        /** Короткая подпись: три названия должны влезть в боковую колонку. */
        public String shortName() {
            String trimmed = name == null ? "" : name.trim();
            if (trimmed.isEmpty()) return "Сервер";
            if (trimmed.length() <= 12) return trimmed;
            int space = trimmed.indexOf(' ');
            return space > 2 ? trimmed.substring(0, space) : trimmed.substring(0, 11) + "…";
        }
    }
}
