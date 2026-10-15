package io.github.dns54.chickensshed.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ShedConfigTest {
    @Test
    void parsesAndSkipsOnlyBrokenEntries() {
        AtomicInteger skipped = new AtomicInteger();
        Map<String, ShedConfig.MobDrop> mobs = ShedConfig.parse("""
            {"mobs": {
              "minecraft:chicken": {"item": "minecraft:feather", "count": 2, "minSeconds": 10, "maxSeconds": 20, "babies": false},
              "minecraft:cow": {"item": "minecraft:leather"},
              "minecraft:pig": {"count": "lots"},
              "minecraft:sheep": {"minSeconds": 30, "maxSeconds": 10},
              "minecraft:goat": {"maxSeconds": 200000000},
              "Not An Id": {}
            }}""", skipped::incrementAndGet);

        assertEquals(2, mobs.size());
        // Each skip is reported, so the file gets backed up
        assertEquals(4, skipped.get());
        ShedConfig.MobDrop chicken = mobs.get("minecraft:chicken");
        assertEquals(2, chicken.count);
        assertEquals(10, chicken.minSeconds);
        assertEquals(20, chicken.maxSeconds);
        assertFalse(chicken.babies);
        // Missing keys fall back to defaults
        assertEquals(900, mobs.get("minecraft:cow").minSeconds);
        assertTrue(mobs.get("minecraft:cow").babies);
    }

    @Test
    void acceptsExactIntegersAndWaitLimit() {
        Map<String, ShedConfig.MobDrop> mobs = ShedConfig.parse("""
            {"mobs": {
              "minecraft:chicken": {"count": 1.0, "minSeconds": 1e1, "maxSeconds": 20.0},
              "minecraft:cow": {"count": 64, "minSeconds": %d, "maxSeconds": %d}
            }}""".formatted(ShedConfig.MobDrop.MAX_SECONDS, ShedConfig.MobDrop.MAX_SECONDS),
            () -> fail("valid entry skipped"));

        assertEquals(2, mobs.size());
        assertEquals(1, mobs.get("minecraft:chicken").count);
        assertEquals(10, mobs.get("minecraft:chicken").minSeconds);
        assertEquals(20, mobs.get("minecraft:chicken").maxSeconds);
        assertEquals(64, mobs.get("minecraft:cow").count);
        assertEquals(ShedConfig.MobDrop.MAX_SECONDS, mobs.get("minecraft:cow").minSeconds);
        assertEquals(ShedConfig.MobDrop.MAX_SECONDS, mobs.get("minecraft:cow").maxSeconds);
    }

    @ParameterizedTest
    @ValueSource(strings = {"count", "minSeconds", "maxSeconds"})
    void rejectsInvalidNumbersBeforeNarrowing(String field) {
        for (String value : new String[] {"1.5", "1.0000000000000000001", "4294967297",
                "-4294967295", "2147483648", "-2147483649", "1e100", "null", "true", "\"1\""}) {
            AtomicInteger skipped = new AtomicInteger();
            String json = """
                {"mobs": {
                  "minecraft:chicken": {"count": %s, "minSeconds": %s, "maxSeconds": %s},
                  "minecraft:cow": {}
                }}""".formatted(field.equals("count") ? value : "1",
                    field.equals("minSeconds") ? value : "1",
                    field.equals("maxSeconds") ? value : "1");
            Map<String, ShedConfig.MobDrop> mobs = ShedConfig.parse(json, skipped::incrementAndGet);

            assertEquals(java.util.Set.of("minecraft:cow"), mobs.keySet(), field + "=" + value);
            assertEquals(1, skipped.get(), field + "=" + value);
        }
    }

    @Test
    void backsUpInvalidNumbersBeforeSavingValidEntries(@TempDir Path dir) throws Exception {
        String original = """
            {"mobs": {
              "minecraft:chicken": {"count": 1.5},
              "minecraft:pig": {"minSeconds": 4294967297},
              "minecraft:sheep": {"minSeconds": 1, "maxSeconds": 4294967297},
              "minecraft:cow": {}
            }}""";
        Path file = dir.resolve(ShedConfig.FILE_NAME);
        Path backup = dir.resolve(ShedConfig.FILE_NAME + ".bak");
        Files.writeString(file, original);

        ShedConfig.load(dir);

        assertEquals(java.util.Set.of("minecraft:cow"), ShedConfig.mobs().keySet());
        assertEquals(original, Files.readString(file));
        assertEquals(original, Files.readString(backup));
        ShedConfig.save(ShedConfig.mobs());
        assertEquals(original, Files.readString(backup));
        assertEquals(java.util.Set.of("minecraft:cow"),
            ShedConfig.parse(Files.readString(file), () -> fail("entry skipped")).keySet());
    }

    @Test
    void saveReplacesTheFile(@TempDir Path dir) throws Exception {
        ShedConfig.load(dir); // writes the defaults
        Map<String, ShedConfig.MobDrop> mobs = Map.of("minecraft:cow", new ShedConfig.MobDrop());
        ShedConfig.save(mobs);

        assertEquals(mobs.keySet(), ShedConfig.parse(Files.readString(dir.resolve(ShedConfig.FILE_NAME)), () -> fail("entry skipped")).keySet());
        assertFalse(Files.exists(dir.resolve(ShedConfig.FILE_NAME + ".tmp")));
    }
}
