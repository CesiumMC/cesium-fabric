package de.yamayaki.cesium.common.serializer;

import de.yamayaki.cesium.api.ISerializer.KeySerializer;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

import static de.yamayaki.cesium.SerializationHelper.readInt;
import static de.yamayaki.cesium.SerializationHelper.writeInt;

public class ChunkPosSerializer implements KeySerializer<ChunkPos> {
    @Override
    public byte @NotNull [] serialize(final @NotNull ChunkPos input) {
        final byte[] bytes = new byte[8];

        writeInt(bytes, 0, input.x);
        writeInt(bytes, 1, input.z);

        return bytes;
    }

    @Override
    public @NotNull ChunkPos deserialize(final byte @NotNull [] input) {
        final int x = readInt(input, 0);
        final int z = readInt(input, 1);

        return new ChunkPos(x, z);
    }
}
