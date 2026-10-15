package io.github.dns54.chickensshed;

import io.github.dns54.chickensshed.config.ShedConfig;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class ChickensShed {
    public static final String MOD_ID = "chickensshed";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private ChickensShed() {}

    public static void init(Path configDir) {
        ShedConfig.load(configDir);
    }

    public static Identifier id(String id) {
        //? if <1.21 {
        /*return new Identifier(id);
        *///?} else
        return Identifier.parse(id);
    }
}
