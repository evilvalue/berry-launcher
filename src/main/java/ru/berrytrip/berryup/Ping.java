package ru.berrytrip.berryup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Пинг Minecraft-сервера по протоколу status.
 *
 * <p>Важно: в игре показывается задержка keep-alive на уже открытом
 * соединении, то есть примерно один TCP round-trip. Раньше лаунчер считал
 * DNS + установку TCP + обработку status на сервере, из-за чего цифра была
 * в разы больше игры, особенно на первом запросе. Здесь ping — это время
 * TCP-подключения; первый «холодный» коннект прогревается отдельным
 * коротким запросом, а статус (MOTD/онлайн) читается уже на втором.</p>
 */
public final class Ping {

    private static final Pattern MOTD_CODES = Pattern.compile("\u00a7[0-9a-fk-orA-FK-OR]");

    public static final class Status {
        public boolean ok;
        public String error = "";
        public int online = -1;
        public int max = -1;
        public int ping = -1;
        public int connect = -1;
        public int statusMs = -1;
        public int dns = -1;
        public String motd = "";
        public String version = "";

        public String players() {
            if (!ok || online < 0) return "нет данных";
            return max >= 0 ? online + "/" + max : String.valueOf(online);
        }
    }

    private static final class Probe {
        boolean ok;
        String error = "";
        int connect = -1;
        int statusMs = -1;
        int online = -1;
        int max = -1;
        String motd = "";
        String version = "";
    }

    private Ping() {
    }

    public static Status query(String host, int port, int timeoutMs) {
        Status out = new Status();
        long started = System.nanoTime();
        InetAddress address;
        try {
            address = InetAddress.getByName(host);
            out.dns = millis(System.nanoTime() - started);
        } catch (Exception error) {
            out.error = "не удалось определить адрес сервера";
            return out;
        }

        Probe first = probe(host, port, address, timeoutMs);
        if (!first.ok) {
            copy(out, first);
            return out;
        }

        // Первый TCP-коннект на Windows часто «холодный» (около секунды),
        // хотя следующие идут за нормальный RTT. Второй замер и показываем.
        Probe measured = first;
        if (first.connect >= 350) {
            Probe second = probe(host, port, address, timeoutMs);
            if (second.ok) {
                measured = second;
            }
        }
        copy(out, measured);
        out.ping = first.connect > 0 && measured.connect > 0
                ? Math.min(first.connect, measured.connect)
                : measured.connect;
        out.connect = measured.connect;
        out.statusMs = measured.statusMs;
        return out;
    }

    private static Probe probe(String host, int port, InetAddress address, int timeoutMs) {
        Probe probe = new Probe();
        Socket socket = new Socket();
        try {
            socket.setTcpNoDelay(true);
            long connectStarted = System.nanoTime();
            socket.connect(new InetSocketAddress(address, port), timeoutMs);
            probe.connect = millis(System.nanoTime() - connectStarted);
            socket.setSoTimeout(timeoutMs);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            ByteArrayOutputStream handshake = new ByteArrayOutputStream();
            DataOutputStream packet = new DataOutputStream(handshake);
            writeVarInt(packet, 0x00);
            writeVarInt(packet, Cfg.PROTOCOL);
            writeString(packet, host);
            packet.writeShort(port);
            writeVarInt(packet, 1);
            writePacket(out, handshake.toByteArray());

            long statusStarted = System.nanoTime();
            writePacket(out, new byte[]{0x00});
            out.flush();

            DataInputStream in = new DataInputStream(socket.getInputStream());
            int length = readVarInt(in);
            if (length <= 0 || length > 512 * 1024) {
                probe.error = "неожиданный размер ответа";
                return probe;
            }
            byte[] payload = new byte[length];
            in.readFully(payload);
            probe.statusMs = millis(System.nanoTime() - statusStarted);
            DataInputStream body = new DataInputStream(new ByteArrayInputStream(payload));
            int id = readVarInt(body);
            if (id != 0) {
                probe.error = "сервер ответил другим пакетом";
                return probe;
            }
            int textLength = readVarInt(body);
            if (textLength < 0 || textLength > 512 * 1024) {
                probe.error = "неверная строка в ответе";
                return probe;
            }
            byte[] text = new byte[textLength];
            body.readFully(text);
            parse(new String(text, StandardCharsets.UTF_8), probe);
            probe.ok = true;
        } catch (Exception error) {
            probe.ok = false;
            probe.error = error instanceof java.net.SocketTimeoutException
                    ? "сервер не ответил за " + (timeoutMs / 1000) + " с"
                    : "нет связи с сервером";
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
        return probe;
    }

    private static void copy(Status out, Probe probe) {
        out.ok = probe.ok;
        out.error = probe.error;
        out.online = probe.online;
        out.max = probe.max;
        out.motd = probe.motd;
        out.version = probe.version;
    }

    private static void parse(String json, Probe probe) {
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            JsonObject players = root.has("players") ? root.getAsJsonObject("players") : null;
            if (players != null) {
                if (players.has("online")) probe.online = players.get("online").getAsInt();
                if (players.has("max")) probe.max = players.get("max").getAsInt();
            }
            if (root.has("version")) {
                JsonObject version = root.getAsJsonObject("version");
                if (version != null && version.has("name")) probe.version = version.get("name").getAsString();
            }
            if (root.has("description")) probe.motd = clean(text(root.get("description")));
        } catch (Exception error) {
            probe.error = "не удалось разобрать ответ сервера";
        }
    }

    /** description бывает строкой, объектом {text,extra[]} или массивом компонентов. */
    private static String text(JsonElement element) {
        if (element == null || element.isJsonNull()) return "";
        if (element.isJsonPrimitive()) return element.getAsString();
        if (element.isJsonArray()) {
            StringBuilder builder = new StringBuilder();
            JsonArray array = element.getAsJsonArray();
            for (JsonElement part : array) builder.append(text(part));
            return builder.toString();
        }
        StringBuilder builder = new StringBuilder();
        JsonObject object = element.getAsJsonObject();
        if (object.has("text")) builder.append(text(object.get("text")));
        if (object.has("extra")) builder.append(text(object.get("extra")));
        return builder.toString();
    }

    private static String clean(String value) {
        if (value == null) return "";
        String text = MOTD_CODES.matcher(value).replaceAll("").replace('\r', ' ').replace('\n', ' ').trim();
        return text;
    }

    private static int millis(long nanos) {
        return (int) Math.max(0, Math.round(nanos / 1_000_000.0));
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        int rest = value;
        do {
            int current = rest & 0x7F;
            rest >>>= 7;
            if (rest != 0) current |= 0x80;
            out.writeByte(current);
        } while (rest != 0);
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static void writePacket(DataOutputStream out, byte[] payload) throws IOException {
        ByteArrayOutputStream head = new ByteArrayOutputStream();
        DataOutputStream headOut = new DataOutputStream(head);
        writeVarInt(headOut, payload.length);
        out.write(head.toByteArray());
        out.write(payload);
    }

    private static int readVarInt(InputStream in) throws IOException {
        int result = 0;
        int shift = 0;
        while (true) {
            int read = in.read();
            if (read < 0) throw new IOException("соединение закрыто");
            result |= (read & 0x7F) << shift;
            if ((read & 0x80) == 0) return result;
            shift += 7;
            if (shift > 35) throw new IOException("слишком длинный varint");
        }
    }
}
