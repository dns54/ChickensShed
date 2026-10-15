package io.github.dns54.chickensshed.client;

import io.github.dns54.chickensshed.Shedding;
import io.github.dns54.chickensshed.config.ShedConfig.MobDrop;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Edits one mob. Each valid change is saved right away; the whole visit is one Undo step on the list.
 */
public class MobEditScreen extends ShedScreen {
    private static final int COLUMN = (WIDTH - 2 * GAP) / 3;

    private final ConfigScreen list;
    private final MobDrop drop;
    private final boolean adding;
    /** The mob's id in the saved config, null until a new mob first becomes valid. */
    private String savedId;
    private boolean remembered;
    private String mob, item, count, min, max;
    private Component problem;

    MobEditScreen(ConfigScreen list, String id, MobDrop drop) {
        super(Component.translatable(id == null ? "chickensshed.edit.add" : "chickensshed.edit.edit"), list);
        this.list = list;
        this.drop = drop;
        adding = id == null;
        savedId = id;
        mob = id == null ? "" : shortId(id);
        item = adding ? "" : shortId(drop.item);
        count = String.valueOf(drop.count);
        min = String.valueOf(drop.minSeconds);
        max = String.valueOf(drop.maxSeconds);
        problem = check();
    }

    @Override
    protected void init() {
        ShedList rows = addRenderableWidget(new ShedList(minecraft, width, height, left()));
        field(rows, "chickensshed.edit.mob", mob, v -> mob = v, this::mobPreview);
        field(rows, "chickensshed.edit.item", item, v -> item = v, this::itemPreview);
        field(rows, "chickensshed.edit.count", count, v -> count = v, (canvas, x, y) -> {});
        field(rows, "chickensshed.edit.min", min, v -> min = v, (canvas, x, y) -> minutes(canvas, min, x, y));
        field(rows, "chickensshed.edit.max", max, v -> max = v, (canvas, x, y) -> minutes(canvas, max, x, y));
        rows.row().add(CycleButton.onOffBuilder(drop.babies).create(0, 0, WIDTH, BUTTON_HEIGHT,
            Component.translatable("chickensshed.edit.babies"), (b, value) -> {
                drop.babies = value;
                changed();
            }), 0);
        rows.row().paint((canvas, x, y) -> {
            if (problem != null) canvas.text(problem, (width - font.width(problem)) / 2, y + 6, RED);
        });

        addRenderableWidget(centered(title, TITLE_Y));
        Button first = adding
            // Undoes this visit, which removes the new mob again if it was already saved
            ? Button.builder(CommonComponents.GUI_CANCEL, b -> {
                if (remembered) list.undo();
                onClose();
            }).build()
            : Button.builder(Component.translatable("chickensshed.edit.delete").withStyle(ChatFormatting.RED), b -> {
                // Already one Undo step when this visit saved an edit, so Undo brings back the original
                list.remove(savedId, !remembered);
                onClose();
            }).build();
        footer(first, Button.builder(CommonComponents.GUI_DONE, b -> onClose()).build());
    }

    /** Label, input and live preview in three equal columns. */
    private void field(ShedList rows, String label, String value, Consumer<String> setter, ShedList.Painter preview) {
        Component text = Component.translatable(label);
        ShedList.Row row = rows.row();
        EditBox box = row.add(new EditBox(font, 0, 0, COLUMN, BUTTON_HEIGHT, text), COLUMN + GAP);
        box.setMaxLength(256);
        box.setValue(value);
        box.setResponder(v -> {
            setter.accept(v);
            changed();
        });
        row.paint((canvas, x, y) -> {
            canvas.text(text, x, y + 6, WHITE);
            preview.paint(canvas, x + 2 * (COLUMN + GAP), y);
        });
    }

    private void mobPreview(Canvas canvas, int x, int y) {
        Shedding.findMob(withNamespace(mob)).ifPresentOrElse(
            type -> canvas.text(fit(type.getDescription(), COLUMN), x, y + 6, GREEN),
            () -> canvas.text(Component.translatable("chickensshed.edit.unknown"), x, y + 6, RED));
    }

    private void itemPreview(Canvas canvas, int x, int y) {
        Shedding.findItem(withNamespace(item)).ifPresentOrElse(found -> {
            var stack = icon(found);
            if (stack != null) canvas.item(stack, x, y + 2);
            canvas.text(fit(name(found), COLUMN - 20), x + 20, y + 6, GREEN);
        }, () -> canvas.text(Component.translatable("chickensshed.edit.unknown"), x, y + 6, RED));
    }

    /** Shortens a preview to its column, so long names don't run over the scrollbar. */
    private Component fit(Component text, int width) {
        if (font.width(text) <= width) return text;
        return Component.literal(font.plainSubstrByWidth(text.getString(), width - font.width("…")) + "…");
    }

    private void changed() {
        problem = check();
        if (problem != null) return;
        if (!remembered) {
            list.remember();
            remembered = true;
        }
        String id = withNamespace(mob);
        if (list.put(savedId, id, drop.copy())) savedId = id;
        else problem = Component.translatable("chickensshed.config.save_failed");
    }

    /** Copies the fields into {@link #drop}; returns what's wrong, or null. */
    private Component check() {
        String mobId = withNamespace(mob);
        drop.item = withNamespace(item);
        Integer count = number(this.count), min = number(this.min), max = number(this.max);
        if (count == null || min == null || max == null) return Component.translatable("chickensshed.edit.not_number");
        if (Shedding.findMob(mobId).isEmpty()) return Component.translatable("chickensshed.edit.unknown_mob");
        if (!mobId.equals(savedId) && list.contains(mobId)) return Component.translatable("chickensshed.edit.duplicate");
        if (Shedding.findItem(drop.item).isEmpty()) return Component.translatable("chickensshed.edit.unknown_item");
        drop.count = count;
        drop.minSeconds = min;
        drop.maxSeconds = max;
        MobDrop.Problem problem = drop.problem();
        return problem == null ? null : Component.translatable(problem.key(), MobDrop.MAX_SECONDS);
    }

    /** Shows "= 15 min" next to a time in seconds. */
    private void minutes(Canvas canvas, String seconds, int x, int y) {
        try {
            int value = Integer.parseInt(seconds.trim());
            // Only for valid waits: no hint for a value that is rejected anyway
            if (value < 60 || value > MobDrop.MAX_SECONDS) return;
            Component text = value % 60 == 0
                ? Component.translatable("chickensshed.time.minutes", value / 60)
                : Component.translatable("chickensshed.time.min_sec", value / 60, value % 60);
            canvas.text(Component.literal("= ").append(text), x, y, GRAY);
        } catch (NumberFormatException ignored) {
        }
    }

    /** Whole number, saturated instead of failing when it's too big to parse; null when not a number. */
    private static Integer number(String text) {
        text = text.trim();
        if (!text.matches("-?\\d+")) return null;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return text.startsWith("-") ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
    }

    /** Lets players type "chicken" for "minecraft:chicken". */
    private static String withNamespace(String id) {
        id = id.trim();
        return id.isEmpty() || id.contains(":") ? id : "minecraft:" + id;
    }

    private static String shortId(String id) {
        return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
    }
}
