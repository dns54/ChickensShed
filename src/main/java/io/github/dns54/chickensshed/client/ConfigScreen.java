package io.github.dns54.chickensshed.client;

import io.github.dns54.chickensshed.ChickensShed;
import io.github.dns54.chickensshed.Shedding;
import io.github.dns54.chickensshed.config.ShedConfig;
import io.github.dns54.chickensshed.config.ShedConfig.MobDrop;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lists the configured mobs. Every change is saved right away; Undo steps back through this visit's changes.
 */
public class ConfigScreen extends ShedScreen {
    private final Deque<Map<String, MobDrop>> history = new ArrayDeque<>();
    private ShedList list;
    private double scroll;
    private Component error;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("chickensshed.config.title"), parent);
    }

    @Override
    protected void init() {
        // The list first: on 1.20.1 it paints the header and footer bars, which the title and buttons go on top of
        list = addRenderableWidget(new ShedList(minecraft, width, height, left()));
        for (Map.Entry<String, MobDrop> mob : ShedConfig.mobs().entrySet()) {
            String id = mob.getKey();
            MobDrop drop = mob.getValue();
            ShedList.Row row = list.row();
            Button edit = row.add(Button.builder(describe(id, drop), b -> edit(id, drop.copy()))
                .width(WIDTH - BUTTON_HEIGHT - GAP).build(), 0);
            edit.setTooltip(Tooltip.create(Component.translatable("chickensshed.config.hint")));
            Button remove = row.add(Button.builder(Component.literal("✕").withStyle(ChatFormatting.RED), b -> remove(id))
                .width(BUTTON_HEIGHT).build(), WIDTH - BUTTON_HEIGHT);
            remove.setTooltip(Tooltip.create(Component.translatable("chickensshed.config.remove")));
            ItemStack icon = Shedding.findItem(drop.item).map(ShedScreen::icon).orElse(null);
            if (icon != null) row.paint((canvas, x, y) -> canvas.item(icon, x + 2, y + 2));
        }
        if (ShedConfig.mobs().isEmpty()) {
            Component empty = Component.translatable("chickensshed.config.empty");
            list.row().paint((canvas, x, y) -> canvas.text(empty, (width - font.width(empty)) / 2, y + 6, GRAY));
        }
        list.row().add(Button.builder(Component.translatable("chickensshed.config.add"), b -> edit(null, new MobDrop()))
            .width(WIDTH).build(), 0);
        list.setScrollAmount(scroll);

        addRenderableWidget(centered(title, TITLE_Y));
        Button undo = Button.builder(Component.translatable("chickensshed.config.undo"), b -> undo()).build();
        undo.active = !history.isEmpty();
        Button reset = Button.builder(Component.translatable("chickensshed.config.reset"), b -> reset()).build();
        reset.setTooltip(Tooltip.create(Component.translatable("chickensshed.config.reset.tooltip")));
        footer(undo, reset, Button.builder(CommonComponents.GUI_DONE, b -> onClose()).build());
    }

    /** Opens the edit screen, remembering the scroll position to come back to. */
    private void edit(String id, MobDrop drop) {
        scroll = list.scroll();
        show(new MobEditScreen(this, id, drop));
    }

    /** Rebuilds the screen after a change, keeping the list scrolled where it was. */
    private void refresh() {
        scroll = list.scroll();
        rebuildWidgets();
    }

    @Override
    protected void draw(Canvas canvas) {
        if (error != null) canvas.text(error, (width - font.width(error)) / 2, TITLE_Y + 12, RED);
    }

    /** "Chicken: 1 × Feather every 15–25 min", falling back to ids for things that aren't installed. */
    static Component describe(String id, MobDrop drop) {
        Component mob = Shedding.findMob(id).map(type -> type.getDescription()).orElse(Component.literal(id));
        Component item = Shedding.findItem(drop.item).map(ShedScreen::name).orElse(Component.literal(drop.item));
        return Component.translatable("chickensshed.config.row", mob, drop.count, item, interval(drop.minSeconds, drop.maxSeconds));
    }

    /** Whole minutes when both times allow it, otherwise seconds. */
    static Component interval(int min, int max) {
        boolean minutes = min % 60 == 0 && max % 60 == 0;
        String unit = minutes ? "minutes" : "seconds";
        int from = minutes ? min / 60 : min;
        int to = minutes ? max / 60 : max;
        return from == to
            ? Component.translatable("chickensshed.time." + unit, from)
            : Component.translatable("chickensshed.time." + unit + "_range", from, to);
    }

    /** Saves {@code mobs} as the new config, remembering the current one for Undo when {@code undoable}. */
    void change(Map<String, MobDrop> mobs, boolean undoable) {
        Map<String, MobDrop> before = copy(ShedConfig.mobs());
        try {
            ShedConfig.save(mobs);
            if (undoable) history.push(before);
            error = null;
        } catch (IOException e) {
            ChickensShed.LOGGER.error("Could not save {}", ShedConfig.FILE_NAME, e);
            error = Component.translatable("chickensshed.config.save_failed");
        }
    }

    /** Remembers the current config as one Undo step, for edits that are saved in many small changes. */
    void remember() {
        history.push(copy(ShedConfig.mobs()));
    }

    /** Replaces {@code oldId} (null when adding) with {@code newId}, keeping its place in the list. */
    void put(String oldId, String newId, MobDrop drop) {
        Map<String, MobDrop> mobs = new LinkedHashMap<>();
        ShedConfig.mobs().forEach((id, old) -> {
            if (id.equals(oldId)) mobs.put(newId, drop);
            else mobs.put(id, old);
        });
        mobs.putIfAbsent(newId, drop);
        change(mobs, false);
    }

    boolean contains(String id) {
        return ShedConfig.mobs().containsKey(id);
    }

    void remove(String id) {
        Map<String, MobDrop> mobs = new LinkedHashMap<>(ShedConfig.mobs());
        mobs.remove(id);
        change(mobs, true);
        refresh();
    }

    void undo() {
        Map<String, MobDrop> previous = history.pop();
        change(previous, false);
        refresh();
    }

    private void reset() {
        change(ShedConfig.defaults(), true);
        scroll = 0;
        rebuildWidgets();
    }

    private static Map<String, MobDrop> copy(Map<String, MobDrop> mobs) {
        Map<String, MobDrop> copy = new LinkedHashMap<>();
        mobs.forEach((id, drop) -> copy.put(id, drop.copy()));
        return copy;
    }
}
