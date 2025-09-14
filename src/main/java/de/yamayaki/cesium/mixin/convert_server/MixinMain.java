package de.yamayaki.cesium.mixin.convert_server;

import com.llamalad7.mixinextras.sugar.Local;
import de.yamayaki.cesium.MCHelper;
import de.yamayaki.cesium.maintenance.AbstractTask;
import de.yamayaki.cesium.maintenance.tasks.DatabaseConvert;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.Main;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MixinMain {
    @Unique private static OptionSpec<String> cesium$convertOption;

    @Inject(
            method = "main",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Ljoptsimple/OptionParser;accepts(Ljava/lang/String;)Ljoptsimple/OptionSpecBuilder;",
                    ordinal = 0
            ),
            remap = false
    )
    private static void addConvertOption(
            final String[] strings, final CallbackInfo ci, final @Local(ordinal = 0) OptionParser optionParser
    ) {
        cesium$convertOption = optionParser.accepts("cesiumConvertTo").withRequiredArg();
    }

    @Inject(
            method = "main",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/core/LayeredRegistryAccess;compositeAccess()Lnet/minecraft/core/RegistryAccess$Frozen;"
            )
    )
    private static void doConvert(
            final String[] strings, final CallbackInfo ci, final @Local OptionSet optionSet,
            final @Local LevelStorageSource.LevelStorageAccess levelAccess,final @Local RegistryAccess.Frozen registryAccess
    ) {
        if (!optionSet.has(cesium$convertOption)) {
            return;
        }

        final String argument = optionSet.valueOf(cesium$convertOption);

        final AbstractTask.Task task = switch (argument) {
            case "cesium" -> AbstractTask.Task.TO_CESIUM;
            case "anvil" -> AbstractTask.Task.TO_ANVIL;
            default -> throw new IllegalStateException("Unexpected value: " + optionSet.valueOf(cesium$convertOption));
        };

        doWorldConversion(task, levelAccess, registryAccess);
    }

    @Unique
    private static void doWorldConversion(
            final AbstractTask.Task task,
            final LevelStorageSource.LevelStorageAccess levelAccess,
            final RegistryAccess registryAccess
    ) {
        final DatabaseConvert converter = new DatabaseConvert(task, MCHelper.createWorldInfo(registryAccess, levelAccess));
        final Logger logger = converter.logger();

        logger.info("Starting world conversion ...");

        while (converter.running()) {
            final double percentage = Math.floor(converter.percentage() * 100);

            final int total = converter.totalElements();
            final int current = converter.currentElement();

            logger.info("{}% completed ({} / {} elements) ...", percentage, current, total);

            try { Thread.sleep(200L); } catch (final InterruptedException ignored) { }
        }
    }
}
