package io.github.dns54.chickensshed.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.dns54.chickensshed.ChickensShed;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * {@code config/chickensshed.json}: which mobs shed what, and how often.
 */
public final class ShedConfig {
    public static final String FILE_NAME = "chickensshed.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private static Path file;
    private static volatile Map<String, MobDrop> mobs = defaults();

    private ShedConfig() {}

    /** One mob's drop. Field names are the JSON keys; missing keys keep these defaults. */
    public static final class MobDrop {
        /** Waits are counted in ticks (×20), which must fit an int. */
        public static final int MAX_SECONDS = Integer.MAX_VALUE / 20;

        public String item = "minecraft:feather";
        public int count = 1;
        public int minSeconds = 900;
        public int maxSeconds = 1500;
        public boolean babies = true;

        /** @return why this drop is unusable, or null when valid */
        public String problem() {
            if (item == null || !ID.matcher(item).matches()) return "item must be an id like minecraft:feather";
            if (count < 1 || count > 64) return "count must be 1-64";
            if (minSeconds < 1) return "minSeconds must be at least 1";
            if (maxSeconds < minSeconds) return "maxSeconds must not be below minSeconds";
            if (maxSeconds > MAX_SECONDS) return "maxSeconds must be at most " + MAX_SECONDS;
            return null;
        }

        public MobDrop copy() {
            MobDrop copy = new MobDrop();
            copy.item = item;
            copy.count = count;
            copy.minSeconds = minSeconds;
            copy.maxSeconds = maxSeconds;
            copy.babies = babies;
            return copy;
        }
    }

    public static Map<String, MobDrop> mobs() {
        return mobs;
    }

    public static boolean isValidId(String id) {
        return id != null && ID.matcher(id).matches();
    }

    /** Loads the config, writing defaults when the file doesn't exist. */
    public static void load(Path configDir) {
        file = configDir.resolve(FILE_NAME);
        try {
            if (Files.exists(file)) {
                boolean[] skipped = {false};
                mobs = parse(Files.readString(file), () -> skipped[0] = true);
                if (skipped[0]) backup();
                return;
            }
            save(mobs);
        } catch (IOException | RuntimeException e) {
            ChickensShed.LOGGER.error("Could not load {}, using defaults until it is fixed", file, e);
            mobs = defaults();
            if (Files.exists(file)) backup();
        }
    }

    /** Keeps a copy of a file that didn't fully load: saving from the config screen writes only what loaded. */
    private static void backup() {
        Path copy = file.resolveSibling(FILE_NAME + ".bak");
        try {
            Files.copy(file, copy, StandardCopyOption.REPLACE_EXISTING);
            ChickensShed.LOGGER.warn("Saved a copy of {} as {}", file, copy);
        } catch (IOException e) {
            ChickensShed.LOGGER.error("Could not back up {}", file, e);
        }
    }

    /** Replaces the config in memory and on disk. */
    public static void save(Map<String, MobDrop> newMobs) throws IOException {
        JsonObject mobsJson = new JsonObject();
        newMobs.forEach((id, drop) -> mobsJson.add(id, GSON.toJsonTree(drop)));
        JsonObject root = new JsonObject();
        root.add("mobs", mobsJson);
        Files.createDirectories(file.getParent());
        // Written next to it and swapped in, so a crash or full disk mid-write never leaves a half-written config
        Path tmp = file.resolveSibling(FILE_NAME + ".tmp");
        Files.writeString(tmp, GSON.toJson(root));
        try {
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
        mobs = Collections.unmodifiableMap(new LinkedHashMap<>(newMobs));
    }

    public static Map<String, MobDrop> defaults() {
        return Map.of("minecraft:chicken", new MobDrop());
    }

    /** Parses the JSON; a broken entry is logged and skipped (reported to {@code onSkip}) without touching the others. */
    static Map<String, MobDrop> parse(String json, Runnable onSkip) {
        JsonElement mobsJson = JsonParser.parseString(json).getAsJsonObject().get("mobs");
        Map<String, MobDrop> result = new LinkedHashMap<>();
        if (mobsJson == null) return result;
        for (Map.Entry<String, JsonElement> entry : mobsJson.getAsJsonObject().entrySet()) {
            String mob = entry.getKey();
            try {
                MobDrop drop = GSON.fromJson(entry.getValue(), MobDrop.class);
                String problem = !isValidId(mob) ? "mob must be an id like minecraft:chicken" : drop.problem();
                if (problem == null) {
                    result.put(mob, drop);
                    continue;
                }
                ChickensShed.LOGGER.warn("Skipping {} in {}: {}", mob, FILE_NAME, problem);
            } catch (RuntimeException e) {
                ChickensShed.LOGGER.warn("Skipping {} in {}: {}", mob, FILE_NAME, e.getMessage());
            }
            onSkip.run();
        }
        return result;
    }
}
