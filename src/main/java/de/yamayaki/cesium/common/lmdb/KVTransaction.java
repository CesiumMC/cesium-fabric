package de.yamayaki.cesium.common.lmdb;

import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import org.lmdbjava.Txn;

import java.io.IOException;

public class KVTransaction<K, V> {
    private final KVDatabase<K, V> storage;

    private final Object2ReferenceMap<K, byte[]> pending = new Object2ReferenceOpenHashMap<>();
    private final Object2ReferenceMap<K, byte[]> snapshot = new Object2ReferenceOpenHashMap<>();

    public KVTransaction(KVDatabase<K, V> storage) {
        this.storage = storage;
    }

    public void add(K key, V value) {
        try {
            byte[] data = null;

            if (value != null) {
                data = this.storage.getValueSerializer()
                        .serialize(value);
            }

            this.addBytes(key, data);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't serialize value", e);
        }
    }

    public void addBytes(final K key, final byte[] value) {
        byte[] data = null;

        if (value != null) {
            data = this.storage.getCompressor()
                    .compress(value);
        }

        synchronized (this.pending) {
            this.pending.put(key, data);
        }

        this.storage.setDirty();
    }

    void createSnapshot() {
        synchronized (this.pending) {
            this.snapshot.putAll(this.pending);
            this.pending.clear();
        }
    }

    void addChanges(Txn<byte[]> txn) {
        for (Object2ReferenceMap.Entry<K, byte[]> entry : this.snapshot.object2ReferenceEntrySet()) {
            if (entry.getValue() != null) {
                this.storage.putValue(txn, entry.getKey(), entry.getValue());
            } else {
                this.storage.delete(txn, entry.getKey());
            }
        }
    }

    void clearSnapshot() {
        this.snapshot.clear();
    }
}
