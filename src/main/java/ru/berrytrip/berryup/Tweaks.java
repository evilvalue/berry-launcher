package ru.berrytrip.berryup;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Необязательные клиентские моды, ресурспаки и шейдеры для экономии памяти.
 * Выключенные моды уносятся в minecraft/berry-off: NeoForge их не грузит,
 * а перед синхронизацией они возвращаются, иначе packwiz качал бы их заново.
 */
public final class Tweaks {

    public static final String OFF_DIR = "berry-off";

    public static final class Mod {
        public final String key;
        public final String label;
        public final String hint;
        public final boolean def;
        public final String[] prefixes;

        Mod(String key, String label, String hint, boolean def, String... prefixes) {
            this.key = key;
            this.label = label;
            this.hint = hint;
            this.def = def;
            this.prefixes = prefixes;
        }
    }

    public static final Mod[] MODS = new Mod[]{
            new Mod("shouldersurfing", "ShoulderSurfing", "вид от плеча", true, "shouldersurfing"),
            new Mod("lights", "LambDynamicLights", "динамический свет", true, "lambdynamiclights"),
            new Mod("tooltips", "Obscure Tooltips", "подсказки предметов", true, "obscure_tooltips"),
            new Mod("quickskin", "Quick Skin", "скины игроков", true, "quick_skin")
    };

    /** Нужны только ресурспакам: включаются и выключаются вместе с тумблером ресурспаков. */
    public static final String[] PACK_MODS = {"entity_model_features", "entity_texture_features"};

    /** Обязаны быть включены всегда: часть добавляет контент, который знает сервер. */
    public static final String[] LOCKED = {
            "journeymap", "immersive_paintings", "emotecraft", "pop_up_emotes", "notenoughanimations"
    };

    private static Map<String, Boolean> values;
    private static String packsSaved = "";
    private static String packsSavedIncompatible = "";
    private static int memory = 0;

    private Tweaks() {
    }

    private static Map<String, Boolean> values() {
        if (values == null) load();
        return values;
    }

    public static void load() {
        values = new LinkedHashMap<String, Boolean>();
        packsSaved = "";
        packsSavedIncompatible = "";
        File path = Cfg.tweaksPath();
        if (!path.isFile()) return;
        try {
            for (String line : Files.readAllLines(path.toPath(), StandardCharsets.UTF_8)) {
                int index = line.indexOf('=');
                if (index <= 0) continue;
                String key = line.substring(0, index).trim();
                String value = line.substring(index + 1).trim();
                if (key.equalsIgnoreCase("packs_saved")) {
                    packsSaved = value;
                } else if (key.equalsIgnoreCase("packs_saved_incompatible")) {
                    packsSavedIncompatible = value;
                } else if (key.equalsIgnoreCase("memory")) {
                    try {
                        memory = Integer.parseInt(value);
                    } catch (Exception ignored) {
                    }
                } else {
                    values().put(key, "1".equals(value) || "true".equalsIgnoreCase(value));
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            File dir = Cfg.configDir();
            if (!dir.isDirectory() && !dir.mkdirs()) return;
            List<String> lines = new ArrayList<String>();
            lines.add("# BerryUp: настройки экономии памяти (1 = включено)");
            for (Mod mod : MODS) lines.add("mod." + mod.key + "=" + (getMod(mod) ? "1" : "0"));
            lines.add("packs=" + (getBool("packs", true) ? "1" : "0"));
            lines.add("shaders=" + (getBool("shaders", false) ? "1" : "0"));
            lines.add("packs_saved=" + packsSaved);
            lines.add("packs_saved_incompatible=" + packsSavedIncompatible);
            lines.add("memory=" + memory);
            Files.write(Cfg.tweaksPath().toPath(), lines, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    public static boolean getBool(String key, boolean fallback) {
        Boolean value = values().get(key);
        return value != null ? value : fallback;
    }

    public static void setBool(String key, boolean value) {
        values().put(key, value);
    }

    public static boolean getMod(Mod mod) {
        return getBool("mod." + mod.key, mod.def);
    }

    public static void setMod(Mod mod, boolean value) {
        setBool("mod." + mod.key, value);
    }

    public static int memory() {
        values();
        return memory;
    }

    public static void setMemory(int gb) {
        values();
        memory = gb;
    }

    public static String describe(Mod mod) {
        return mod.label + " — " + mod.hint + ". Выключение освобождает память, на сервер не влияет.";
    }

    public static File offDir(Prism prism) {
        return prism == null ? null : new File(prism.minecraftDir(), OFF_DIR);
    }

    public static int offCount(Prism prism) {
        File off = offDir(prism);
        File[] jars = off == null ? null : off.listFiles();
        if (jars == null) return 0;
        int count = 0;
        for (File jar : jars) if (jar.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) count++;
        return count;
    }

    /** Возвращает выключенные моды обратно перед запуском packwiz. */
    public static void restore(Prism prism) {
        File off = offDir(prism);
        File[] jars = off == null ? null : off.listFiles();
        if (jars == null) return;
        for (File jar : jars) {
            if (!jar.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) continue;
            move(jar, new File(prism.modsDir(), jar.getName()));
        }
        deleteIfEmpty(off);
    }

    /** Раскладывает моды по текущим настройкам. Возвращает число перенесённых файлов. */
    public static int syncMods(Prism prism) {
        if (prism == null || !prism.modsDir().isDirectory()) return 0;
        values();
        int changed = 0;
        for (Mod mod : MODS) changed += setEnabled(prism, mod.prefixes, getMod(mod));
        changed += setEnabled(prism, PACK_MODS, getBool("packs", true));
        for (String prefix : LOCKED) changed += setEnabled(prism, new String[]{prefix}, true);
        deleteIfEmpty(offDir(prism));
        return changed;
    }

    private static int setEnabled(Prism prism, String[] prefixes, boolean enabled) {
        int changed = 0;
        File off = offDir(prism);
        if (enabled) {
            File[] jars = off.listFiles();
            if (jars == null) return 0;
            for (File jar : jars) {
                if (!matches(jar.getName(), prefixes)) continue;
                if (move(jar, new File(prism.modsDir(), jar.getName()))) changed++;
            }
        } else {
            File[] jars = prism.modsDir().listFiles();
            if (jars == null) return 0;
            if (!off.isDirectory() && !off.mkdirs()) return 0;
            for (File jar : jars) {
                if (!matches(jar.getName(), prefixes)) continue;
                if (move(jar, new File(off, jar.getName()))) changed++;
            }
        }
        return changed;
    }

    private static boolean matches(String fileName, String[] prefixes) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        for (String prefix : prefixes) if (lower.startsWith(prefix)) return true;
        return false;
    }

    private static boolean move(File from, File to) {
        try {
            if (to.isFile()) Files.deleteIfExists(from.toPath());
            else Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception error) {
            Log.error("Не удалось перенести " + from.getName() + ": " + error.getMessage());
            return false;
        }
    }

    private static void deleteIfEmpty(File dir) {
        File[] left = dir == null ? null : dir.listFiles();
        if (left != null && left.length == 0) {
            try {
                Files.deleteIfExists(dir.toPath());
            } catch (Exception ignored) {
            }
        }
    }

    /** Включает/выключает все ресурспаки в options.txt, запоминая прежний список. */
    public static void applyPacks(Prism prism, boolean enabled) {
        File path = new File(prism.minecraftDir(), "options.txt");
        if (!path.isFile()) return;
        try {
            List<String> lines = new ArrayList<String>(Files.readAllLines(path.toPath(), StandardCharsets.UTF_8));
            int packsIndex = -1;
            int incompatibleIndex = -1;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).toLowerCase(Locale.ROOT).startsWith("resourcepacks:")) packsIndex = i;
                else if (lines.get(i).toLowerCase(Locale.ROOT).startsWith("incompatibleresourcepacks:")) incompatibleIndex = i;
            }
            if (packsIndex < 0) return;

            String current = lines.get(packsIndex).substring("resourcePacks:".length()).trim();
            String currentIncompatible = incompatibleIndex < 0 ? "[]"
                    : lines.get(incompatibleIndex).substring("incompatibleResourcePacks:".length()).trim();

            if (!enabled) {
                if (current.length() > 2 && !current.equals("[]")) {
                    packsSaved = current;
                    packsSavedIncompatible = currentIncompatible;
                }
                lines.set(packsIndex, "resourcePacks:[]");
                if (incompatibleIndex >= 0) lines.set(incompatibleIndex, "incompatibleResourcePacks:[]");
            } else if (!packsSaved.isEmpty()) {
                lines.set(packsIndex, "resourcePacks:" + packsSaved);
                if (incompatibleIndex >= 0 && !packsSavedIncompatible.isEmpty()) {
                    lines.set(incompatibleIndex, "incompatibleResourcePacks:" + packsSavedIncompatible);
                }
            }
            Files.write(path.toPath(), lines, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    /** Включает/выключает шейдеры через config/iris.properties. */
    public static void applyShaders(Prism prism, boolean enabled) {
        File path = new File(new File(prism.minecraftDir(), "config"), "iris.properties");
        if (!path.isFile()) return;
        try {
            List<String> lines = new ArrayList<String>(Files.readAllLines(path.toPath(), StandardCharsets.UTF_8));
            boolean found = false;
            for (int i = 0; i < lines.size(); i++) {
                if (!lines.get(i).trim().toLowerCase(Locale.ROOT).startsWith("enableshaders=")) continue;
                lines.set(i, "enableShaders=" + enabled);
                found = true;
            }
            if (!found) lines.add("enableShaders=" + enabled);
            Files.write(path.toPath(), lines, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    /** Пишет выделение памяти в instance.cfg (MaxMemAlloc/MinMemAlloc). */
    public static void applyMemory(Prism prism, int gb) {
        if (gb < 2) return;
        File config = new File(prism.instanceDir(), "instance.cfg");
        if (!config.isFile()) return;
        try {
            int max = gb * 1024;
            int min = Math.min(2048, max / 2);
            List<String> lines = new ArrayList<String>(Files.readAllLines(config.toPath(), StandardCharsets.UTF_8));
            boolean hasMax = false;
            boolean hasMin = false;
            boolean hasOverride = false;
            for (int i = 0; i < lines.size(); i++) {
                String trimmed = lines.get(i).trim().toLowerCase(Locale.ROOT);
                if (trimmed.startsWith("maxmemalloc=")) {
                    lines.set(i, "MaxMemAlloc=" + max);
                    hasMax = true;
                } else if (trimmed.startsWith("minmemalloc=")) {
                    lines.set(i, "MinMemAlloc=" + min);
                    hasMin = true;
                } else if (trimmed.startsWith("overridememory=")) {
                    lines.set(i, "OverrideMemory=true");
                    hasOverride = true;
                }
            }
            if (!hasMax) lines.add("MaxMemAlloc=" + max);
            if (!hasMin) lines.add("MinMemAlloc=" + min);
            if (!hasOverride) lines.add("OverrideMemory=true");
            Files.write(config.toPath(), lines, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    public static void applyAll(Prism prism) {
        if (prism == null || !prism.hasInstance()) return;
        values();
        int changed = syncMods(prism);
        applyPacks(prism, getBool("packs", true));
        applyShaders(prism, getBool("shaders", false));
        int memory = effectiveMemory();
        setMemory(memory);
        applyMemory(prism, memory);
        save();
        Log.ok("Настройки применены к сборке: память " + memory + " ГБ, ресурспаки "
                + (getBool("packs", true) ? "вкл" : "выкл") + ", шейдеры "
                + (getBool("shaders", false) ? "вкл" : "выкл")
                + (changed > 0 ? ", перенесено модов: " + changed : "") + ".");
    }

    /**
     * Память, которая реально уйдёт в игру: сохранённая настройка, а если её ещё нет —
     * значение по умолчанию для объёма ОЗУ.
     */
    public static int effectiveMemory() {
        int total = Math.max(8, Ram.gb());
        int stored = memory();
        int value = stored > 0 ? stored : (Ram.gb() <= 8 ? 4 : (Ram.gb() <= 16 ? 6 : 8));
        return Math.max(2, Math.min(total, value));
    }

    /** Список «всегда включённых» для подписи в интерфейсе. */
    public static String lockedText() {
        return "JourneyMap · Emotecraft · Immersive Paintings · Not Enough Animations\n"
                + "EMF и ETF — только вместе с ресурспаками";
    }

    public static List<String> prefixes() {
        List<String> all = new ArrayList<String>();
        for (Mod mod : MODS) all.addAll(Arrays.asList(mod.prefixes));
        return all;
    }
}
