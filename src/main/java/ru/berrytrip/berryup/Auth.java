package ru.berrytrip.berryup;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

/**
 * Вход через Telegram без кодов и без nonce в лаунчере.
 *
 * <p>Лаунчер отправляет серверу только ник. Сервер (мод berryauth) создаёт
 * одноразовую ссылку на бота, бот по Start сразу привязывает ник и добавляет
 * его в вайтлист, а лаунчер опрашивает статус и забирает подписанный токен.
 * Токен кладётся в config/berryauth-token.json — его читает клиентский мод.</p>
 */
public final class Auth {

    public static final int TOKEN_NONE = 0;
    public static final int TOKEN_OK = 1;
    public static final int TOKEN_EXPIRED = 2;
    public static final int TOKEN_OTHER_NICK = 3;
    public static final int TOKEN_BROKEN = 4;

    public static final int POLL_WAITING = 0;
    public static final int POLL_OK = 1;
    public static final int POLL_ERROR = 2;
    public static final int POLL_EXPIRED = 3;
    public static final int POLL_NONE = 4;

    private Auth() {
    }

    public static String nick() {
        return Store.get("auth.nick", "");
    }

    /** Ссылка, которую уже выдал сервер (её можно открыть повторно). */
    public static String pendingLink() {
        return Store.get("auth.request.link", "");
    }

    public static boolean pending() {
        return !Store.get("auth.request.id", "").isEmpty()
                && System.currentTimeMillis() < pendingUntil();
    }

    public static String pendingNick() {
        return Store.get("auth.request.nick", "");
    }

    private static long pendingUntil() {
        try {
            return Long.parseLong(Store.get("auth.request.until", "0"));
        } catch (Exception error) {
            return 0L;
        }
    }

    /**
     * Просит сервер создать ссылку. Вызывать в фоновом потоке: здесь сеть.
     * Возвращает текст ошибки или "".
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

        JsonObject body = new JsonObject();
        body.addProperty("nick", clean);
        try {
            Http.Result result = Http.call("POST", api("/auth/link"), body.toString());
            JsonObject json = parse(result.body);
            if (result.code != 200 || json == null || !json.has("ok")
                    || !json.get("ok").getAsBoolean()) {
                return message(json, "Сервер входа ответил ошибкой " + result.code + ".");
            }
            String id = text(json, "id");
            String secret = text(json, "secret");
            String link = text(json, "link");
            long expires = json.has("expires") ? json.get("expires").getAsLong() : 0L;
            if (id.isEmpty() || secret.isEmpty() || !link.startsWith("https://t.me/")
                    || expires <= System.currentTimeMillis()) {
                return "Сервер выдал некорректную ссылку. Попробуй ещё раз.";
            }
            Store.set("auth.nick", clean);
            Store.set("auth.request.id", id);
            Store.set("auth.request.secret", secret);
            Store.set("auth.request.nick", clean);
            Store.set("auth.request.link", link);
            Store.set("auth.request.until", String.valueOf(expires));
            Store.save();
            Sys.openUrl(link);
            Log.info("Сервер выдал ссылку входа для " + clean);
            return "";
        } catch (Exception error) {
            Log.error("Не удалось получить ссылку входа: " + error.getMessage());
            return "Сервер входа недоступен: " + shortError(error)
                    + ". Проверь сеть или адрес auth.api в config.ini.";
        }
    }

    /** Повторно открыть уже выданную ссылку, не создавая новую. */
    public static boolean openPending() {
        String link = pendingLink();
        if (link.isEmpty() || !pending()) {
            return false;
        }
        Sys.openUrl(link);
        return true;
    }

    /** Опрос сервера: вызывать в фоновом потоке. */
    public static Poll poll(Prism prism) {
        String id = Store.get("auth.request.id", "");
        String secret = Store.get("auth.request.secret", "");
        if (id.isEmpty() || secret.isEmpty()) {
            return new Poll(POLL_NONE, "", "");
        }
        if (System.currentTimeMillis() > pendingUntil()) {
            clearPending();
            return new Poll(POLL_EXPIRED, "Ссылка истекла — начни вход заново.", "");
        }
        try {
            JsonObject body = new JsonObject();
            body.addProperty("id", id);
            body.addProperty("secret", secret);
            Http.Result result = Http.call("POST", api("/auth/status"), body.toString());
            JsonObject json = parse(result.body);
            if (result.code == 404) {
                clearPending();
                return new Poll(POLL_ERROR, message(json,
                        "Ссылка не найдена. Начни вход заново."), "");
            }
            if (result.code != 200 || json == null) {
                return new Poll(POLL_WAITING, "", "");
            }
            String status = text(json, "status");
            String nick = text(json, "nick");
            if ("waiting".equals(status)) {
                return new Poll(POLL_WAITING, "", nick);
            }
            if ("verified".equals(status)) {
                String token = text(json, "token");
                if (token.isEmpty()) {
                    return new Poll(POLL_WAITING, "", nick);
                }
                if (!writeToken(prism, nick, token)) {
                    return new Poll(POLL_ERROR, "Не смог записать токен в сборку.", nick);
                }
                clearPending();
                Log.ok("Вход подтверждён: " + nick);
                return new Poll(POLL_OK, "", nick);
            }
            if ("expired".equals(status)) {
                clearPending();
                return new Poll(POLL_EXPIRED, message(json,
                        "Ссылка истекла — начни вход заново."), nick);
            }
            String reason = message(json, "Вход отклонён.");
            clearPending();
            return new Poll(POLL_ERROR, reason, nick);
        } catch (Exception error) {
            // сеть моргнула — оставляем ссылку живой и попробуем снова
            return new Poll(POLL_WAITING, "", pendingNick());
        }
    }

    public static void cancel() {
        clearPending();
    }

    /** Токен есть и ещё действует — сервер пустит без Telegram. */
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
        clearPending();
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

    /** Старый файл ссылки: новые версии его не пишут, но чистим при входе. */
    public static File linkFile(Prism prism) {
        if (prism == null || !prism.hasInstance()) return null;
        return new File(new File(prism.minecraftDir(), "config"), "berryauth-link.json");
    }

    private static boolean writeToken(Prism prism, String nick, String token) {
        File file = tokenFile(prism);
        if (file == null) {
            return false;
        }
        try {
            File parent = file.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) {
                return false;
            }
            JsonObject json = new JsonObject();
            json.addProperty("nick", nick);
            json.addProperty("token", token);
            File temp = new File(parent, "berryauth-token.json.tmp");
            Files.write(temp.toPath(), json.toString().getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception atomic) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            File legacy = linkFile(prism);
            if (legacy != null && legacy.isFile()) {
                Files.deleteIfExists(legacy.toPath());
            }
            return true;
        } catch (Exception error) {
            Log.error("Не удалось сохранить токен входа: " + error.getMessage());
            return false;
        }
    }

    private static void clearPending() {
        Store.set("auth.request.id", "");
        Store.set("auth.request.secret", "");
        Store.set("auth.request.nick", "");
        Store.set("auth.request.link", "");
        Store.set("auth.request.until", "0");
        Store.save();
    }

    private static String api(String path) {
        String base = Store.get("auth.api", Cfg.AUTH_API).trim();
        if (base.isEmpty()) base = Cfg.AUTH_API;
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + path;
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception error) {
            return value;
        }
    }

    private static JsonObject parse(String text) {
        try {
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (Exception error) {
            return null;
        }
    }

    private static String text(JsonObject json, String key) {
        try {
            return json != null && json.has(key) ? json.get(key).getAsString() : "";
        } catch (Exception error) {
            return "";
        }
    }

    private static String message(JsonObject json, String fallback) {
        String value = text(json, "error");
        if (value.isEmpty()) value = text(json, "message");
        return value.isEmpty() ? fallback : value;
    }

    private static String shortError(Exception error) {
        String text = error.getMessage();
        if (text == null || text.isEmpty()) return error.getClass().getSimpleName();
        return text.length() > 120 ? text.substring(0, 120) : text;
    }

    public static final class Poll {
        public final int state;
        public final String message;
        public final String nick;

        Poll(int state, String message, String nick) {
            this.state = state;
            this.message = message == null ? "" : message;
            this.nick = nick == null ? "" : nick;
        }
    }
}
