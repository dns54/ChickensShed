package io.github.dns54.chickensshed.client;

//? if forge {
/*import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/^* Kept apart from ChickensShedForge so dedicated servers never load client classes. ^/
public final class ChickensShedForgeClient {
    private ChickensShedForgeClient() {}

    public static void init() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ConfigScreen(parent)));
    }
}
*///?}
