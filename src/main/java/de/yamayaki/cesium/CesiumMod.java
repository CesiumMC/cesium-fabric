package de.yamayaki.cesium;

import com.mojang.logging.LogUtils;
import de.yamayaki.cesium.api.database.DatabaseSpec;
import de.yamayaki.cesium.api.database.IDBInstance;
import de.yamayaki.cesium.common.lmdb.LMDBInstance;
import de.yamayaki.cesium.common.spec.PlayerDatabaseSpecs;
import de.yamayaki.cesium.common.spec.WorldDatabaseSpecs;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;

public class CesiumMod implements ModInitializer {
    private static final DatabaseSpec<?, ?>[] worldSpecs = new DatabaseSpec[]{
            WorldDatabaseSpecs.CHUNK_DATA,
            WorldDatabaseSpecs.POI,
            WorldDatabaseSpecs.ENTITY
    };
    private static final DatabaseSpec<?, ?>[] playerSpecs = new DatabaseSpec[]{
            PlayerDatabaseSpecs.PLAYER_DATA,
            PlayerDatabaseSpecs.ADVANCEMENTS,
            PlayerDatabaseSpecs.STATISTICS
    };

    private static Logger cesiumLogger;
    private static CesiumConfig cesiumConfig;

    @Override
    public void onInitialize() {
        var configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("cesium.json");

        cesiumLogger = LogUtils.getLogger();
        cesiumConfig = new CesiumConfig.Loader(configPath).get();
    }

    public static @NotNull IDBInstance openWorldDB(@NotNull final Path dimensionPath) {
        return openDB(dimensionPath, "chunks", worldSpecs);
    }

    public static @NotNull IDBInstance openPlayerDB(@NotNull final Path worldPath) {
        return openDB(worldPath, "players", playerSpecs);
    }

    private static @NotNull IDBInstance openDB(@NotNull final Path dbBasePath, @NotNull final String dbName, @NotNull final DatabaseSpec<?, ?>[] dbSpecs) {
        FileHelper.ensureDirectory(dbBasePath);
        return new LMDBInstance(dbBasePath.resolve(dbName + getFileEnding()), dbSpecs, cesiumLogger, config());
    }

    public static CesiumConfig config() {
        if (cesiumConfig == null) {
            throw new RuntimeException("Config is not yet available, did you do something silly?");
        }

        return cesiumConfig;
    }

    public static String getFileEnding() {
        return config().isUncompressed() ? ".uncompressed.db" : ".db";
    }
}
