package de.yamayaki.cesium.maintenance.storage.impl;

import de.yamayaki.cesium.CesiumMod;
import de.yamayaki.cesium.common.lmdb.KVDatabase;
import de.yamayaki.cesium.common.lmdb.LMDBInstance;
import de.yamayaki.cesium.common.lmdb.SerializingCursor;
import de.yamayaki.cesium.common.spec.PlayerDatabaseSpecs;
import de.yamayaki.cesium.common.spec.WorldDatabaseSpecs;
import de.yamayaki.cesium.maintenance.storage.IWorldStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lmdbjava.LmdbException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CesiumWorldStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(CesiumWorldStorage.class);

    public static class DimensionStorage implements IWorldStorage.IDimensionStorage {
        private final LMDBInstance database;

        private final CesiumBytesData<ChunkPos> chunkData;
        private final CesiumBytesData<ChunkPos> poiData;
        private final CesiumBytesData<ChunkPos> entityData;

        public DimensionStorage(final Path basePath) {
            this.database = CesiumMod.openWorldDB(basePath);

            this.chunkData = new CesiumBytesData<>(this.database.getDatabase(WorldDatabaseSpecs.CHUNK_DATA));
            this.poiData = new CesiumBytesData<>(this.database.getDatabase(WorldDatabaseSpecs.POI));
            this.entityData = new CesiumBytesData<>(this.database.getDatabase(WorldDatabaseSpecs.ENTITY));
        }

        @Override
        public @NotNull IAbstractData<ChunkPos, byte @Nullable []> chunk() {
            return this.chunkData;
        }

        @Override
        public @NotNull IAbstractData<ChunkPos, byte @Nullable []> poi() {
            return this.poiData;
        }

        @Override
        public @NotNull IAbstractData<ChunkPos, byte @Nullable []> entity() {
            return this.entityData;
        }

        @Override
        public @NotNull List<ChunkPos> getAllKeys() {
            final List<ChunkPos> list = new ArrayList<>();

            try (final SerializingCursor<ChunkPos> crs = this.chunkData.database().getIterator()) {
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
            this.database.flushChanges();
        }

        @Override
        public void close() {
            this.database.close();
        }

        @Override
        public String toString() {
            return "LmdbChunk";
        }
    }

    public static class PlayerStorage implements IWorldStorage.IPlayerStorage {
        private final LMDBInstance database;

        private final CesiumData<UUID, CompoundTag> playerData;
        private final CesiumData<UUID, String> statsData;
        private final CesiumData<UUID, String> advancementsData;

        public PlayerStorage(final Path basePath) {
            this.database = CesiumMod.openPlayerDB(basePath);

            this.playerData = new CesiumData<>(this.database.getDatabase(PlayerDatabaseSpecs.PLAYER_DATA));
            this.statsData = new CesiumData<>(this.database.getDatabase(PlayerDatabaseSpecs.STATISTICS));
            this.advancementsData = new CesiumData<>(this.database.getDatabase(PlayerDatabaseSpecs.ADVANCEMENTS));
        }

        @Override
        public @NotNull IAbstractData<UUID, CompoundTag> player() {
            return this.playerData;
        }

        @Override
        public @NotNull IAbstractData<UUID, String> advancements() {
            return this.advancementsData;
        }

        @Override
        public @NotNull IAbstractData<UUID, String> statistics() {
            return this.statsData;
        }

        @Override
        public @NotNull List<UUID> getAllKeys() {
            final List<UUID> list = new ArrayList<>();

            try (final SerializingCursor<UUID> crs = this.playerData.database().getIterator()) {
                while (crs.hasNext()) {
                    list.add(crs.next());
                }
            } catch (final Throwable t) {
                throw new RuntimeException("Could not iterate on cursor.", t);
            }

            return list;
        }

        @Override
        public String toString() {
            return "LmdbPlayer";
        }

        @Override
        public void flush() {
            try {
                this.database.flushChanges();
            } catch (LmdbException lmdbException) {
                LOGGER.error("Failed to flush data", lmdbException);
            }
        }

        @Override
        public void close() {
            this.database.close();
        }
    }

    private record CesiumBytesData<K>(KVDatabase<K, ?> database) implements IWorldStorage.IAbstractData<K, byte[]> {
        @Override
        public void set(final @NotNull K key, final byte @Nullable [] value) {
            this.database.stageChangeRaw(key, value);
        }

        @Override
        public byte @Nullable [] get(final @NotNull K key) {
            return this.database.getValueRaw(key);
        }
    }

    private record CesiumData<K, V>(KVDatabase<K, V> database) implements IWorldStorage.IAbstractData<K, V> {
        @Override
        public void set(final @NotNull K key, final @Nullable V value) {
            this.database.stageChange(key, value);
        }

        @Override
        public @Nullable V get(final @NotNull K key) {
            return this.database.getValue(key);
        }
    }
}
