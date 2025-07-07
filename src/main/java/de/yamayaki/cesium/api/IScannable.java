package de.yamayaki.cesium.api;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface IScannable<T> {
    void scan(final byte @NotNull [] input, final @NotNull T scanner) throws IOException;
}
