package ru.berrytrip.berryup;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Вход через Telegram. Лаунчер формирует ссылку на бота с ником и одноразовым
 * nonce, кладёт nonce в сборку и открывает бота. Бот выдаёт код, игрок вводит
 * /verify в игре — мод сохраняет токен. Токен привязан к нику и Telegram.
 */
public final class Auth {

    public static final int TOKEN_NONE = 0;
    public static final int TOKEN_OK = 1;
    public static final int TOKEN_EXPIRED = 2;
    public static final int TOKEN_OTHER_NICK = 3;
    public static final int TOKEN_BROKEN = 4;

    /** Ссылка живёт 30 минут: столько же, сколько код бота. */
    public static final long LINK_TTL_MS = 30 * 60_000L;

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private Auth() {
    }

    public static String nick() {
        return Store.get("auth.nick", "");
    }

    public static String link(String nick, String nonce) {
        String clean = nick == null ? "" : nick.trim();
        if (clean.isEmpty()) {
            return Cfg.BOT_URL;
        }
        String payload = nonce == null || nonce.isEmpty() ? clean : clean + "-" + nonce;
        try {
            return Cfg.BOT_URL + "?start=" + URLEncoder.encode(payload, "UTF-8");
        } catch (Exception error) {
            return Cfg.BOT_URL;
        }
    }

    /**
     * Открывает бота со ссылкой на ник. Возвращает текст ошибки или "".
     */
    public static String start(Prism prism, String nick) {
        String clean = nick == null ? "" : nick.trim();
        if (!clean.matches("[A-Za-z0-9_]{3,16}")) {
            return "Ник: 3–16 символов, латиница, цифры и _.";
        }
        String account = prism == null ? "" : prism.accountName();
        if (!account.isEmpty() && !account.equalsIgnoreCase(clean)) {
            return "В Prism выбран аккаунт " + account + " — зайти можно только под ним.";
        }
        if (prism == null || !prism.hasInstance()) {
            return "Сначала установи сборку BerryTrip в Prism.";
        }
        String nonce = nonce();
        Store.set("auth.nick", clean);
        Store.set("auth.nonce", nonce);
        Store.set("auth.linkAt", String.valueOf(System.currentTimeMillis()));
        Store.save();
        if (!writeLink(prism, clean, nonce)) {
            return "Не смог записать ссылку в сборку — проверь папку instances в Prism.";
        }
        Sys.openUrl(link(clean, nonce));
        return "";
    }

    /** Токен есть и ещё действует — сервер пустит без кода. */
    public static boolean signedIn(Prism prism) {
        return tokenState(prism) == TOKEN_OK;
    }

    /** Состояние сохранённого токена: TOKEN_NONE / OK / EXPIRED / OTHER_NICK / BROKEN. */
    public static int tokenState(Prism prism) {
        File file = tokenFile(prism);
        if (file == null || !file.isFile() || file.length() == 0) {
            return TOKEN_NONE;
        }
        try {
            JsonObject json = JsonParser.parseString(Sys.read(file)).getAsJsonObject();
            String token = json.has("token") ? json.get("token").getAsString() : "";
            int dot = token.indexOf('.');
            if (dot <= 0) {
                return TOKEN_BROKEN;
            }
            byte[] raw = Base64.getUrlDecoder().decode(token.substring(0, dot));
            JsonObject payload = JsonParser.parseString(
                    new String(raw, StandardCharsets.UTF_8)).getAsJsonObject();
            String tokenNick = payload.has("nick") ? payload.get("nick").getAsString() : "";
            long expires = payload.has("exp") ? payload.get("exp").getAsLong() : 0L;
            if (tokenNick.isEmpty() || expires <= 0) {
                return TOKEN_BROKEN;
            }
            if (expires * 1000L < System.currentTimeMillis()) {
                return TOKEN_EXPIRED;
            }
            String current = prism == null ? "" : prism.accountName();
            if (current.isEmpty()) {
                current = nick();
            }
            if (!current.isEmpty() && !tokenNick.equalsIgnoreCase(current)) {
                return TOKEN_OTHER_NICK;
            }
            return TOKEN_OK;
        } catch (Exception error) {
            return TOKEN_BROKEN;
        }
    }

    public static String tokenNick(Prism prism) {
        try {
            File file = tokenFile(prism);
            if (file == null || !file.isFile()) return "";
            JsonObject json = JsonParser.parseString(Sys.read(file)).getAsJsonObject();
            return json.has("nick") ? json.get("nick").getAsString() : "";
        } catch (Exception error) {
            return "";
        }
    }

    public static void logout(Prism prism) {
        Store.set("auth.nick", "");
        Store.set("auth.nonce", "");
        Store.save();
        File file = tokenFile(prism);
        try {
            if (file != null && file.isFile()) Files.delete(file.toPath());
        } catch (Exception ignored) {
        }
        File link = linkFile(prism);
        try {
            if (link != null && link.isFile()) Files.delete(link.toPath());
        } catch (Exception ignored) {
        }
    }

    public static File tokenFile(Prism prism) {
        if (prism == null || !prism.hasInstance()) return null;
        return new File(new File(prism.minecraftDir(), "config"), "berryauth-token.json");
    }

    public static File linkFile(Prism prism) {
        if (prism == null || !prism.hasInstance()) return null;
        return new File(new File(prism.minecraftDir(), "config"), "berryauth-link.json");
    }

    private static boolean writeLink(Prism prism, String nick, String nonce) {
        File file = linkFile(prism);
        if (file == null) {
            return false;
        }
        try {
            if (!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs()) {
                return false;
            }
            JsonObject json = new JsonObject();
            json.addProperty("nick", nick);
            json.addProperty("nonce", nonce);
            json.addProperty("until", System.currentTimeMillis() + LINK_TTL_MS);
            Files.write(file.toPath(), json.toString().getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception error) {
            Log.error("Не удалось записать ссылку входа: " + error.getMessage());
            return false;
        }
    }

    private static String nonce() {
        StringBuilder builder = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
