package de.yamayaki.cesium.accessor;

import de.yamayaki.cesium.common.DatabaseSpec;

public interface SpecificationSetter {
    void cesium$setSpec(final DatabaseSpec<?, ?> databaseSpec);
}
