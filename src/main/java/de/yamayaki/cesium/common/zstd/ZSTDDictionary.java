package de.yamayaki.cesium.common.zstd;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdDictCompress;
import com.github.luben.zstd.ZstdDictDecompress;
import de.yamayaki.cesium.CesiumMod;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import java.io.IOException;
import java.io.InputStream;

public class ZSTDDictionary implements AutoCloseable {
    private static final String[] DICTIONARIES = new String[]{
            "1",
            "2"
    };

    private final long dictionaryToUse;

    private final Long2ObjectMap<ZstdDictCompress> compressMap = new Long2ObjectArrayMap<>();
    private final Long2ObjectMap<ZstdDictDecompress> decompressMap = new Long2ObjectArrayMap<>();

    public ZSTDDictionary(final int compressionLevel) {
        this.dictionaryToUse = this.loadDictionaries(compressionLevel);
    }

    private long loadDictionaries(final int compressionLevel) {
        long dictionaryId = -1;

        for (final String dictionary : DICTIONARIES) {
            dictionaryId = this.loadDictionary(dictionary, compressionLevel);
        }

        return dictionaryId;
    }

    private long loadDictionary(final String name, final int compressionLevel) {
        try {
            final byte[] dictionary = this.loadFromResources(name);
            final long dictionaryId = Zstd.getDictIdFromDict(dictionary);

            final ZstdDictCompress dictCompress = new ZstdDictCompress(dictionary, compressionLevel);
            final ZstdDictDecompress dictDecompress = new ZstdDictDecompress(dictionary);

            this.compressMap.put(dictionaryId, dictCompress);
            this.decompressMap.put(dictionaryId, dictDecompress);

            return dictionaryId;
        } catch (final Throwable t) {
            throw new RuntimeException("Could not load dictionaries", t);
        }
    }

    private byte[] loadFromResources(final String name) throws IOException {
        try (final InputStream inputStream = CesiumMod.class.getResourceAsStream("/cesium/dictionaries/" + name + ".zstd.dict")) {
            if (inputStream == null) {
                throw new IOException("Dictionary file not available: " + name);
            }

            return inputStream.readAllBytes();
        }
    }

    public long[] dictionaries() {
        return this.compressMap.keySet().toLongArray();
    }

    public ZstdDictCompress compressDictionary() {
        return this.compressMap.get(this.dictionaryToUse);
    }

    public ZstdDictDecompress decompressDictionary(final long id) {
        final ZstdDictDecompress dictionary = this.decompressMap.get(id);

        if (dictionary == null) {
            throw new RuntimeException("Could not find dictionary with id " + id);
        }

        return dictionary;
    }

    @Override
    public void close() throws Exception {
        for (final ZstdDictCompress value : this.compressMap.values()) {
            value.close();
        }

        this.compressMap.clear();

        for (final ZstdDictDecompress value : this.decompressMap.values()) {
            value.close();
        }

        this.decompressMap.clear();
    }
}
