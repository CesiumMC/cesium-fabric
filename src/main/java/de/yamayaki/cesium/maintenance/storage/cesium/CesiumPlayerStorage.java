package de.yamayaki.cesium.maintenance.storage.cesium;

import de.yamayaki.cesium.CesiumMod;
import de.yamayaki.cesium.common.lmdb.SerializingCursor;
import de.yamayaki.cesium.common.lmdb.LMDBInstance;
import de.yamayaki.cesium.common.spec.PlayerDatabaseSpecs;
import de.yamayaki.cesium.maintenance.storage.IWorldStorage;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.lmdbjava.LmdbException;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CesiumPlayerStorage implements IWorldStorage.IPlayerStorage {
    private final Logger logger;
    private final LMDBInstance database;

    public CesiumPlayerStorage(final Logger logger, final Path basePath) {
        this.logger = logger;
        this.database = CesiumMod.openPlayerDB(basePath);
    }

    @Override
    public List<UUID> getAllKeys() {
        final List<UUID> list = new ArrayList<>();

        try (final SerializingCursor<UUID> crs = this.database.getDatabase(PlayerDatabaseSpecs.STATISTICS).getIterator()) {
            while (crs.hasNext()) {
                list.add(crs.next());
            }
        } catch (final Throwable t) {
            throw new RuntimeException("Could not iterate on cursor.", t);
        }

        return list;
    }

    @Override
    public void setPlayer(final @NotNull UUID uuid, final CompoundTag compoundTag) {
        this.database.getDatabase(PlayerDatabaseSpecs.PLAYER_DATA).stageChange(uuid, compoundTag);
    }

    @Override
    public CompoundTag getPlayer(final @NotNull UUID uuid) {
        return this.database.getDatabase(PlayerDatabaseSpecs.PLAYER_DATA).getValue(uuid);
    }

    @Override
    public void setAdvancements(final @NotNull UUID uuid, final String advancements) {
        this.database.getDatabase(PlayerDatabaseSpecs.ADVANCEMENTS).stageChange(uuid, advancements);
    }

    @Override
    public String getAdvancements(final @NotNull UUID uuid) {
        return this.database.getDatabase(PlayerDatabaseSpecs.ADVANCEMENTS).getValue(uuid);
    }

    @Override
    public void setStatistics(final @NotNull UUID uuid, final String statistics) {
        this.database.getDatabase(PlayerDatabaseSpecs.STATISTICS).stageChange(uuid, statistics);
    }

    @Override
    public String getStatistics(final @NotNull UUID uuid) {
        return this.database.getDatabase(PlayerDatabaseSpecs.STATISTICS).getValue(uuid);
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
            this.logger.error("Failed to flush data", lmdbException);
        }
    }

    @Override
    public void close() {
        this.database.close();
    }
}
