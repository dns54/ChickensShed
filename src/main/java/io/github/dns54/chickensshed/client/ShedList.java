package io.github.dns54.chickensshed.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else
/*import net.minecraft.client.gui.GuiGraphics;*/

import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla scrolling list between the header and footer, so each version draws its own options-screen look
 * (bars, separators, background, scrollbar). Rows are widgets on the shared grid.
 */
public class ShedList extends ContainerObjectSelectionList<ShedList.Row> {
    private final int left;

    ShedList(Minecraft minecraft, int screenWidth, int screenHeight, int left) {
        //? if <1.20.2 {
        /*super(minecraft, screenWidth, screenHeight, ShedScreen.HEADER, screenHeight - ShedScreen.FOOTER, ShedScreen.ROW);
        *///?} else
        super(minecraft, screenWidth, screenHeight - ShedScreen.HEADER - ShedScreen.FOOTER, ShedScreen.HEADER, ShedScreen.ROW);
        this.left = left;
    }

    Row row() {
        Row row = new Row(left);
        addEntry(row);
        return row;
    }

    double scroll() {
        //? if >=1.21.9 {
        return scrollAmount();
        //?} else
        /*return getScrollAmount();*/
    }

    @Override
    public int getRowWidth() {
        // Rows draw their content 2px in from each side
        return ShedScreen.WIDTH + 4;
    }

    //? if >=1.21.9 {
    @Override
    protected int scrollBarX() {
        return left + ShedScreen.WIDTH + 8;
    }
    //?} else {
    /*@Override
    protected int getScrollbarPosition() {
        return left + ShedScreen.WIDTH + 8;
    }
    *///?}

    /** Draws things that aren't widgets (labels, icons) at the row's position. */
    interface Painter {
        void paint(ShedScreen.Canvas canvas, int x, int y);
    }

    public static final class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final int left;
        private final List<AbstractWidget> widgets = new ArrayList<>();
        private final List<Integer> offsets = new ArrayList<>();
        private Painter painter = (canvas, x, y) -> {};

        private Row(int left) {
            this.left = left;
        }

        /** Adds a widget {@code offset} pixels from the grid's left edge. */
        <T extends AbstractWidget> T add(T widget, int offset) {
            widgets.add(widget);
            offsets.add(offset);
            return widget;
        }

        Row paint(Painter painter) {
            this.painter = painter;
            return this;
        }

        private void place(int y) {
            for (int i = 0; i < widgets.size(); i++) {
                widgets.get(i).setX(left + offsets.get(i));
                widgets.get(i).setY(y);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return widgets;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return widgets;
        }

        //? if >=26.1 {
        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            int y = getContentY();
            place(y);
            widgets.forEach(widget -> widget.extractRenderState(graphics, mouseX, mouseY, delta));
            painter.paint(ShedScreen.canvas(graphics), left, y);
        }
        //?} elif >=1.21.9 {
        /*@Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            int y = getContentY();
            place(y);
            widgets.forEach(widget -> widget.render(graphics, mouseX, mouseY, delta));
            painter.paint(ShedScreen.canvas(graphics), left, y);
        }
        *///?} else {
        /*@Override
        public void render(GuiGraphics graphics, int index, int top, int rowLeft, int rowWidth, int rowHeight,
                           int mouseX, int mouseY, boolean hovered, float delta) {
            place(top);
            widgets.forEach(widget -> widget.render(graphics, mouseX, mouseY, delta));
            painter.paint(ShedScreen.canvas(graphics), left, top);
        }
        *///?}
    }
}
