package de.yamayaki.cesium.mixin.gui;

import de.yamayaki.cesium.maintenance.screens.CesiumTasksScreen;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditWorldScreen.class)
@MixinEnvironment(type = MixinEnvironment.Env.CLIENT)
public abstract class MixinEditWorldScreen extends Screen {
    @Shadow @Final private LevelStorageSource.LevelStorageAccess levelAccess;
    @Shadow @Final private BooleanConsumer callback;
    //? >= 1.20.4 {
    @Shadow @Final private net.minecraft.client.gui.layouts.LinearLayout layout;
    //?}

    protected MixinEditWorldScreen(Component component) {
        super(component);
    }

    //? >= 1.20.4 {
    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 9
            )
    )
    public void reInit(CallbackInfo ci) {
        this.layout.addChild(Button.builder(Component.literal("Cesium Maintenance"), buttonx -> {
            this.minecraft.setScreen(new CesiumTasksScreen(this.levelAccess, this.callback));
        }).width(200).build());
    }
    //?} else {
    /*// Half the width of the optimize world button ...
    @org.spongepowered.asm.mixin.injection.ModifyArg(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/Button$Builder;bounds(IIII)Lnet/minecraft/client/gui/components/Button$Builder;",
                    shift = At.Shift.BY,
                    ordinal = 5
            ),
            index = 2
    )
    private int injected(int x) {
        return x / 2;
    }

    // ... and add our own button next to it.
    @Inject(method = "init", at = @At("RETURN"))
    public void reInit(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(Component.literal("Cesium Maintenance"), buttonx -> {
            assert this.minecraft != null;
            this.minecraft.setScreen(new CesiumTasksScreen(this.levelAccess, this.callback));
        }).bounds(this.width / 2, this.height / 4 + 96 + 5, 100, 20).build());
    }
    *///?}
}
