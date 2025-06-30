package de.yamayaki.cesium.mixin.core.storage;

import de.yamayaki.cesium.accessor.DatabaseActions;
import de.yamayaki.cesium.accessor.DatabaseSetter;
import de.yamayaki.cesium.accessor.SpecificationSetter;
import de.yamayaki.cesium.common.DatabaseSpec;
import de.yamayaki.cesium.common.lmdb.LMDBInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;

@Mixin(IOWorker.class)
public abstract class MixinIOWorker implements DatabaseSetter, SpecificationSetter, DatabaseActions {
    @Mutable @Shadow @Final private RegionFileStorage storage;

    @Unique private LMDBInstance database;
    @Unique private DatabaseSpec<ChunkPos, CompoundTag> databaseSpec;
    //? >= 1.21.1 {
    @Unique private net.minecraft.world.level.chunk.storage.RegionStorageInfo storageInfo;


    @org.spongepowered.asm.mixin.injection.Inject(method = "<init>", at = @At("TAIL"))
    public void storeStorageInfo(net.minecraft.world.level.chunk.storage.RegionStorageInfo regionStorageInfo, java.nio.file.Path path, boolean bl, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        this.storageInfo = regionStorageInfo;
    }

    /**
     * @author Yamayaki
     * @reason Return from field.
     */
    @org.spongepowered.asm.mixin.Overwrite
    public net.minecraft.world.level.chunk.storage.RegionStorageInfo storageInfo() {
        return this.storageInfo;
    }
    //?}

    /**
     * @author Yamayaki
     * @see IOWorker#loadAsync(ChunkPos)
     */
    @Redirect(
            method = "method_27943",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/storage/RegionFileStorage;read(Lnet/minecraft/world/level/ChunkPos;)Lnet/minecraft/nbt/CompoundTag;"
            )
    )
    private CompoundTag cesium$read(RegionFileStorage instance, ChunkPos chunkPos) throws IOException {
        if (instance == null) {
            return this.database
                    .getDatabase(this.databaseSpec)
                    .getValue(chunkPos);
        } else {
            return instance.read(chunkPos);
        }
    }

    /**
     * @author Yamayaki
     * @see IOWorker#scanChunk(ChunkPos, StreamTagVisitor)
     */
    @Redirect(
            method = "method_39801",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/storage/RegionFileStorage;scanChunk(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/StreamTagVisitor;)V"
            )
    )
    private void cesium$scanChunk(RegionFileStorage instance, ChunkPos chunkPos, StreamTagVisitor streamTagVisitor) throws IOException {
        if (instance == null) {
            this.database
                    .getDatabase(this.databaseSpec)
                    .scan(chunkPos, streamTagVisitor);
        } else {
            instance.scanChunk(chunkPos, streamTagVisitor);
        }
    }

    /**
     * @author Yamayaki
     * @see IOWorker#synchronize(boolean)
     */
    @Redirect(
            method = "method_27946",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/storage/RegionFileStorage;flush()V"
            )
    )
    private void cesium$flush(RegionFileStorage instance) throws IOException {
        if (instance != null) {
            instance.flush();
        }
    }

    @Redirect(
            method = "runStore",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/storage/RegionFileStorage;write(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/CompoundTag;)V"
            )
    )
    private void cesium$write(RegionFileStorage instance, ChunkPos chunkPos, CompoundTag compoundTag) throws IOException {
        if (instance == null) {
            this.database
                    .getDatabase(this.databaseSpec)
                    .stageChange(chunkPos, compoundTag);
        } else {
            instance.write(chunkPos, compoundTag);
        }
    }

    @Redirect(method = "close", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/storage/RegionFileStorage;close()V"))
    private void cesium$close(RegionFileStorage instance) throws IOException {
        if (instance != null) {
            instance.close();
        }
    }

    public void cesium$flush() {
        this.database.flushChanges();
    }

    public void cesium$close() {
        this.database.close();
    }

    @Override
    public void cesium$setStorage(LMDBInstance dbInstance) {
        this.database = dbInstance;

        try {
            if (this.storage != null) {
                this.storage.close();
                this.storage = null;
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void cesium$setSpec(DatabaseSpec<?, ?> databaseSpec) {
        this.databaseSpec = (DatabaseSpec<ChunkPos, CompoundTag>) databaseSpec;
    }
}
