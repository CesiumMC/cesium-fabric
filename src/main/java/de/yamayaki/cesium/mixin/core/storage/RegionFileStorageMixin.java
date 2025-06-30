package de.yamayaki.cesium.mixin.core.storage;

import de.yamayaki.cesium.accessor.RawAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@Mixin(RegionFileStorage.class)
public abstract class RegionFileStorageMixin implements RawAccess {
    @Shadow
    protected abstract RegionFile getRegionFile(ChunkPos chunkPos) throws IOException;

    @Override
    public byte[] cesium$getBytes(final ChunkPos chunkPos) throws IOException {
        final RegionFile regionFile = this.getRegionFile(chunkPos);

        try (final DataInputStream dos = regionFile.getChunkDataInputStream(chunkPos)) {
            if (dos == null) {
                return null;
            }

            return dos.readAllBytes();
        }
    }

    @Override
    public void cesium$putBytes(final ChunkPos chunkPos, byte[] bytes) throws IOException {
        final RegionFile regionFile = this.getRegionFile(chunkPos);

        if (bytes == null) {
            regionFile.clear(chunkPos);
        } else {
            try (final DataOutputStream dos = regionFile.getChunkDataOutputStream(chunkPos)) {
                dos.write(bytes);
            }
        }
    }
}
