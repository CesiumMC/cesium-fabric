package de.yamayaki.cesium;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public class CesiumMod implements ModInitializer {
    private static Logger cesiumLogger;
    private static CesiumConfig cesiumConfig;

    @Override
    public void onInitialize() {
        cesiumLogger = LogUtils.getLogger();
        cesiumConfig = initConfig();
    }

    public static @NotNull IDBInstance openWorldDB(@NotNull final Path dimensionPath) {
        return new LMDBInstance(dimensionPath, "chunks", new DatabaseSpec[]{
                WorldDatabaseSpecs.CHUNK_DATA,
                WorldDatabaseSpecs.POI,
                WorldDatabaseSpecs.ENTITY
        });
    }

    public static @NotNull IDBInstance openPlayerDB(@NotNull final Path worldPath) {
        return new LMDBInstance(worldPath, "players", new DatabaseSpec[]{
                PlayerDatabaseSpecs.PLAYER_DATA,
                PlayerDatabaseSpecs.ADVANCEMENTS,
                PlayerDatabaseSpecs.STATISTICS
        });
    }

    public static Logger logger() {
        return cesiumLogger;
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

    private static CesiumConfig initConfig() {
        final Gson gson = new GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .setLenient()
                .create();

        final Path path = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("cesium.json");

        try {
            var config = loadConfig(gson, path); // Load from disk or create new default instance
            saveConfig(config, gson, path); // Save to disk and/ or add missing fields

            return config;
        } catch (final IOException ioException) {
            throw new RuntimeException("Failed to initialize Cesium config.", ioException);
        }
    }

    private static CesiumConfig loadConfig(final Gson gson, final Path path) throws IOException {
        CesiumConfig config;

        if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            try (final BufferedReader bufferedReader = Files.newBufferedReader(path)) {
                config = gson.fromJson(bufferedReader, CesiumConfig.class);
            }
        } else {
            config = new CesiumConfig();
        }

        return config;
    }

    private static void saveConfig(final CesiumConfig config, final Gson gson, final Path path) throws IOException {
        try (final BufferedWriter bufferedWriter = Files.newBufferedWriter(path)) {
            gson.toJson(config, CesiumConfig.class, gson.newJsonWriter(bufferedWriter));
        }
    }
}
