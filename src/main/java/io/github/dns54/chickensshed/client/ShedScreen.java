package io.github.dns54.chickensshed.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else
/*import net.minecraft.client.gui.GuiGraphics;*/

/**
 * Shared base of the config screens; holds the only version-specific drawing code.
 */
public abstract class ShedScreen extends Screen {
    static final int WHITE = 0xFFFFFFFF;
    static final int GRAY = 0xFFA0A0A0;
    static final int GREEN = 0xFF55FF55;
    static final int RED = 0xFFFF5555;

    /** Layout grid shared by both screens, following vanilla options screens. */
    static final int WIDTH = 305;
    static final int GAP = 5;
    static final int BUTTON_HEIGHT = 20;
    static final int ROW = BUTTON_HEIGHT + GAP;
    static final int HEADER = 33;
    static final int FOOTER = 33;
    static final int TITLE_Y = (HEADER - 9) / 2;

    protected final Screen parent;

    protected ShedScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    /** Version-neutral drawing, so screens don't care how each version renders. */
    interface Canvas {
        void item(ItemStack stack, int x, int y);

        void text(Component text, int x, int y, int color);
    }

    /** Draws on top of the widgets. */
    protected void draw(Canvas canvas) {}

    /** Works on the main menu too, where 26.x items can't make stacks yet. */
    static Component name(Item item) {
        return Component.translatable(item.getDescriptionId());
    }

    /** Set while icons are drawn; ItemStackMixin then turns off the enchantment glint so icons look the same everywhere. */
    public static boolean drawingIcon;

    /**
     * The item's own model (from its id), drawn the same way on every version, with or without a world;
     * null when that model is missing, so no missing-texture icon is shown.
     */
    static ItemStack icon(Item item) {
        Identifier id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        Minecraft minecraft = Minecraft.getInstance();
        //? if >=1.21.4 {
        if (minecraft.getModelManager().getItemModel(id) instanceof net.minecraft.client.renderer.item.MissingItemModel) return null;
        //?} else
        /*if (minecraft.getItemRenderer().getItemModelShaper().getItemModel(item) == minecraft.getModelManager().getMissingModel()) return null;*/

        //? if >=26.1 {
        // 26.x items get their components only when a world loads and a normal stack crashes before that,
        // so the stack holds just the model
        var components = net.minecraft.core.component.DataComponentMap.builder()
            .set(net.minecraft.core.component.DataComponents.ITEM_MODEL, id)
            .build();
        return new ItemStack(net.minecraft.core.Holder.direct(item, components));
        //?} elif >=1.21.4 {
        /*ItemStack stack = new ItemStack(item);
        stack.set(net.minecraft.core.component.DataComponents.ITEM_MODEL, id);
        return stack;
        *///?} else
        /*return new ItemStack(item);*/
    }

    static void show(Screen screen) {
        //? if >=26.1 {
        Minecraft.getInstance().gui.setScreen(screen);
        //?} else
        /*Minecraft.getInstance().setScreen(screen);*/
    }

    /** Text labels stopped centering themselves in 26.x, so each one is sized to its text and placed centered. */
    protected StringWidget centered(Component text, int y) {
        int textWidth = font.width(text);
        return new StringWidget((width - textWidth) / 2, y, textWidth, 9, text, font);
    }

    protected int left() {
        return (width - WIDTH) / 2;
    }

    protected int footerY() {
        return height - FOOTER + (FOOTER - BUTTON_HEIGHT) / 2;
    }

    protected void footer(Button... buttons) {
        int buttonWidth = (WIDTH - GAP * (buttons.length - 1)) / buttons.length;
        for (int i = 0; i < buttons.length; i++) {
            // The last button takes the pixels left over by rounding, so the row always ends at the grid's edge
            buttons[i].setWidth(i == buttons.length - 1 ? WIDTH - i * (buttonWidth + GAP) : buttonWidth);
            buttons[i].setX(left() + i * (buttonWidth + GAP));
            buttons[i].setY(footerY());
            addRenderableWidget(buttons[i]);
        }
    }

    @Override
    public void onClose() {
        show(parent);
    }

    //? if >=26.1 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        draw(canvas(graphics));
    }

    static Canvas canvas(GuiGraphicsExtractor graphics) {
        var font = Minecraft.getInstance().font;
        return new Canvas() {
            public void item(ItemStack stack, int x, int y) {
                drawingIcon = true;
                try {
                    graphics.item(stack, x, y);
                } finally {
                    drawingIcon = false;
                }
            }

            public void text(Component text, int x, int y, int color) {
                graphics.text(font, text, x, y, color);
            }
        };
    }
    //?} else {
    /*@Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        //? if <1.20.2
        /^renderBackground(graphics);^/
        super.render(graphics, mouseX, mouseY, delta);
        draw(canvas(graphics));
    }

    static Canvas canvas(GuiGraphics graphics) {
        var font = Minecraft.getInstance().font;
        return new Canvas() {
            public void item(ItemStack stack, int x, int y) {
                drawingIcon = true;
                try {
                    graphics.renderItem(stack, x, y);
                } finally {
                    drawingIcon = false;
                }
            }

            public void text(Component text, int x, int y, int color) {
                graphics.drawString(font, text, x, y, color);
            }
        };
    }
    *///?}
}
