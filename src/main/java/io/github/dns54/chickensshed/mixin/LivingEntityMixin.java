package io.github.dns54.chickensshed.mixin;

import io.github.dns54.chickensshed.Shedder;
import io.github.dns54.chickensshed.Shedding;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.6 {
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//?} else
/*import net.minecraft.nbt.CompoundTag;*/

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements Shedder {
    /** Ticks until the next drop, 0 when not started. */
    @Unique
    private int chickensshed$time;

    @Override
    public void chickensshed$shed(ServerLevel level) {
        chickensshed$time = Shedding.tick((LivingEntity) (Object) this, level, chickensshed$time);
    }

    @Override
    public int chickensshed$getTime() {
        return chickensshed$time;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void chickensshed$tick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level() instanceof ServerLevel level) chickensshed$shed(level);
    }

    //? if >=1.21.6 {
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void chickensshed$save(ValueOutput output, CallbackInfo ci) {
        if (chickensshed$time > 0) output.putInt(TIME_KEY, chickensshed$time);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void chickensshed$load(ValueInput input, CallbackInfo ci) {
        chickensshed$time = input.getIntOr(TIME_KEY, 0);
    }
    //?} else {
    /*@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void chickensshed$save(CompoundTag tag, CallbackInfo ci) {
        if (chickensshed$time > 0) tag.putInt(TIME_KEY, chickensshed$time);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void chickensshed$load(CompoundTag tag, CallbackInfo ci) {
        chickensshed$time = tag.getInt(TIME_KEY);
    }
    *///?}
}
