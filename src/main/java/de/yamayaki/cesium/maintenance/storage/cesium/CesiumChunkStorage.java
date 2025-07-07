package de.yamayaki.cesium.maintenance.storage.cesium;

import de.yamayaki.cesium.CesiumMod;
import de.yamayaki.cesium.common.lmdb.SerializingCursor;
import de.yamayaki.cesium.common.lmdb.LMDBInstance;
import de.yamayaki.cesium.common.spec.WorldDatabaseSpecs;
import de.yamayaki.cesium.maintenance.storage.IWorldStorage;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lmdbjava.LmdbException;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CesiumChunkStorage implements IWorldStorage.IChunkStorage {
    private final Logger logger;
    private final LMDBInstance database;

    public CesiumChunkStorage(final Logger logger, final Path basePath) {
        this.logger = logger;
        this.database = CesiumMod.openWorldDB(basePath);
    }

    @Override
    public List<ChunkPos> getAllKeys() {
        final List<ChunkPos> list = new ArrayList<>();

        try (final SerializingCursor<ChunkPos> crs = this.database.getDatabase(WorldDatabaseSpecs.CHUNK_DATA).getIterator()) {
            while (crs.hasNext()) {
                list.add(crs.next());
            }
        } catch (final Throwable t) {
            throw new RuntimeException("Could not iterate on cursor.", t);
        }

        return list;
    }

    @Override
    public void flush() {
        try {
            this.database.flushChanges();
        } catch (LmdbException lmdbException) {
            this.logger.error("Failed to flush data", lmdbException);
        }
    }

    @Override
    public void close() {
        this.flush();
        this.database.close();
    }

    @Override
    public void setChunk(final @NotNull ChunkPos chunkPos, final byte @Nullable [] bytes) {
        this.database.getDatabase(WorldDatabaseSpecs.CHUNK_DATA).stageChangeRaw(chunkPos, bytes);
    }

    @Override
    public byte[] getChunk(final @NotNull ChunkPos chunkPos) {
        return this.database.getDatabase(WorldDatabaseSpecs.CHUNK_DATA).getValueRaw(chunkPos);
    }

    @Override
    public void setPOI(final ChunkPos chunkPos, final byte @Nullable [] bytes) {
        this.database.getDatabase(WorldDatabaseSpecs.POI).stageChangeRaw(chunkPos, bytes);
    }

    @Override
    public byte[] getPOI(final ChunkPos chunkPos) {
        return this.database.getDatabase(WorldDatabaseSpecs.POI).getValueRaw(chunkPos);
    }

    @Override
    public void setEntity(final ChunkPos chunkPos, final byte @Nullable [] bytes) {
        this.database.getDatabase(WorldDatabaseSpecs.ENTITY).stageChangeRaw(chunkPos, bytes);
    }

    @Override
    public byte[] getEntity(final ChunkPos chunkPos) {
        return this.database.getDatabase(WorldDatabaseSpecs.ENTITY).getValueRaw(chunkPos);
    }

    @Override
    public String toString() {
        return "LmdbChunk";
    }
}
