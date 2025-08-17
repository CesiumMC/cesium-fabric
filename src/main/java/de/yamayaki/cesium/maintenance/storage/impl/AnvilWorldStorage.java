package de.yamayaki.cesium.maintenance.storage.impl;

import de.yamayaki.cesium.FileHelper;
import de.yamayaki.cesium.MCHelper;
import de.yamayaki.cesium.accessor.RawAccess;
import de.yamayaki.cesium.maintenance.storage.IWorldStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnvilWorldStorage {
    private static final Pattern PATTERN_MCA = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.mca$");
    private static final Pattern PATTERN_UID = Pattern.compile("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}\\.(dat|json)$");

    private static final Logger LOGGER = LoggerFactory.getLogger(AnvilWorldStorage.class);

    public static class DimensionStorage implements IWorldStorage.IDimensionStorage {
        private final AnvilData chunkData;
        private final AnvilData poiData;
        private final AnvilData entityData;

        public DimensionStorage(final Path basePath) {
            this.chunkData = AnvilData.open(basePath.resolve("region"));
            this.poiData = AnvilData.open(basePath.resolve("poi"));
            this.entityData = AnvilData.open(basePath.resolve("entities"));
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
            return FileHelper.traverseFilesSafe(
                    () -> "Could not resolve regions from directory, aborting.",
                    this.chunkData.path(),
                    new FileHelper.NoOpMapper(),
                    new FileHelper.FileExtensionRule(".mca")
            ).flatMap(regionFile -> {
                final Stream.Builder<ChunkPos> builder = Stream.builder();

                Matcher matcher = PATTERN_MCA.matcher(regionFile.getFileName().toString());

                if (matcher.matches()) {
                    final int regionX = Integer.parseInt(matcher.group(1)) << 5;
                    final int regionZ = Integer.parseInt(matcher.group(2)) << 5;

                    for (int chunkX = 0; chunkX < 32; ++chunkX) {
                        for (int chunkY = 0; chunkY < 32; ++chunkY) {
                            builder.add(new ChunkPos(chunkX + regionX, chunkY + regionZ));
                        }
                    }
                }

                return builder.build();
            }).collect(Collectors.toList());
        }

        @Override
        public void flush() {
            try {
                this.chunkData.flush();
                this.poiData.flush();
                this.chunkData.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {
            try {
                this.chunkData.close();
                this.poiData.close();
                this.entityData.close();
            } catch (IOException exception) {
                LOGGER.error("[ANVIL] Failed to close chunk storage", exception);
            }
        }

        @Override
        public String toString() {
            return "AnvilChunk";
        }
    }

    public static class PlayerStorage implements IWorldStorage.IPlayerStorage {
        private final NbtData playerData;
        private final JsonData statsData;
        private final JsonData advancementsData;

        public PlayerStorage(final Path basePath) {
            this.playerData = new NbtData(basePath.resolve("playerdata"));
            this.statsData = new JsonData(basePath.resolve("stats"));
            this.advancementsData = new JsonData(basePath.resolve("advancements"));
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
            return FileHelper.traverseFilesSafe(
                    () -> "Could not resolve players from directory, aborting.",
                    this.playerData.basePath(),
                    FileHelper.MAPPER_UUID,
                    new FileHelper.FileExtensionRule(".dat"),
                    new FileHelper.PatternRule(PATTERN_UID)
            ).toList();
        }

        @Override
        public void flush() {
            // Not supported
        }

        @Override
        public void close() {
            // Not supported
        }

        @Override
        public String toString() {
            return "AnvilPlayer";
        }
    }

    private record AnvilData(Path path, RegionFileStorage regionStorage) implements IWorldStorage.IAbstractData<ChunkPos, byte @Nullable []> {
        public static AnvilData open(final Path regionStorage) {
            return new AnvilData(regionStorage, MCHelper.openRegionStorage(regionStorage));
        }

        @Override
        public void set(final @NotNull ChunkPos key, final byte @Nullable [] value) {
            try {
                ((RawAccess) (Object) this.regionStorage).cesium$putBytes(key, value);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public byte @Nullable [] get(final @NotNull ChunkPos key) {
            try {
                return ((RawAccess) (Object) this.regionStorage).cesium$getBytes(key);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void flush() throws IOException {
            this.regionStorage.flush();
        }

        public void close() throws IOException {
            this.regionStorage.close();
        }
    }

    private record NbtData(Path basePath) implements IWorldStorage.IAbstractData<UUID, CompoundTag> {
        @Override
        public void set(final @NotNull UUID key, final @Nullable CompoundTag value) {
            try {
                final Path savePath = this.savePath(key);

                if (value == null) {
                    Files.deleteIfExists(savePath);
                } else {
                    FileHelper.ensureFile(savePath);
                    MCHelper.writeCompressedNbt(savePath, value);
                }
            } catch (IOException exception) {
                LOGGER.warn("[ANVIL] Failed to save player data for {}", key);
            }
        }

        @Override
        public @Nullable CompoundTag get(@NotNull UUID key) {
            CompoundTag compoundTag = null;

            try {
                final Path savePath = this.savePath(key);

                if (Files.isRegularFile(savePath)) {
                    return MCHelper.readCompressedNbt(savePath);
                } else {
                    return null;
                }
            } catch (IOException exception) {
                LOGGER.warn("[ANVIL] Failed to load player data for {}", key);
            }

            return compoundTag;
        }

        private Path savePath(final @NotNull UUID key) {
            return this.basePath.resolve(key + ".dat");
        }
    }

    private record JsonData(Path basePath) implements IWorldStorage.IAbstractData<UUID, String> {
        @Override
        public void set(final @NotNull UUID key, final @Nullable String value) {
            try {
                final Path savePath = this.savePath(key);

                if (value == null) {
                    Files.deleteIfExists(savePath);
                } else {
                    Files.writeString(savePath, value, StandardCharsets.UTF_8);
                }
            } catch (final IOException i) {
                throw new RuntimeException("Could not write data", i);
            }
        }

        @Override
        public @Nullable String get(@NotNull UUID key) {
            String advancements = null;

            try {
                advancements = Files.readString(this.savePath(key), StandardCharsets.UTF_8);
            } catch (final IOException i) {
                throw new RuntimeException("Could not read data", i);
            }

            return advancements;
        }

        private Path savePath(final UUID key) {
            return this.basePath.resolve(key + ".json");
        }
    }
}
