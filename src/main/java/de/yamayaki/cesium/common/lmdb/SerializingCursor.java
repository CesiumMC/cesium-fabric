package de.yamayaki.cesium.common.lmdb;

import de.yamayaki.cesium.api.ISerializer.KeySerializer;
import org.lmdbjava.Cursor;

import java.util.Iterator;

public class SerializingCursor<K> implements Iterator<K>, AutoCloseable {
    private final Cursor<byte[]> cursor;
    private final KeySerializer<K> serializer;

    private boolean hasNext;

    public SerializingCursor(final Cursor<byte[]> cursor, final KeySerializer<K> serializer) {
        this.cursor = cursor;
        this.serializer = serializer;

        this.hasNext = this.cursor.first();
    }

    @Override
    public boolean hasNext() {
        return this.hasNext;
    }

    @Override
    public K next() {
        final K key = this.serializer.deserialize(this.cursor.key());

        this.hasNext = this.cursor.next();

        return key;
    }

    @Override
    public void close() throws Exception {
        this.cursor.close();
    }
}
