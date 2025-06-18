package de.yamayaki.cesium.mixin.debug;

import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mixin(IOWorker.class)
public class ChunkExporterMixin {
    @Unique
    private static final Path basePath = Path.of("./exportedChunks/");

    static {
        try {
            if (!Files.isDirectory(basePath)) {
                Files.createDirectories(basePath);
            }
        } catch (IOException e) {
            throw new IllegalStateException("This should not have happened");
        }
    }

    @Inject(method = "runStore", at = @At("HEAD"))
    public void storeChunkOnDisk(ChunkPos chunkPos, IOWorker.PendingStore pendingStore, CallbackInfo ci) {
        try {
            if (pendingStore.data == null || !pendingStore.data.contains("Status")) {
                return;
            }

            NbtIo.write(pendingStore.data, basePath.resolve(chunkPos.toLong() + ".nbt")/*? <= 1.20.1 {*/ /*.toFile() *//*?}*/);
        } catch (IOException ignored) {
        }
    }
}
