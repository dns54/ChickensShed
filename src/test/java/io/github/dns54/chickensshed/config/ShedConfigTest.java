package io.github.dns54.chickensshed.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
    void saveReplacesTheFile(@TempDir Path dir) throws Exception {
        ShedConfig.load(dir); // writes the defaults
        Map<String, ShedConfig.MobDrop> mobs = Map.of("minecraft:cow", new ShedConfig.MobDrop());
        ShedConfig.save(mobs);

        assertEquals(mobs.keySet(), ShedConfig.parse(Files.readString(dir.resolve(ShedConfig.FILE_NAME)), () -> fail("entry skipped")).keySet());
        assertFalse(Files.exists(dir.resolve(ShedConfig.FILE_NAME + ".tmp")));
    }
}
