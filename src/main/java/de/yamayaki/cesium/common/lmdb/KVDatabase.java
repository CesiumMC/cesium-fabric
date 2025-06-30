package de.yamayaki.cesium.common.lmdb;

import de.yamayaki.cesium.common.DatabaseSpec;
import de.yamayaki.cesium.api.io.ICompressor;
import de.yamayaki.cesium.api.io.IScannable;
import de.yamayaki.cesium.api.io.ISerializer;
import de.yamayaki.cesium.common.DefaultCompressors;
import de.yamayaki.cesium.common.DefaultSerializers;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import org.jetbrains.annotations.Nullable;
import org.lmdbjava.Cursor;
import org.lmdbjava.Dbi;
import org.lmdbjava.DbiFlags;
import org.lmdbjava.Env;
import org.lmdbjava.Stat;
import org.lmdbjava.Txn;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class KVDatabase<K, V> {
    private final Object2ReferenceMap<K, byte[]> pending = new Object2ReferenceOpenHashMap<>();
    private final Object2ReferenceMap<K, byte[]> snapshot = new Object2ReferenceOpenHashMap<>();

    private final LMDBInstance storage;

    private final Env<byte[]> env;
    private final Dbi<byte[]> dbi;

    private final ISerializer<K> keySerializer;
    private final ISerializer<V> valueSerializer;

    private final ICompressor compressor;

    public KVDatabase(final LMDBInstance storage, final DatabaseSpec<K, V> spec, final boolean isUncompressed) {
        this.storage = storage;

        this.env = this.storage.env;
        this.dbi = this.env.openDbi(spec.name(), DbiFlags.MDB_CREATE);

        this.keySerializer = DefaultSerializers.getSerializer(spec.key());
        this.valueSerializer = DefaultSerializers.getSerializer(spec.value());

        this.compressor = isUncompressed ? DefaultCompressors.NONE : DefaultCompressors.ZSTD;
    }

    public V getValue(K key) {
        byte[] buf = this.getValueRaw(key);

        if (buf == null) {
            return null;
        }

        try {
            return this.valueSerializer.deserialize(buf);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize value", e);
        }
    }

    public byte[] getValueRaw(final K key) {
        ReentrantReadWriteLock lock = this.storage.getLock();
        byte[] buf;

        lock.readLock()
                .lock();

        try {
            try {
                buf = this.dbi.get(this.env.txnRead(), this.keySerializer.serialize(key));
            } catch (final IOException e) {
                throw new RuntimeException("Failed to deserialize key", e);
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
            throw new RuntimeException("Failed to decompress value", e);
        }
    }

    //idea by https://github.com/mo0dss/radon-fabric
    @SuppressWarnings("unchecked")
    public <T> void scan(K key, T scanner) {
        if (!(this.valueSerializer instanceof IScannable<?>)) {
            return;
        }

        byte[] bytes = this.getValueRaw(key);

        if (bytes == null) {
            return;
        }

        try {
            ((IScannable<T>) this.valueSerializer).scan(bytes, scanner);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to scan value", ex);
        }
    }

    public void stageChange(K key, V value) {
        try {
            byte[] data = null;

            if (value != null) {
                data = this.valueSerializer.serialize(value);
            }

            this.stageChangeRaw(key, data);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't serialize value", e);
        }
    }

    public void stageChangeRaw(final K key, final byte[] value) {
        byte[] data = null;

        if (value != null) {
            data = this.compressor.compress(value);
        }

        synchronized (this.pending) {
            this.pending.put(key, data);
        }

        this.storage.dirty = true;
    }

    public CursorIterator<K> getIterator() {
        final Txn<byte[]> txn = this.env.txnRead();
        final Cursor<byte[]> cursor = this.dbi.openCursor(txn);

        return new CursorIterator<>(cursor, this.keySerializer);
    }

    public Stat getStats() {
        return this.dbi.stat(this.env.txnRead());
    }

    void prepareCommit() {
        synchronized (this.pending) {
            this.snapshot.putAll(this.pending);
            this.pending.clear();
        }
    }

    void addChanges(Txn<byte[]> txn) {
        for (Object2ReferenceMap.Entry<K, byte[]> entry : this.snapshot.object2ReferenceEntrySet()) {
            this.dbiPutDelete(txn, entry.getKey(), entry.getValue());
        }
    }

    void cleanupCommit() {
        this.snapshot.clear();
    }

    private void dbiPutDelete(final Txn<byte[]> txn, final K key, final byte @Nullable [] value) {
        try {
            final byte[] serializedKey = this.keySerializer.serialize(key);

            if (value == null) {
                this.dbi.delete(txn, serializedKey);
            } else {
                this.dbi.put(txn, serializedKey, value);
            }
        } catch (final IOException e) {
            throw new RuntimeException("Could not serialize key", e);
        }
    }

    public void close() {
        this.dbi.close();
    }
}
