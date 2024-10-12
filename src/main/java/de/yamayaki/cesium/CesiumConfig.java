package de.yamayaki.cesium;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public class CesiumConfig {
    private final Option<Boolean> log_map_grows = new Option<>(false, "Log when a database map is being resized.");
    private final Option<Boolean> show_debug_info = new Option<>(false, "Display information on the debug screen.");
    private final Option<Integer> zstd_compression_level = new Option<>(8, "The zstd library supports compression levels from 1 to 22. The lower the level, the faster the speed (at the cost of compression).");
    private final Option<Boolean> zstd_use_dictionary = new Option<>(true, "The compression ratio achievable can be highly improved using the built-in dictionary.");
    private final Option<Boolean> disable_compression = new Option<>(false, "HERE BE DRAGONS!! Forcefully disable all compression. A different database is used for uncompressed data, you can't have compressed and uncompressed worlds at the same time. Compressed worlds have to be reimported.");
    private final Option<Boolean> force_save_after_tick = new Option<>(false, "HERE BE DRAGONS!! Forces the (internal) to save all player and chunk data after each server tick. This option can slow down your server.");

    public boolean logMapGrows() {
        return this.log_map_grows.value;
    }

    public boolean showDebugInfo() {
        return this.show_debug_info.value;
    }

    public int compressionLevel() {
        return this.zstd_compression_level.value;
    }

    public boolean useDictionary() {
        return this.zstd_use_dictionary.value;
    }

    public boolean isUncompressed() {
        return this.disable_compression.value;
    }

    public boolean saveAfterTick() {
        return this.force_save_after_tick.value;
    }

    @SuppressWarnings({"unused", "FieldMayBeFinal", "FieldCanBeLocal"})
    private static class Option<T> {
        private T value;
        private final String comment;

        public Option(T value, String comment) {
            this.value = value;
            this.comment = comment;
        }
    }

    public static class Loader {
        private final Path configPath;
        private final Gson gson;

        public Loader(@NotNull final Path configPath) {
            this.configPath = configPath;
            this.gson = new GsonBuilder()
                    .serializeNulls()
                    .setPrettyPrinting()
                    .setLenient()
                    .create();
        }

        public CesiumConfig get() {
            try {
                return this.loadConfig();
            } catch (final IOException i) {
                throw new RuntimeException("Failed to initialize Cesium config.", i);
            }
        }

        private CesiumConfig loadConfig() throws IOException {
            CesiumConfig config = null;

            // Load from disk or create new default instance
            if (Files.isRegularFile(this.configPath, LinkOption.NOFOLLOW_LINKS)) {
                try (final BufferedReader bufferedReader = Files.newBufferedReader(this.configPath)) {
                    config = this.gson.fromJson(bufferedReader, CesiumConfig.class);
                }
            }

            if (config == null) {
                config = new CesiumConfig();
            }

            // Save to disk and/ or add missing fields
            try (final BufferedWriter bufferedWriter = Files.newBufferedWriter(this.configPath)) {
                this.gson.toJson(config, CesiumConfig.class, bufferedWriter);
            }

            return config;
        }
    }
}
