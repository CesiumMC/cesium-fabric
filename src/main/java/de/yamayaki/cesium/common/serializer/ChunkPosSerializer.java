package de.yamayaki.cesium.common.serializer;

import de.yamayaki.cesium.api.ISerializer;
import net.minecraft.world.level.ChunkPos;

public class ChunkPosSerializer implements ISerializer<ChunkPos> {
    @Override
    public byte[] serialize(final ChunkPos input) {
        final int x = input.x;
        final int z = input.z;

        return new byte[]{
                (byte) (x >> 24), (byte) (x >> 16), (byte) (x >> 8), (byte) x,
                (byte) (z >> 24), (byte) (z >> 16), (byte) (z >> 8), (byte) z
        };
    }

    @Override
    public ChunkPos deserialize(final byte[] input) {
        final int x = input[0] << 24 | (input[1] & 0xFF) << 16 | (input[2] & 0xFF) << 8 | (input[3] & 0xFF);
        final int z = input[4] << 24 | (input[5] & 0xFF) << 16 | (input[6] & 0xFF) << 8 | (input[7] & 0xFF);

        return new ChunkPos(x, z);
    }
}
