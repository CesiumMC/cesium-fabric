package de.yamayaki.cesium.api.accessor;

import de.yamayaki.cesium.common.DatabaseSpec;

public interface SpecificationSetter {
    void cesium$setSpec(final DatabaseSpec<?, ?> databaseSpec);
}
