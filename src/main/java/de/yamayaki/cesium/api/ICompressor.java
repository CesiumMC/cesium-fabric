package de.yamayaki.cesium.api;

public interface ICompressor {
    byte[] compress(final byte[] input);

    byte[] decompress(final byte[] input);
}
