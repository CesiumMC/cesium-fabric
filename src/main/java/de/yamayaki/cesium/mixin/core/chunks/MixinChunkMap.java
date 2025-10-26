package de.yamayaki.cesium.mixin.core.chunks;

import com.mojang.datafixers.DataFixer;
import de.yamayaki.cesium.accessor.DatabaseSetter;
import de.yamayaki.cesium.accessor.DatabaseSource;
import de.yamayaki.cesium.common.lmdb.LMDBInstance;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
//? < 1.21.9 {
/*import net.minecraft.server.level.progress.ChunkProgressListener;
*///?}
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Mixin(ChunkMap.class)
public class MixinChunkMap {
    @Shadow
    @Final
    private PoiManager poiManager;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void setCesiumDB(ServerLevel serverLevel, LevelStorageSource.LevelStorageAccess levelStorageAccess, DataFixer dataFixer, StructureTemplateManager structureTemplateManager, Executor executor, BlockableEventLoop blockableEventLoop, LightChunkGetter lightChunkGetter, ChunkGenerator chunkGenerator, /*? < 1.21.9 {*/ /*ChunkProgressListener chunkProgressListener, *//*?}*/ ChunkStatusUpdateListener chunkStatusUpdateListener, Supplier supplier,/*? if >= 1.21.5 {*/net.minecraft.world.level.TicketStorage ticketStorage, /*?}*/ int i, boolean bl, CallbackInfo ci) {
        LMDBInstance database = ((DatabaseSource) serverLevel).cesium$getStorage();

        ((DatabaseSetter) this.poiManager)
                .cesium$setStorage(database);

        ((DatabaseSetter) this)
                .cesium$setStorage(database);
    }
}