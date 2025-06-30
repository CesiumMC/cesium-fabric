package de.yamayaki.cesium.accessor;

import de.yamayaki.cesium.common.lmdb.LMDBInstance;

public interface DatabaseSource {
    LMDBInstance cesium$getStorage();
}
