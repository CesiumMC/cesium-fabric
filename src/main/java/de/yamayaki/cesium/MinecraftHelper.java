package de.yamayaki.cesium;

import de.yamayaki.cesium.maintenance.WorldInfo;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.concurrent.Executor;

public class MinecraftHelper {
    public static void writeNbt(final @NotNull DataOutput output, final @NotNull CompoundTag input) throws IOException {
        NbtIo.write(input, output);
    }

    public static CompoundTag readNbt(final @NotNull DataInput input) throws IOException {
        return NbtIo.read(input);
    }

    public static void parseNbt(final @NotNull DataInput input, final @NotNull StreamTagVisitor scanner) throws IOException {
        NbtIo.parse(input, scanner/*? >= 1.20.4 {*/, net.minecraft.nbt.NbtAccounter.unlimitedHeap() /*?}*/);
    }

    /*
     * See vanilla code net.minecraft.client.gui.screens.worldselection.OptimizeWorldScreen.create(...);
     */
    public static WorldInfo createWorldInfo(final @NotNull Minecraft minecraft, final @NotNull LevelStorageSource.LevelStorageAccess levelAccess) {
        final WorldOpenFlows openFlow = minecraft.createWorldOpenFlows();
        final PackRepository packRepo = ServerPacksSource.createPackRepository(levelAccess);

        try (final WorldStem stem = openFlow.loadWorldStem(levelAccess /*? >= 1.20.4 {*/.getDataTag()/*?}*/, false/*? >= 1.20.4 {*/, packRepo/*?}*/)) {
            return createWorldInfo(stem.registries().compositeAccess(), levelAccess);
        } catch (final Throwable t) {
            throw new RuntimeException("Failed to load world stem, can not access registry!", t);
        }
    }

    public static WorldInfo createWorldInfo(final @NotNull RegistryAccess registryAccess, final @NotNull LevelStorageSource.LevelStorageAccess levelAccess) {
        final WorldInfo.DimensionInfo[] dimensions = registryAccess
                //? if >= 1.21.2 {
                .lookupOrThrow(Registries.LEVEL_STEM)
                //?} else {
                /*.registryOrThrow(Registries.LEVEL_STEM)
                 *///?}
                .registryKeySet()
                .stream().map(Registries::levelStemToLevel)
                .map(level ->
                        new WorldInfo.DimensionInfo(levelAccess.getDimensionPath(level), level.location().toString())
                )
                .toArray(WorldInfo.DimensionInfo[]::new);

        return new WorldInfo(levelAccess.getDimensionPath(Level.OVERWORLD), dimensions);
    }

    public static Executor executorPool() {
        return Util.backgroundExecutor();
    }
}
