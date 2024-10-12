package de.yamayaki.cesium;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class FileHelper {
    public static List<File> resolveAllEnding(final Path path, final String ending) {
        final File directory = path.toFile();
        final File[] files = directory.listFiles((dir, name) -> name.endsWith(ending));

        return files != null ? Arrays.stream(files).toList() : ImmutableList.of();
    }

    public static void ensureDirectory(final Path path) {
        try {
            if (!Files.isDirectory(path)) {
                Files.createDirectories(path);
            }
        } catch (final IOException i) {
            throw new RuntimeException("Failed to create directory.", i);
        }
    }
}
