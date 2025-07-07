package de.yamayaki.cesium.maintenance.screens;

import de.yamayaki.cesium.maintenance.AbstractTask;
import de.yamayaki.cesium.maintenance.WorldInfo;
import de.yamayaki.cesium.maintenance.tasks.DatabaseCompact;
import de.yamayaki.cesium.maintenance.tasks.DatabaseConvert;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

public class CesiumWorkScreen extends Screen {
    private final BooleanConsumer callback;
    private final AbstractTask cesiumTask;

    public CesiumWorkScreen(AbstractTask.Task task, WorldInfo worldInfo, BooleanConsumer callback) {
        super(Component.literal("Working on Cesium task"));

        this.callback = callback;
        this.cesiumTask = switch (task) {
            case TO_ANVIL, TO_CESIUM -> new DatabaseConvert(task, worldInfo);
            case COMPACT -> new DatabaseCompact(worldInfo);
        };
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> {
            this.cesiumTask.cancelTask();
            this.callback.accept(false);
        }).bounds(this.width / 2 - 100, this.height / 4 + 150, 200, 20).build());
    }

    @Override
    public void tick() {
        if (!this.cesiumTask.running()) {
            this.callback.accept(true);
        }
    }

    @Override
    public void onClose() {
        this.callback.accept(false);
    }

    @Override
    public void removed() {
        this.cesiumTask.cancelTask();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        //? <= 1.20.1 {
        /*this.renderDirtBackground(guiGraphics);
         *///?} else {
        super.render(guiGraphics, i, j, f);
        //?}

        final int textColor = CommonColors.WHITE;
        final int textOffset = 14;

        final int offsetX = this.width / 2;
        final int startX = offsetX - 150;
        final int endX = offsetX + 150;

        guiGraphics.drawCenteredString(this.font, this.title, offsetX, 20, textColor);

        final Component[] drawables = new Component[]{
                Component.literal(String.format("Level: %s", this.cesiumTask.levelName())),
                Component.literal(String.format("Progress: %s / %s", this.cesiumTask.currentElement(), this.cesiumTask.totalElements()))
        };

        for (int index = 0; index < drawables.length; index++) {
            guiGraphics.drawString(this.font, drawables[index], startX, 40 + ((index + 1) * textOffset), textColor);
        }

        final int statusOffset = 40 + ((drawables.length + 2) * textOffset);
        guiGraphics.drawCenteredString(this.font, this.cesiumTask.status(), offsetX, statusOffset, textColor);

        final int progressOffset = statusOffset + textOffset;
        final int barEnd = (int) Math.floor(this.cesiumTask.percentage() * (endX - startX));

        guiGraphics.fill(startX - 1, progressOffset - 1, endX + 1, progressOffset + textOffset + 1, -16777216);
        guiGraphics.fill(startX, progressOffset, startX + barEnd, progressOffset + textOffset, -13408734);

        //? <= 1.20.1 {
        /*super.render(guiGraphics, i, j, f);
        *///?}
    }
}
