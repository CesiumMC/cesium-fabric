package de.yamayaki.cesium.common.serializer;

import de.yamayaki.cesium.api.ISerializer.KeySerializer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static de.yamayaki.cesium.SerializationHelper.readLong;
import static de.yamayaki.cesium.SerializationHelper.writeLong;

public class UUIDSerializer implements KeySerializer<UUID> {
    @Override
    public byte @NotNull [] serialize(final @NotNull UUID input) {
        final byte[] array = new byte[16];

        writeLong(array, 0, input.getLeastSignificantBits());
        writeLong(array, 1, input.getMostSignificantBits());

        return array;
    }

    @Override
    public @NotNull UUID deserialize(final byte @NotNull [] input) {
        final long least = readLong(input, 0);
        final long most = readLong(input, 1);

        return new UUID(most, least);
    }
}
