package de.yamayaki.cesium.mixin.core.chunks;

import de.yamayaki.cesium.CesiumMod;
import de.yamayaki.cesium.api.accessor.DatabaseSource;
import de.yamayaki.cesium.api.database.IDBInstance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(ServerLevel.class)
public class MixinServerLevel implements DatabaseSource {
    @Unique
    private IDBInstance database;

    @Inject(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;getFixerUpper()Lcom/mojang/datafixers/DataFixer;",
                    shift = At.Shift.AFTER
            )
    )
    public void initCesiumChunkStorage(MinecraftServer minecraftServer, Executor executor, LevelStorageSource.LevelStorageAccess levelStorageAccess, ServerLevelData serverLevelData, ResourceKey resourceKey, LevelStem levelStem, ChunkProgressListener chunkProgressListener, boolean bl, long l, List list, boolean bl2, RandomSequences randomSequences, CallbackInfo ci) {
        this.database = CesiumMod.openWorldDB(levelStorageAccess.getDimensionPath(resourceKey));
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void postClose(CallbackInfo ci) {
        this.database.close();
    }

    @Override
    public IDBInstance cesium$getStorage() {
        return this.database;
    }
}
