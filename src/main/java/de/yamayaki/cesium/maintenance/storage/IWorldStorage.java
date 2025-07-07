package de.yamayaki.cesium.maintenance.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface IWorldStorage<Type> extends ICopyable<Type>, AutoCloseable {
    interface IChunkStorage extends IWorldStorage<ChunkPos> {
        void setChunk(final @NotNull ChunkPos chunkPos, final byte @Nullable [] bytes);
        byte @Nullable [] getChunk(final @NotNull ChunkPos chunkPos);

        void setPOI(final ChunkPos chunkPos, final byte @Nullable [] bytes);
        byte @Nullable[] getPOI(final ChunkPos chunkPos);

        void setEntity(final ChunkPos chunkPos, final byte @Nullable [] bytes);
        byte @Nullable[] getEntity(final ChunkPos chunkPos);

        @Override
        default void copyTo(final @NotNull ChunkPos key, final @NotNull ICopyable<ChunkPos> copyable) {
            if (!(copyable instanceof IChunkStorage to)) {
                throw new UnsupportedOperationException("Can only copy from and to chunk storage!");
            }

            to.setChunk(key, this.getChunk(key));
            to.setEntity(key, this.getEntity(key));
            to.setPOI(key, this.getPOI(key));
        }
    }

    interface IPlayerStorage extends IWorldStorage<UUID> {
        void setPlayer(final @NotNull UUID uuid, final @Nullable CompoundTag compoundTag);
        @Nullable CompoundTag getPlayer(final @NotNull UUID uuid);

        void setAdvancements(final @NotNull UUID uuid, final @Nullable String advancements);
        @Nullable String getAdvancements(final @NotNull UUID uuid);

        void setStatistics(final @NotNull UUID uuid, final @Nullable String statistics);
        @Nullable String getStatistics(final @NotNull UUID uuid);

        @Override
        default void copyTo(final @NotNull UUID key, final @NotNull ICopyable<UUID> copyable) {
            if (!(copyable instanceof IPlayerStorage to)) {
                throw new UnsupportedOperationException("Can only copy from and to player storage!");
            }

            to.setPlayer(key, this.getPlayer(key));
            to.setAdvancements(key, this.getAdvancements(key));
            to.setStatistics(key, this.getStatistics(key));
        }
    }

    List<Type> getAllKeys();

    void flush();
}
