package de.yamayaki.cesium.common.lmdb;

import de.yamayaki.cesium.CesiumMod;
import de.yamayaki.cesium.api.database.DatabaseSpec;
import de.yamayaki.cesium.api.database.ICloseableIterator;
import de.yamayaki.cesium.api.database.IKVDatabase;
import de.yamayaki.cesium.api.io.ICompressor;
import de.yamayaki.cesium.api.io.IScannable;
import de.yamayaki.cesium.api.io.ISerializer;
import de.yamayaki.cesium.common.DefaultCompressors;
import de.yamayaki.cesium.common.DefaultSerializers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lmdbjava.*;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static de.yamayaki.cesium.common.lmdb.LMDBInstance.HEX_FORMAT;

public class KVDatabase<K, V> implements IKVDatabase<K, V> {
    private final LMDBInstance storage;
    private final DatabaseSpec<K, V> spec;

    private final Env<byte[]> env;
    private final Dbi<byte[]> dbi;

    private final ISerializer<K> keySerializer;
    private final ISerializer<V> valueSerializer;

    private final ICompressor compressor;

    public KVDatabase(LMDBInstance storage, DatabaseSpec<K, V> spec, boolean compressed) {
        this.storage = storage;
        this.spec = spec;

        this.env = this.storage.env;
        this.dbi = this.env.openDbi(spec.getName(), DbiFlags.MDB_CREATE);

        this.keySerializer = DefaultSerializers.getSerializer(spec.getKeyType());
        this.valueSerializer = DefaultSerializers.getSerializer(spec.getValueType());

        this.compressor = compressed ? DefaultCompressors.ZSTD : DefaultCompressors.NONE;
    }

    @Override
    public V getValue(K key) {
        byte[] buf = this.getBytes(key);

        if (buf == null) {
            return null;
        }

        try {
            return this.valueSerializer.deserialize(buf);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize value", e);
        }
    }

    @Override
    public byte[] getBytes(final K key) {
        ReentrantReadWriteLock lock = this.storage.getLock();
        byte[] keyBuf = null;
        byte[] buf = null;

        lock.readLock()
                .lock();

        try {
            try {
                keyBuf = this.keySerializer.serialize(key);
                buf = this.dbi.get(this.env.txnRead(), keyBuf);
            } catch (final Exception e) {
                if (CesiumMod.config().removeErroringData()) {
                    this.printErroringData(keyBuf, buf);
                    this.forceDelete(key);
                    return null;
                } else {
                    throw new RuntimeException("Failed to deserialize key", e);
                }
            }
        } finally {
            lock.readLock()
                    .unlock();
        }

        if (buf == null) {
            return null;
        }

        try {
            return this.compressor.decompress(buf);
        } catch (Exception e) {
            if (CesiumMod.config().removeErroringData()) {
                this.printErroringData(keyBuf, buf);
                this.forceDelete(key);
                return null;
            } else {
                throw new RuntimeException("Failed to deserialize key", e);
            }
        }
    }

    private void forceDelete(final @NotNull K key) {
        this.storage.getLock()
                .writeLock()
                .lock();

        boolean success = false;

        while (!success) {
            try (final Txn<byte[]> txn = this.env.txnWrite()) {
                this.delete(txn, key);
                txn.commit();

                success = true;
            } catch (Exception e) {
                if (e instanceof LmdbException lE && lE instanceof Env.MapFullException) {
                    this.storage.growMap();
                } else {
                    e.printStackTrace();
                }
            }
        }

        this.storage.getLock()
                .writeLock()
                .unlock();
    }

    public void printErroringData(final byte @Nullable [] key, final byte @Nullable [] value) {
        final String keyInHex = key != null ? HEX_FORMAT.formatHex(key) : System.nanoTime() + "";
        final String valueInHex = HEX_FORMAT.formatHex(value != null ? value : new byte[0]);

        this.storage.logger.info("Dumping data for {}:\nKEY: {}\nVALUE: {}", this.spec.getName(), keyInHex, valueInHex);
    }

    //idea by https://github.com/mo0dss/radon-fabric
    @Override
    @SuppressWarnings("unchecked")
    public <T> void scan(K key, T scanner) {
        if (!(this.valueSerializer instanceof IScannable<?>)) {
            return;
        }

        byte[] bytes = this.getBytes(key);

        if (bytes == null) {
            return;
        }

        try {
            ((IScannable<T>) this.valueSerializer).scan(bytes, scanner);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to scan value", ex);
        }
    }

    public void setDirty() {
        this.storage.isDirty = true;
    }

    @Override
    public ISerializer<K> getKeySerializer() {
        return this.keySerializer;
    }

    @Override
    public ISerializer<V> getValueSerializer() {
        return this.valueSerializer;
    }

    @Override
    public ICompressor getCompressor() {
        return this.compressor;
    }

    public void putValue(Txn<byte[]> txn, K key, byte[] value) {
        try {
            this.dbi.put(txn, this.keySerializer.serialize(key), value);
        } catch (final IOException e) {
            throw new RuntimeException("Could not serialize key", e);
        }
    }

    public void delete(Txn<byte[]> txn, K key) {
        try {
            this.dbi.delete(txn, this.keySerializer.serialize(key));
        } catch (final IOException e) {
            throw new RuntimeException("Could not serialize key", e);
        }
    }

    @Override
    public ICloseableIterator<K> getIterator() {
        final Cursor<byte[]> cursor = this.dbi.openCursor(this.env.txnRead());
        return new CursorIterator<>(cursor, this.keySerializer);
    }

    public Stat getStats() {
        return this.dbi.stat(this.env.txnRead());
    }


    public void close() {
        this.dbi.close();
    }
}
