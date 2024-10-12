package de.yamayaki.cesium.maintenance.storage.anvil;

import de.yamayaki.cesium.FileHelper;
import de.yamayaki.cesium.maintenance.storage.IPlayerStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AnvilPlayerStorage implements IPlayerStorage {
    private final Logger logger;

    private final Path playerData;
    private final Path statsStorage;
    private final Path advancementsStorage;

    public AnvilPlayerStorage(final Logger logger, final Path basePath) {
        this.logger = logger;

        this.playerData = basePath.resolve("playerdata");
        this.statsStorage = basePath.resolve("stats");
        this.advancementsStorage = basePath.resolve("advancements");
    }

    @Override
    public List<UUID> getAllPlayers() {
        return FileHelper.resolveAllEnding(this.playerData, ".dat").stream().map(file -> {
            try {
                final String fileName = file.getName();
                final String uuid = fileName.substring(0, fileName.length() - 4);

                if (uuid.length() > 36 || uuid.length() < 32) {
                    this.logger.warn("Found non UUID player file in directory, ignoring ({})", file.getAbsolutePath());
                    return null;
                }

                return UUID.fromString(uuid);
            } catch (final Throwable t) {
                this.logger.error("Could not resolve UUID from filename, aborting ({})", file.getAbsolutePath());
                throw t;
            }
        }).filter(Objects::nonNull).toList();
    }

    @Override
    public void close() {
    }

    @Override
    public void setPlayerNBT(final UUID uuid, final CompoundTag compoundTag) {
        if (compoundTag == null) {
            return;
        }

        try {
            final Path savePath = this.playerData.resolve(uuid.toString() + ".dat");
            if (!Files.isDirectory(savePath.getParent()) || !Files.isRegularFile(savePath)) {
                Files.createDirectories(savePath.getParent());
                Files.createFile(savePath);
            }

            NbtIo.writeCompressed(compoundTag, savePath);
        } catch (IOException exception) {
            this.logger.warn("[ANVIL] Failed to save player data for {}", uuid);
        }
    }

    @Override
    public CompoundTag getPlayerNBT(final UUID uuid) {
        CompoundTag compoundTag = null;

        try {
            final Path savePath = this.playerData.resolve(uuid.toString() + ".dat");
            if (Files.isRegularFile(savePath)) {
                compoundTag = NbtIo.readCompressed(savePath, NbtAccounter.unlimitedHeap());
            }
        } catch (IOException exception) {
            this.logger.warn("[ANVIL] Failed to load player data for {}", uuid);
        }

        return compoundTag;
    }

    @Override
    public void setPlayerAdvancements(final UUID uuid, final String advancements) {
        try {
            final Path savePath = this.advancementsStorage.resolve(uuid.toString() + ".json");
            Files.writeString(savePath, advancements, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            this.logger.warn("[ANVIL] Failed to set advancements for {}", uuid);
        }
    }

    @Override
    public String getPlayerAdvancements(final UUID uuid) {
        String advancements = null;
        try {
            final Path savePath = this.advancementsStorage.resolve(uuid.toString() + ".json");
            advancements = Files.readString(savePath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            this.logger.warn("[ANVIL] Failed to load advancements for {}", uuid);
        }

        return advancements;
    }

    @Override
    public void setPlayerStatistics(final UUID uuid, final String statistics) {
        try {
            final Path savePath = this.statsStorage.resolve(uuid.toString() + ".json");
            Files.writeString(savePath, statistics, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            this.logger.warn("[ANVIL] Failed to save statistics for {}", uuid);
        }
    }

    @Override
    public String getPlayerStatistics(final UUID uuid) {
        String statistics = null;
        try {
            final Path savePath = this.statsStorage.resolve(uuid.toString() + ".json");
            statistics = Files.readString(savePath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            this.logger.warn("[ANVIL] Failed to load statistics for {}", uuid);
        }

        return statistics;
    }
}
