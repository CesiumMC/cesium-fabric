package de.yamayaki.cesium.maintenance.storage.cesium;

import de.yamayaki.cesium.CesiumMod;
import de.yamayaki.cesium.api.database.ICloseableIterator;
import de.yamayaki.cesium.api.database.IDBInstance;
import de.yamayaki.cesium.common.spec.WorldDatabaseSpecs;
import de.yamayaki.cesium.maintenance.storage.IChunkStorage;
import net.minecraft.world.level.ChunkPos;
import org.lmdbjava.LmdbException;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CesiumChunkStorage implements IChunkStorage {
    private final Logger logger;
    private final IDBInstance database;

    public CesiumChunkStorage(final Logger logger, final Path basePath) {
        this.logger = logger;
        this.database = CesiumMod.openWorldDB(basePath);
    }

    @Override
    public List<ChunkPos> getAllChunks() {
        final List<ChunkPos> list = new ArrayList<>();

        try (final ICloseableIterator<ChunkPos> crs = this.database.getDatabase(WorldDatabaseSpecs.CHUNK_DATA).getIterator()) {
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
    public void setChunkData(final ChunkPos chunkPos, final byte[] bytes) {
        this.database.getTransaction(WorldDatabaseSpecs.CHUNK_DATA).addBytes(chunkPos, bytes);
    }

    @Override
    public byte[] getChunkData(final ChunkPos chunkPos) {
        return this.database.getDatabase(WorldDatabaseSpecs.CHUNK_DATA).getBytes(chunkPos);
    }

    @Override
    public void setPOIData(final ChunkPos chunkPos, final byte[] bytes) {
        this.database.getTransaction(WorldDatabaseSpecs.POI).addBytes(chunkPos, bytes);
    }

    @Override
    public byte[] getPOIData(final ChunkPos chunkPos) {
        return this.database.getDatabase(WorldDatabaseSpecs.POI).getBytes(chunkPos);
    }

    @Override
    public void setEntityData(final ChunkPos chunkPos, final byte[] bytes) {
        this.database.getTransaction(WorldDatabaseSpecs.ENTITY).addBytes(chunkPos, bytes);
    }

    @Override
    public byte[] getEntityData(final ChunkPos chunkPos) {
        return this.database.getDatabase(WorldDatabaseSpecs.ENTITY).getBytes(chunkPos);
    }
}
