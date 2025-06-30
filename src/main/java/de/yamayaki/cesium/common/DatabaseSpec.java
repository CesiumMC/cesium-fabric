package de.yamayaki.cesium.common;

import org.jetbrains.annotations.NotNull;

public record DatabaseSpec<K, V>(String name, Class<K> key, Class<V> value, int initialSize) {
    @Override
    public @NotNull String toString() {
        return String.format("DatabaseSpec{name=%s, key=%s, value=%s}@%s", this.name, this.key.getName(), this.value.getName(), this.hashCode());
    }
}
