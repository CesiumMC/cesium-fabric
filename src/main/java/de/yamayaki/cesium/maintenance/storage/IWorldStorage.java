package de.yamayaki.cesium.maintenance.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface IWorldStorage<Type> extends ICopyable<Type>, AutoCloseable {
    interface IDimensionStorage extends IWorldStorage<ChunkPos> {
        @NotNull IAbstractData<ChunkPos, byte @Nullable []> chunk();

        @NotNull IAbstractData<ChunkPos, byte @Nullable []> poi();

        @NotNull IAbstractData<ChunkPos, byte @Nullable []> entity();

        @Override
        default void copyTo(final @NotNull ChunkPos key, final @NotNull ICopyable<ChunkPos> copyable) {
            if (!(copyable instanceof IWorldStorage.IDimensionStorage to)) {
                throw new UnsupportedOperationException("Can only copy from and to chunk storage!");
            }

            to.chunk().set(key, this.chunk().get(key));
            to.entity().set(key, this.entity().get(key));
            to.poi().set(key, this.poi().get(key));
        }
    }

    interface IPlayerStorage extends IWorldStorage<UUID> {
        @NotNull IAbstractData<UUID, CompoundTag> player();

        @NotNull IAbstractData<UUID, String> advancements();

        @NotNull IAbstractData<UUID, String> statistics();

        @Override
        default void copyTo(final @NotNull UUID key, final @NotNull ICopyable<UUID> copyable) {
            if (!(copyable instanceof IPlayerStorage to)) {
                throw new UnsupportedOperationException("Can only copy from and to player storage!");
            }

            to.player().set(key, this.player().get(key));
            to.advancements().set(key, this.advancements().get(key));
            to.statistics().set(key, this.statistics().get(key));
        }
    }

    interface IAbstractData<KeyType, ValueType> {
        void set(final @NotNull KeyType key, final @Nullable ValueType value);

        @Nullable ValueType get(final @NotNull KeyType key);
    }

    @NotNull List<Type> getAllKeys();

    void flush();
}
