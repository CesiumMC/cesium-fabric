package de.yamayaki.cesium.common.spec;

import de.yamayaki.cesium.common.DatabaseSpec;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class PlayerDatabaseSpecs {
    public static final DatabaseSpec<UUID, String> ADVANCEMENTS =
            new DatabaseSpec<>("advancements", UUID.class, String.class, 128 * 1024);

    public static final DatabaseSpec<UUID, String> STATISTICS =
            new DatabaseSpec<>("statistics", UUID.class, String.class, 128 * 1024);

    public static final DatabaseSpec<UUID, CompoundTag> PLAYER_DATA =
            new DatabaseSpec<>("player_data", UUID.class, CompoundTag.class, 128 * 1024);
}
