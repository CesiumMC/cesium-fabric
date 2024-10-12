package de.yamayaki.cesium.maintenance.storage.cesium;

import de.yamayaki.cesium.CesiumMod;
import de.yamayaki.cesium.api.database.ICloseableIterator;
import de.yamayaki.cesium.api.database.IDBInstance;
import de.yamayaki.cesium.common.spec.PlayerDatabaseSpecs;
import de.yamayaki.cesium.maintenance.storage.IPlayerStorage;
import net.minecraft.nbt.CompoundTag;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CesiumPlayerStorage implements IPlayerStorage {
    private final IDBInstance database;

    public CesiumPlayerStorage(final Path basePath) {
        this.database = CesiumMod.openPlayerDB(basePath);
    }

    @Override
    public List<UUID> getAllPlayers() {
        final List<UUID> list = new ArrayList<>();

        try (final ICloseableIterator<UUID> crs = this.database.getDatabase(PlayerDatabaseSpecs.STATISTICS).getIterator()) {
            while (crs.hasNext()) {
                list.add(crs.next());
            }
        } catch (final Throwable t) {
            throw new RuntimeException("Could not iterate on cursor.", t);
        }

        return list;
    }

    @Override
    public void close() {
        this.database.flushChanges();
        this.database.close();
    }

    @Override
    public void setPlayerNBT(final UUID uuid, final CompoundTag compoundTag) {
        this.database.getTransaction(PlayerDatabaseSpecs.PLAYER_DATA).add(uuid, compoundTag);
    }

    @Override
    public CompoundTag getPlayerNBT(final UUID uuid) {
        return this.database.getDatabase(PlayerDatabaseSpecs.PLAYER_DATA).getValue(uuid);
    }

    @Override
    public void setPlayerAdvancements(final UUID uuid, final String advancements) {
        this.database.getTransaction(PlayerDatabaseSpecs.ADVANCEMENTS).add(uuid, advancements);
    }

    @Override
    public String getPlayerAdvancements(final UUID uuid) {
        return this.database.getDatabase(PlayerDatabaseSpecs.ADVANCEMENTS).getValue(uuid);
    }

    @Override
    public void setPlayerStatistics(final UUID uuid, final String statistics) {
        this.database.getTransaction(PlayerDatabaseSpecs.STATISTICS).add(uuid, statistics);
    }

    @Override
    public String getPlayerStatistics(final UUID uuid) {
        return this.database.getDatabase(PlayerDatabaseSpecs.STATISTICS).getValue(uuid);
    }
}
