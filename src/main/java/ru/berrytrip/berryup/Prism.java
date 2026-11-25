package ru.berrytrip.berryup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Найденный лаунчер: Prism Launcher, PineconeMC или другой форк.
 * Инстанс ищется по имени внутри instance.cfg — папку игрок мог переименовать.
 */
public final class Prism {

    private static final String[] PRISM_EXE = {"prismlauncher.exe"};
    private static final String[] PINE_EXE = {"pineconemc.exe", "elyprismlauncher.exe"};

    public final String kind;
    public final File exe;
    public File dataDir;

    private String instanceId;
    private File instanceDir;

    private Prism(String kind, File exe, File dataDir) {
        this.kind = kind;
        this.exe = exe;
        this.dataDir = dataDir;
    }

    // ------------------------------------------------------------- поиск

    public static Prism find() {
        String saved = Store.exe();
        if (!saved.isEmpty()) {
            Prism info = fromExe(new File(saved), Store.data().isEmpty() ? null : new File(Store.data()));
            if (info != null && info.dataDir.isDirectory()) return info;
        }

        List<Prism> found = new ArrayList<Prism>();
        File[] roots = {new File(Cfg.LOCAL, "Programs"), Cfg.PROGRAM_FILES, Cfg.PROGRAM_FILES_X86};
        for (File root : roots) {
            addFound(found, inFolder(new File(root, "PrismLauncher"), PRISM_EXE));
            addFound(found, scan(root, PRISM_EXE));
            addFound(found, inFolder(new File(root, "PineconeMC"), PINE_EXE));
            addFound(found, scan(root, PINE_EXE));
        }

        if (found.isEmpty()) return null;
        for (Prism info : found) if (info.hasInstance()) return info;
        for (Prism info : found) if (info.dataDir.isDirectory()) return info;
        return found.get(0);
    }

    private static void addFound(List<Prism> found, File exe) {
        if (exe == null) return;
        for (Prism info : found) if (info.exe.equals(exe)) return;
        Prism info = fromExe(exe, null);
        if (info != null) found.add(info);
    }

    private static File inFolder(File folder, String[] names) {
        for (String name : names) {
            File file = new File(folder, name);
            if (file.isFile()) return file;
        }
        return null;
    }

    private static File scan(File root, String[] names) {
        if (root == null || !root.isDirectory()) return null;
        File[] children = root.listFiles();
        if (children == null) return null;
        for (File child : children) {
            if (!child.isDirectory()) continue;
            File direct = inFolder(child, names);
            if (direct != null) return direct;
            File[] inner = child.listFiles();
            if (inner == null) continue;
            for (File sub : inner) {
                if (!sub.isDirectory()) continue;
                File file = inFolder(sub, names);
                if (file != null) return file;
            }
        }
        return null;
    }

    public static Prism fromExe(File exe, File dataDir) {
        if (exe == null || !exe.isFile()) return null;
        String kind = kindOf(exe);
        File data = dataDir != null ? dataDir : guessDataDir(exe, kind);
        return new Prism(kind, exe, data);
    }

    public static String kindOf(File exe) {
        String name = exe.getName().toLowerCase(java.util.Locale.ROOT);
        return name.contains("pine") || name.contains("ely") ? "PineconeMC" : "Prism Launcher";
    }

    private static File guessDataDir(File exe, String kind) {
        File exeDir = exe.getParentFile();
        List<File> candidates = new ArrayList<File>();
        if (exeDir != null) {
            candidates.add(new File(exeDir, kind.equals("PineconeMC") ? "PineconeMC" : "PrismLauncher"));
            candidates.add(new File(exeDir, "instances"));
            candidates.add(exeDir);
        }
        candidates.add(new File(Cfg.ROAMING, kind.equals("PineconeMC") ? "PineconeMC" : "PrismLauncher"));
        if (kind.equals("PineconeMC")) {
            candidates.add(new File(Cfg.ROAMING, "ElyPrismLauncher"));
            candidates.add(new File(Cfg.ROAMING, "PrismLauncher"));
        }
        for (File candidate : candidates) {
            if (candidate != null && new File(candidate, "instances").isDirectory()) return candidate;
        }
        return new File(Cfg.ROAMING, kind.equals("PineconeMC") ? "PineconeMC" : "PrismLauncher");
    }

    // ------------------------------------------------------------- инстанс

    public File instancesDir() {
        return new File(dataDir, "instances");
    }

    public void rescan() {
        instanceId = null;
        instanceDir = null;
    }

    public File instanceDir() {
        if (instanceDir != null) return instanceDir;
        File byFolder = new File(instancesDir(), Cfg.BUILD);
        instanceDir = byFolder;
        File[] dirs = instancesDir().listFiles();
        if (dirs == null) return instanceDir;
        File byFolderCfg = new File(byFolder, "instance.cfg");
        if (byFolderCfg.isFile() && hasOurName(byFolderCfg)) {
            instanceId = byFolder.getName();
            return instanceDir;
        }
        Arrays.sort(dirs, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        for (File dir : dirs) {
            if (!dir.isDirectory()) continue;
            File cfg = new File(dir, "instance.cfg");
            if (cfg.isFile() && hasOurName(cfg)) {
                instanceDir = dir;
                instanceId = dir.getName();
                break;
            }
        }
        return instanceDir;
    }

    public boolean hasInstance() {
        File cfg = new File(instanceDir(), "instance.cfg");
        return cfg.isFile() && hasOurName(cfg);
    }

    /** Идентификатор для `--launch`: у Prism это имя папки инстанса. */
    public String instanceId() {
        instanceDir();
        if (instanceId != null) return instanceId;
        return instanceDir().getName();
    }

    public File minecraftDir() {
        return new File(instanceDir(), "minecraft");
    }

    /** Ник активного аккаунта Prism — чтобы подставить его в заявку. */
    public String accountName() {
        try {
            File file = new File(dataDir, "accounts.json");
            if (!file.isFile()) return "";
            JsonObject json = JsonParser.parseString(Sys.read(file)).getAsJsonObject();
            String active = json.has("activeAccount") ? json.get("activeAccount").getAsString() : "";
            JsonArray accounts = json.has("accounts") ? json.getAsJsonArray("accounts") : null;
            if (accounts == null) return "";
            String first = "";
            for (JsonElement element : accounts) {
                JsonObject account = element.getAsJsonObject();
                if (!account.has("profile") || account.get("profile").isJsonNull()) continue;
                String name = account.getAsJsonObject("profile").get("name").getAsString();
                if (first.isEmpty()) first = name;
                if (account.has("active") && account.get("active").getAsBoolean()) return name;
                if (!active.isEmpty() && account.has("id")
                        && active.equals(account.get("id").getAsString())) return name;
            }
            return first;
        } catch (Exception error) {
            return "";
        }
    }

    public File modsDir() {
        return new File(minecraftDir(), "mods");
    }

    public File bootstrapJar() {
        return new File(minecraftDir(), "packwiz-installer-bootstrap.jar");
    }

    public int modCount() {
        File[] jars = modsDir().listFiles();
        if (jars == null) return 0;
        int count = 0;
        for (File jar : jars) if (jar.isFile() && jar.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) count++;
        return count;
    }

    private static boolean hasOurName(File cfg) {
        String name = readInstanceName(cfg);
        return name != null && name.equalsIgnoreCase(Cfg.BUILD);
    }

    public static String readInstanceName(File cfg) {
        try {
            for (String line : Files.readAllLines(cfg.toPath(), StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.toLowerCase(java.util.Locale.ROOT).startsWith("name=")) {
                    return trimmed.substring(5).trim();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // ------------------------------------------------------------- java

    public File findJava() {
        List<File> roots = new ArrayList<File>();
        if (dataDir != null) roots.add(new File(dataDir, "java"));
        roots.add(new File(Cfg.ROAMING, "PrismLauncher/java"));
        roots.add(new File(Cfg.ROAMING, "PineconeMC/java"));

        for (File root : roots) {
            File[] runtimes = root.listFiles();
            if (runtimes == null) continue;
            Arrays.sort(runtimes, new Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            });
            for (int i = runtimes.length - 1; i >= 0; i--) {
                if (!runtimes[i].getName().startsWith("java-runtime")) continue;
                for (String name : new String[]{"javaw.exe", "java.exe"}) {
                    File java = new File(new File(runtimes[i], "bin"), name);
                    if (java.isFile()) return java;
                }
            }
        }
        return null;
    }

    private static String cachedJavaVersion;

    public static String javaVersion(File java) {
        if (cachedJavaVersion != null) return cachedJavaVersion;
        cachedJavaVersion = "";
        if (java == null || !java.isFile()) return cachedJavaVersion;
        try {
            Process process = new ProcessBuilder(java.getAbsolutePath(), "-version")
                    .redirectErrorStream(true).start();
            StringBuilder out = new StringBuilder();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) out.append(line).append('\n');
            process.waitFor();
            Matcher match = Pattern.compile("version \"([^\"]+)\"").matcher(out);
            if (match.find()) cachedJavaVersion = match.group(1);
        } catch (Exception ignored) {
        }
        return cachedJavaVersion;
    }

    public static void resetJavaVersion() {
        cachedJavaVersion = null;
    }
}
