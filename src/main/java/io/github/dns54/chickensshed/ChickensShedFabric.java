package io.github.dns54.chickensshed;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class ChickensShedFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ChickensShed.init(FabricLoader.getInstance().getConfigDir());
    }
}
//?}
