package de.yamayaki.cesium.common.spec;

import de.yamayaki.cesium.common.DatabaseSpec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

public class WorldDatabaseSpecs {
    public static final DatabaseSpec<ChunkPos, CompoundTag> CHUNK_DATA =
            new DatabaseSpec<>("chunks", ChunkPos.class, CompoundTag.class, 8 * 1024 * 1024);

    public static final DatabaseSpec<ChunkPos, CompoundTag> POI =
            new DatabaseSpec<>("poi", ChunkPos.class, CompoundTag.class, 512 * 1024);

    public static final DatabaseSpec<ChunkPos, CompoundTag> ENTITY =
            new DatabaseSpec<>("entity", ChunkPos.class, CompoundTag.class, 512 * 1024);
}
