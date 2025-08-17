package de.yamayaki.cesium.common.serializer;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.yamayaki.cesium.MCHelper;
import de.yamayaki.cesium.api.IScannable;
import de.yamayaki.cesium.api.ISerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StreamTagVisitor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CompoundTagSerializer implements ISerializer<CompoundTag>, IScannable<StreamTagVisitor> {
    @Override
    public byte @NotNull [] serialize(final @NotNull CompoundTag input) throws IOException {
        final ByteArrayDataOutput output = ByteStreams.newDataOutput(2048);

        MCHelper.writeNbt(output, input);

        return output.toByteArray();
    }

    @Override
    public @NotNull CompoundTag deserialize(final byte @NotNull [] input) throws IOException {
        return MCHelper.readNbt(ByteStreams.newDataInput(input));
    }

    @Override
    public void scan(final byte @NotNull [] input, final @NotNull StreamTagVisitor scanner) throws IOException {
        MCHelper.parseNbt(ByteStreams.newDataInput(input), scanner);
    }
}
