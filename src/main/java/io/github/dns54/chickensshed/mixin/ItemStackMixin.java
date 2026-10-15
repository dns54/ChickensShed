package io.github.dns54.chickensshed.mixin;

import io.github.dns54.chickensshed.client.ShedScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Config icons show the plain item model: some items glint by default on some versions but can't on others.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "hasFoil", at = @At("HEAD"), cancellable = true)
    private void chickensshed$noGlintInConfig(CallbackInfoReturnable<Boolean> cir) {
        if (ShedScreen.drawingIcon) cir.setReturnValue(false);
    }
}
