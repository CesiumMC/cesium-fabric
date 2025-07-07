package de.yamayaki.cesium.api;

import org.jetbrains.annotations.NotNull;

public interface ICompressor {
    byte @NotNull [] compress(final byte @NotNull [] input);

    byte @NotNull [] decompress(final byte @NotNull [] input);
}
