package io.github.dns54.chickensshed;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Skips the Supplementaries mixin when Supplementaries isn't installed, so Mixin doesn't warn about the missing class.
 */
public class MixinPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !mixinClassName.contains(".SupplementariesMixin") || supplementariesLoaded(); // the merged jar renames it SupplementariesMixin_<loader>
    }

    private static boolean supplementariesLoaded() {
        //? if !supplementaries {
        return false;
        //?} elif fabric {
        /*return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("supplementaries");
        *///?} elif forge {
        /*return net.minecraftforge.fml.loading.LoadingModList.get().getModFileById("supplementaries") != null;
        *///?} else {
        /*return net.neoforged.fml.loading.LoadingModList.get().getModFileById("supplementaries") != null;
        *///?}
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
