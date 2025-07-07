package de.yamayaki.cesium.maintenance.storage;

import org.jetbrains.annotations.NotNull;

public interface ICopyable<Key> {
    void copyTo(final @NotNull Key key, final @NotNull ICopyable<Key> copyable);
}
