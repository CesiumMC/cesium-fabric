package de.yamayaki.cesium.common;

import de.yamayaki.cesium.api.ICompressor;
import de.yamayaki.cesium.common.zstd.ZSTDCompressor;

public class DefaultCompressors {
    public static final ICompressor NONE = new ICompressor() {
        @Override
        public byte[] compress(byte[] input) {
            return input;
        }

        @Override
        public byte[] decompress(byte[] input) {
            return input;
        }
    };

    public static final ICompressor ZSTD = new ZSTDCompressor();
}
