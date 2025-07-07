package de.yamayaki.cesium.maintenance;

import java.nio.file.Path;

public record WorldInfo(Path root, DimensionInfo[] levels) {
    public record DimensionInfo(Path path, String name) {}
}
