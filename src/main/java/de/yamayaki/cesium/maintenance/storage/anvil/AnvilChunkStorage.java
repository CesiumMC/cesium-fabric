package de.yamayaki.cesium.maintenance.storage.anvil;

import com.google.common.collect.ImmutableList;
import de.yamayaki.cesium.api.accessor.RawAccess;
import de.yamayaki.cesium.maintenance.storage.IChunkStorage;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnvilChunkStorage implements IChunkStorage {
    private static final Pattern REGEX = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.mca$");

    private final Logger logger;
    private final Path basePath;

    private final RegionFileStorage chunkData;
    private final RegionFileStorage poiData;
    private final RegionFileStorage entityData;

    public AnvilChunkStorage(final Logger logger, final Path basePath) {
        this.logger = logger;
        this.basePath = basePath;

        this.chunkData = new RegionFileStorage(new RegionStorageInfo("cesium", null, "region"), basePath.resolve("region"), false);
        this.poiData = new RegionFileStorage(new RegionStorageInfo("cesium", null, "poi"), basePath.resolve("poi"), false);
        this.entityData = new RegionFileStorage(new RegionStorageInfo("cesium", null, "entities"), basePath.resolve("entities"), false);
    }

    @Override
    public List<ChunkPos> getAllChunks() {
        final File regionsFolder = new File(this.basePath.toFile(), "region");
        final File[] files = regionsFolder.listFiles((filex, string) -> string.endsWith(".mca"));

        if (files == null) {
            return ImmutableList.of();
        }

        final List<ChunkPos> list = new ArrayList<>();

        for (File region : files) {
            Matcher matcher = REGEX.matcher(region.getName());
            if (matcher.matches()) {
                final int regionX = Integer.parseInt(matcher.group(1)) << 5;
                final int regionZ = Integer.parseInt(matcher.group(2)) << 5;

                for (int chunkX = 0; chunkX < 32; ++chunkX) {
                    for (int chunkY = 0; chunkY < 32; ++chunkY) {
                        list.add(new ChunkPos(chunkX + regionX, chunkY + regionZ));
                    }
                }
            }
        }

        return list;
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
        this.flush();

        try {
            this.chunkData.close();
            this.poiData.close();
            this.entityData.close();
        } catch (IOException exception) {
            this.logger.error("[ANVIL] Failed to close chunk storage", exception);
        }
    }

    @Override
    public synchronized void setChunkData(final ChunkPos chunkPos, final byte[] bytes) {
        try {
            ((RawAccess) (Object) this.chunkData).cesium$putBytes(chunkPos, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized byte[] getChunkData(final ChunkPos chunkPos) {
        try {
            return ((RawAccess) (Object) this.chunkData).cesium$getBytes(chunkPos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void setPOIData(final ChunkPos chunkPos, final byte[] bytes) {
        try {
            ((RawAccess) (Object) this.poiData).cesium$putBytes(chunkPos, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized byte[] getPOIData(final ChunkPos chunkPos) {
        try {
            return ((RawAccess) (Object) this.poiData).cesium$getBytes(chunkPos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void setEntityData(final ChunkPos chunkPos, final byte[] bytes) {
        try {
            ((RawAccess) (Object) this.entityData).cesium$putBytes(chunkPos, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized byte[] getEntityData(final ChunkPos chunkPos) {
        try {
            return ((RawAccess) (Object) this.entityData).cesium$getBytes(chunkPos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
