package de.yamayaki.cesium.common.zstd;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdCompressCtx;
import com.github.luben.zstd.ZstdDecompressCtx;
import de.yamayaki.cesium.CesiumMod;
import de.yamayaki.cesium.api.ICompressor;

import java.util.Arrays;

public class ZSTDCompressor implements ICompressor {
    private static final int compressionLevel = CesiumMod.config().compressionLevel();
    private static final boolean usesDictionary = CesiumMod.config().useDictionary();

    private final ZSTDDictionary dictionary = new ZSTDDictionary(compressionLevel);
    private final ThreadLocal<ZSTDContext> ctx = ThreadLocal.withInitial(this::createContext);

    private ZSTDContext createContext() {
        return new ZSTDContext(usesDictionary, compressionLevel, dictionary);
    }

    private static long checkError(long rc) {
        if (Zstd.isError(rc)) {
            throw new IllegalStateException(Zstd.getErrorName(rc));
        }

        return rc;
    }

    @Override
    public byte[] compress(final byte[] input) {
        final byte[] dst = new byte[(int) Zstd.compressBound(input.length)];
        final ZstdCompressCtx ctx = this.ctx.get().compress();

        final int size = (int) checkError(ctx.compress(dst, input));

        return Arrays.copyOfRange(dst, 0, size);
    }

    @Override
    public byte[] decompress(byte[] input) {
        byte[] dst = new byte[(int) checkError(Zstd.getFrameContentSize(input))];
        final long dictId = Zstd.getDictIdFromFrame(input);

        final ZstdDecompressCtx ctx = this.ctx.get().decompress(dictId);

        checkError(ctx.decompress(dst, input));

        return dst;
    }
}
