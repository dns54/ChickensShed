package io.github.dns54.chickensshed.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if supplementaries {
/*import io.github.dns54.chickensshed.Shedder;
import net.mehvahdjukaar.supplementaries.api.CapturedMobInstance;
import net.minecraft.server.level.ServerLevel;
*///?}

/**
 * Mobs caught in Supplementaries jars and cages leave the world, so they're ticked here instead.
 * Drops appear at the captured mob like Supplementaries' own eggs, so hoppers collect them.
 * Does nothing when Supplementaries isn't installed.
 */
@Pseudo
// Data-defined mobs (vanilla ones like chickens) override containerTick without calling super,
// mobs implementing Supplementaries' ICatchableMob use the base class, so both are covered
@Mixin(targets = {
    "net.mehvahdjukaar.supplementaries.api.CapturedMobInstance",
    "net.mehvahdjukaar.supplementaries.common.misc.mob_container.DataCapturedMobInstance"
})
public abstract class SupplementariesMixin {
    @Inject(method = "containerTick", at = @At("TAIL"), require = 0)
    private void chickensshed$tick(Level level, BlockPos pos, float scale, CompoundTag tag, CallbackInfo ci) {
        //? if supplementaries {
        /*if (level instanceof ServerLevel server
                && ((CapturedMobInstance<?>) (Object) this).getEntityForRenderer() instanceof Shedder shedder) {
            shedder.chickensshed$shed(server);
            // Supplementaries saves this tag, not the cached entity used for ticking.
            int time = shedder.chickensshed$getTime();
            if (time > 0) tag.putInt(Shedder.TIME_KEY, time);
            else tag.remove(Shedder.TIME_KEY);
        }
        *///?}
    }
}
