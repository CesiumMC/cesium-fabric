package de.yamayaki.cesium.api;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface ISerializer<T> {
    byte @NotNull[] serialize(final @NotNull T input) throws IOException;

    @NotNull T deserialize(final byte @NotNull [] input) throws IOException;
}
