package io.github.dns54.chickensshed;

import io.github.dns54.chickensshed.config.ShedConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.21.11 {
import net.minecraft.world.level.gamerules.GameRules;
//?} else
/*import net.minecraft.world.level.GameRules;*/

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Egg-style shedding: each configured mob counts down to its next drop, like a chicken to its next egg.
 */
public final class Shedding {
    private record Drop(Item item, int count, int minTicks, int maxTicks, boolean babies) {}

    private static Map<String, ShedConfig.MobDrop> resolvedFrom;
    private static Map<EntityType<?>, Drop> drops = Map.of();

    private Shedding() {}

    /**
     * Advances the mob's countdown and drops its item at the mob when it runs out.
     *
     * @param time ticks until the next drop, 0 when not started yet
     * @return the new countdown to store on the mob
     */
    public static int tick(LivingEntity entity, ServerLevel level, int time) {
        Drop drop = drops().get(entity.getType());
        // 0 is not saved, so mobs removed from the config lose their timer tag on the next save
        if (drop == null) return 0;
        // Babies that don't shed pause their countdown until they grow up
        if (!entity.isAlive() || (entity.isBaby() && !drop.babies)) return time;

        if (time <= 0) return roll(entity, drop);
        if (--time > 0) return time;

        if (mobLoot(level)) {
            ItemEntity item = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(drop.item, drop.count));
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
        return roll(entity, drop);
    }

    private static int roll(LivingEntity entity, Drop drop) {
        return drop.minTicks + entity.getRandom().nextInt(drop.maxTicks - drop.minTicks + 1);
    }

    private static boolean mobLoot(ServerLevel level) {
        //? if >=1.21.11 {
        return level.getGameRules().get(GameRules.MOB_DROPS);
        //?} else
        /*return level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);*/
    }

    /** Resolves config ids against the registries once per config change; unknown ids are logged and skipped. */
    private static Map<EntityType<?>, Drop> drops() {
        Map<String, ShedConfig.MobDrop> mobs = ShedConfig.mobs();
        if (mobs == resolvedFrom) return drops;

        Map<EntityType<?>, Drop> result = new HashMap<>();
        mobs.forEach((mobId, mob) -> {
            var type = findMob(mobId);
            var item = findItem(mob.item);
            if (type.isEmpty()) ChickensShed.LOGGER.warn("Unknown mob {} in {}", mobId, ShedConfig.FILE_NAME);
            else if (item.isEmpty()) ChickensShed.LOGGER.warn("Unknown item {} for {} in {}", mob.item, mobId, ShedConfig.FILE_NAME);
            else result.put(type.get(), new Drop(item.get(), mob.count, mob.minSeconds * 20, mob.maxSeconds * 20, mob.babies));
        });
        drops = result;
        resolvedFrom = mobs;
        return result;
    }

    public static Optional<EntityType<?>> findMob(String id) {
        return ShedConfig.isValidId(id) ? BuiltInRegistries.ENTITY_TYPE.getOptional(ChickensShed.id(id)) : Optional.empty();
    }

    public static Optional<Item> findItem(String id) {
        return ShedConfig.isValidId(id)
            ? BuiltInRegistries.ITEM.getOptional(ChickensShed.id(id)).filter(item -> item != Items.AIR)
            : Optional.empty();
    }
}
