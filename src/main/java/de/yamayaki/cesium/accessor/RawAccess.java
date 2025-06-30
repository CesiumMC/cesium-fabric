package de.yamayaki.cesium.accessor;

import net.minecraft.world.level.ChunkPos;

import java.io.IOException;

public interface RawAccess {
    byte[] cesium$getBytes(final ChunkPos chunkPos) throws IOException;

    void cesium$putBytes(final ChunkPos chunkPos, byte[] bytes) throws IOException;
}
