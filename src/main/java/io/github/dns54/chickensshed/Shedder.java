package io.github.dns54.chickensshed;

import net.minecraft.server.level.ServerLevel;

/** Added to every LivingEntity by LivingEntityMixin, which owns the countdown. */
public interface Shedder {
    void chickensshed$shed(ServerLevel level);
}
