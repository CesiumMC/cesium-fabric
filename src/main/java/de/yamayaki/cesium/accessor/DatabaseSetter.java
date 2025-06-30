package de.yamayaki.cesium.accessor;

import de.yamayaki.cesium.common.lmdb.LMDBInstance;

public interface DatabaseSetter {
    void cesium$setStorage(final LMDBInstance dbInstance);
}
