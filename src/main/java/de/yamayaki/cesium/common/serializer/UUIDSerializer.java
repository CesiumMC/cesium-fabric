package de.yamayaki.cesium.common.serializer;

import de.yamayaki.cesium.api.ISerializer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class UUIDSerializer implements ISerializer<UUID> {
    @Override
    public byte @NotNull [] serialize(final @NotNull UUID input) {
        final byte[] array = new byte[16];

        long least = input.getLeastSignificantBits();
        for (int i = 7; i >= 0; i--) {
            array[i] = (byte) (least & 0xffL);
            least >>= 8;
        }

        long most = input.getMostSignificantBits();
        for (int i = 15; i >= 8; i--) {
            array[i] = (byte) (most & 0xffL);
            most >>= 8;
        }

        return array;
    }

    @Override
    public @NotNull UUID deserialize(final byte @NotNull [] input) {
        final long least = (input[0] & 0xFFL) << 56 | (input[1] & 0xFFL) << 48 | (input[2] & 0xFFL) << 40 | (input[3] & 0xFFL) << 32 | (input[4] & 0xFFL) << 24 | (input[5] & 0xFFL) << 16 | (input[6] & 0xFFL) << 8 | (input[7] & 0xFFL);
        final long most = (input[8] & 0xFFL) << 56 | (input[9] & 0xFFL) << 48 | (input[10] & 0xFFL) << 40 | (input[11] & 0xFFL) << 32 | (input[12] & 0xFFL) << 24 | (input[13] & 0xFFL) << 16 | (input[14] & 0xFFL) << 8 | (input[15] & 0xFFL);

        return new UUID(most, least);
    }
}
