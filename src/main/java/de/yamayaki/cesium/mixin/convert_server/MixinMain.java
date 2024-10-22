package de.yamayaki.cesium.mixin.convert_server;

import com.llamalad7.mixinextras.sugar.Local;
import de.yamayaki.cesium.maintenance.AbstractTask;
import de.yamayaki.cesium.maintenance.tasks.DatabaseConvert;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.Main;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MixinMain {
    @Unique
    private static OptionSpec<Void> cesium$convertOptionAnvil;

    @Unique
    private static OptionSpec<Void> cesium$convertOptionCesium;

    @Inject(
            method = "main",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Ljoptsimple/OptionParser;accepts(Ljava/lang/String;)Ljoptsimple/OptionSpecBuilder;",
                    ordinal = 0
            ),
            remap = false
    )
    private static void addConvertOption(String[] strings, CallbackInfo ci, @Local(ordinal = 0) OptionParser optionParser) {
        cesium$convertOptionAnvil = optionParser.accepts("cesiumConvertToAnvil");
        cesium$convertOptionCesium = optionParser.accepts("cesiumConvertToCesium");
    }

    @Inject(
            method = "main",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/core/LayeredRegistryAccess;compositeAccess()Lnet/minecraft/core/RegistryAccess$Frozen;",
                    shift = At.Shift.AFTER
            )
    )
    private static void doConvert(String[] strings, CallbackInfo ci, @Local OptionSet optionSet, @Local LevelStorageSource.LevelStorageAccess levelAccess, @Local RegistryAccess.Frozen registryAccess) {
        final boolean convertAnvil;

        if ((convertAnvil = optionSet.has(cesium$convertOptionAnvil)) || optionSet.has(cesium$convertOptionCesium)) {
            final AbstractTask.Task task = convertAnvil ? AbstractTask.Task.TO_ANVIL : AbstractTask.Task.TO_CESIUM;
            doWorldConversion(task, levelAccess, registryAccess);
        }
    }

    @Unique
    private static void doWorldConversion(final AbstractTask.Task task, final LevelStorageSource.LevelStorageAccess levelAccess, final RegistryAccess registryAccess) {
        var databaseConvert = new DatabaseConvert(task, levelAccess, registryAccess);
        var logger = databaseConvert.logger();

        logger.info("Starting world conversion ...");

        String previousStatus = null;
        String currentStatus;

        while (databaseConvert.running()) {
            currentStatus = databaseConvert.status();

            if (currentStatus != null && !currentStatus.equals(previousStatus)) {
                previousStatus = currentStatus;
                logger.info(currentStatus);
            }

            logger.info("{}% completed ({} / {} elements) ...", Math.floor(databaseConvert.percentage() * 100), databaseConvert.currentElement(), databaseConvert.totalElements());

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
