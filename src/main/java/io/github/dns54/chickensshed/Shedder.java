package io.github.dns54.chickensshed;

import net.minecraft.server.level.ServerLevel;

/** Added to every LivingEntity by LivingEntityMixin, which owns the countdown. */
public interface Shedder {
    String TIME_KEY = "ChickensShedTime";

    void chickensshed$shed(ServerLevel level);

    int chickensshed$getTime();
}
