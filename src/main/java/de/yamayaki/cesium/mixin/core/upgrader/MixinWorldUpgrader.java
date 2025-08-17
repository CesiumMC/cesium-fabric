package de.yamayaki.cesium.mixin.core.upgrader;

import com.llamalad7.mixinextras.sugar.Local;
import de.yamayaki.cesium.CesiumMod;
import de.yamayaki.cesium.accessor.DatabaseActions;
import de.yamayaki.cesium.accessor.DatabaseSetter;
import de.yamayaki.cesium.common.lmdb.LMDBInstance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Map;

//? >= 1.20.6 {
@Mixin(net.minecraft.util.worldupdate.WorldUpgrader.AbstractUpgrader.class)
//?} else {
/*@Mixin(net.minecraft.util.worldupdate.WorldUpgrader.class)
*///?}
public abstract class MixinWorldUpgrader {
    //? >= 1.20.6 {
    @Shadow protected abstract boolean processOnePosition(ResourceKey<Level> resourceKey, AutoCloseable autoCloseable, ChunkPos chunkPos);

    @org.spongepowered.asm.mixin.Unique private LMDBInstance tmpDatabase;
    @org.spongepowered.asm.mixin.Unique private de.yamayaki.cesium.common.DatabaseSpec<ChunkPos, net.minecraft.nbt.CompoundTag> tmpSpec;
    @org.spongepowered.asm.mixin.Unique private double chunkCount = 0;

    @Redirect(
            method = "upgrade",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/AutoCloseable;close()V"
            )
    )
    public void cesiumClose(AutoCloseable instance) throws Exception {
        if (instance instanceof DatabaseActions databaseActions) {
            databaseActions.cesium$close();
        }

        instance.close();
    }

    @Redirect(
            method = "upgrade",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/worldupdate/WorldUpgrader$AbstractUpgrader;processOnePosition(Lnet/minecraft/resources/ResourceKey;Ljava/lang/AutoCloseable;Lnet/minecraft/world/level/ChunkPos;)Z"
            )
    )
    public boolean cesiumFlush(net.minecraft.util.worldupdate.WorldUpgrader.AbstractUpgrader<?> instance, ResourceKey<Level> resourceKey, AutoCloseable autoCloseable, ChunkPos chunkPos) {
        if (chunkCount % 1024 == 0 && autoCloseable instanceof DatabaseActions databaseActions) {
            databaseActions.cesium$flush();
        }

        chunkCount++;

        return this.processOnePosition(resourceKey, autoCloseable, chunkPos);
    }


    @Inject(
            method = "getDimensionsToUpgrade",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/worldupdate/WorldUpgrader$AbstractUpgrader;getFilesToProcess(Lnet/minecraft/world/level/chunk/storage/RegionStorageInfo;Ljava/nio/file/Path;)Ljava/util/ListIterator;",
                    shift = At.Shift.BY
            )
    )
    public <T extends AutoCloseable> void cesiumCreate(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<List<net.minecraft.util.worldupdate.WorldUpgrader.DimensionToUpgrade<T>>> cir, @Local java.nio.file.Path path, @Local net.minecraft.world.level.chunk.storage.RegionStorageInfo regionStorageInfo, @Local AutoCloseable autoCloseable) {
        LMDBInstance dbInstance = CesiumMod.openWorldDB(path.getParent());
        tmpDatabase = dbInstance;

        de.yamayaki.cesium.common.DatabaseSpec<ChunkPos, net.minecraft.nbt.CompoundTag> databaseSpec = switch (regionStorageInfo.type()) {
            case "entities" -> de.yamayaki.cesium.common.spec.WorldDatabaseSpecs.ENTITY;
            case "poi" -> de.yamayaki.cesium.common.spec.WorldDatabaseSpecs.POI;
            case "chunk" -> de.yamayaki.cesium.common.spec.WorldDatabaseSpecs.CHUNK_DATA;
            default -> throw new IllegalStateException("Unexpected value: " + regionStorageInfo.type());
        };
        tmpSpec = databaseSpec;

        ((DatabaseSetter) autoCloseable).cesium$setStorage(dbInstance);
        if (autoCloseable instanceof de.yamayaki.cesium.accessor.SpecificationSetter) {
            ((de.yamayaki.cesium.accessor.SpecificationSetter) autoCloseable).cesium$setSpec(databaseSpec);
        }
    }

    @Redirect(method = "getFilesToProcess", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/worldupdate/WorldUpgrader$AbstractUpgrader;getAllChunkPositions(Lnet/minecraft/world/level/chunk/storage/RegionStorageInfo;Ljava/nio/file/Path;)Ljava/util/List;"))
    public List<net.minecraft.util.worldupdate.WorldUpgrader.FileToUpgrade> cesiumGetChunks(net.minecraft.world.level.chunk.storage.RegionStorageInfo regionStorageInfo, java.nio.file.Path path) {
        final Map<String, List<ChunkPos>> regionList = new java.util.HashMap<>();

        try (final de.yamayaki.cesium.common.lmdb.SerializingCursor<ChunkPos> crs = tmpDatabase.getDatabase(tmpSpec).getIterator()) {
            while (crs.hasNext()) {
                final ChunkPos chunkPos = crs.next();
                final String regionKey = chunkPos.getRegionX() + "." + chunkPos.getRegionZ();

                if (!regionList.containsKey(regionKey)) {
                    regionList.put(regionKey, new java.util.ArrayList<>());
                }

                regionList.get(regionKey).add(chunkPos);
            }
        } catch (final Throwable t) {
            throw new RuntimeException("Could not iterate on cursor.", t);
        }

        tmpDatabase = null;
        tmpSpec = null;

        return regionList.values().stream().map(list -> new net.minecraft.util.worldupdate.WorldUpgrader.FileToUpgrade(null, list)).toList();
    }
     
    //?} else {
    /*@Shadow
    @org.spongepowered.asm.mixin.Final
    private net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess levelStorage;
    @Shadow
    private volatile int converted;
    @Shadow
    private volatile int skipped;
    @Shadow
    @org.spongepowered.asm.mixin.Final
    private static org.slf4j.Logger LOGGER;

    @Redirect(
            method = "work",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/ImmutableMap$Builder;build()Lcom/google/common/collect/ImmutableMap;",
                    remap = false
            )
    )
    private com.google.common.collect.ImmutableMap<ResourceKey<Level>, net.minecraft.world.level.chunk.storage.ChunkStorage> injectDatabase(com.google.common.collect.ImmutableMap.Builder<ResourceKey<Level>, net.minecraft.world.level.chunk.storage.ChunkStorage> instance) {
        com.google.common.collect.ImmutableMap<ResourceKey<Level>, net.minecraft.world.level.chunk.storage.ChunkStorage> chunkStorageMap = instance.build();

        for (Map.Entry<ResourceKey<Level>, net.minecraft.world.level.chunk.storage.ChunkStorage> resourceKeyChunkStorageEntry : chunkStorageMap.entrySet()) {
            final LMDBInstance database = CesiumMod.openWorldDB(this.levelStorage.getDimensionPath(resourceKeyChunkStorageEntry.getKey()));

            ((DatabaseSetter) resourceKeyChunkStorageEntry.getValue()).cesium$setStorage(database);
        }

        return chunkStorageMap;
    }

    @Inject(
            method = "work",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/ListIterator;hasNext()Z",
                    remap = false
            )
    )
    private void flushData(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci, @Local net.minecraft.world.level.chunk.storage.ChunkStorage chunkStorage) {
        if ((this.converted + this.skipped) % 10240 == 0) {
            ((DatabaseActions) chunkStorage).cesium$flush();
        }
    }

    @Redirect(
            method = "work",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/storage/ChunkStorage;close()V"
            )
    )
    private void closeDatabase(net.minecraft.world.level.chunk.storage.ChunkStorage instance) {
        ((DatabaseActions) instance).cesium$close();
    }

    /^*
     * @author Yamayaki
     * @reason Cesium
     ^/
    @org.spongepowered.asm.mixin.Overwrite
    private List<ChunkPos> getAllChunkPos(ResourceKey<Level> resourceKey) {
        final de.yamayaki.cesium.maintenance.storage.impl.CesiumWorldStorage.DimensionStorage chunkStorage = new de.yamayaki.cesium.maintenance.storage.impl.CesiumWorldStorage.DimensionStorage(this.levelStorage.getDimensionPath(resourceKey));
        final List<ChunkPos> chunkList = chunkStorage.getAllKeys();

        chunkStorage.close();
        return chunkList;
    }
    *///?}
}
