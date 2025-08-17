package de.yamayaki.cesium;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class FileHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileHelper.class);

    public static final PathMapper<UUID> MAPPER_UUID = new UUIDMapper();
    public static final PathMapper<Path> MAPPER_PASSTHROUGH = new NoOpMapper();

    public static void ensureDirectory(final Path path) throws IOException{
        if (!Files.isDirectory(path)) {
            Files.createDirectories(path);
        }
    }

    public static void ensureFile(final Path path) throws IOException {
        ensureDirectory(path.getParent());
        Files.createFile(path);
    }

    public static void atomicReplace(final Path source, final Path target) throws IOException {
        if (!Files.isRegularFile(source)) {
            throw new IOException("Source file at " + source + " does not exist!");
        }

        if (!Files.isRegularFile(target)) {
            throw new IOException("Target file at " + target + " does not exist!");
        }

        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (final AtomicMoveNotSupportedException atomicException) {
            throw new IOException("Failed to atomically move file!", atomicException);
        }
    }

    public static <T> Stream<T> traverseFilesSafe(final @NotNull Supplier<String> onError, final @NotNull Path searchPath, final @NotNull PathMapper<T> pathMapper, final @NotNull Rule... rules) {
        try {
            return traverseFiles(searchPath, pathMapper, rules);
        } catch (final IOException i) {
            throw new RuntimeException(onError.get(), i);
        }
    }

    @SuppressWarnings("resource")
    public static <T> Stream<T> traverseFiles(final @NotNull Path searchPath, final @NotNull PathMapper<T> pathMapper, final @NotNull Rule... rules) throws IOException {
        if (!Files.isDirectory(searchPath)) {
            return Stream.empty();
        }

        return Files.walk(searchPath, 1)
                .filter(path -> __matchesAllOf(path, rules))
                .map(pathMapper::mapTo);
    }

    private static boolean __matchesAllOf(final @NotNull Path path, final @NotNull Rule... rules) {
        final String pathString = path.getFileName().toString();

        for (final Rule rule : rules) {
            if (!rule.matches(pathString)) {
                LOGGER.warn("Found non complying file in directory, ignoring ({}). (Failed {})", pathString, rule.name());
                return false;
            }
        }

        return true;
    }

    public interface PathMapper<T> {
        @NotNull T mapTo(final @NotNull Path path);
    }

    public static class NoOpMapper implements PathMapper<Path> {
        @Override
        @NotNull
        public Path mapTo(final @NotNull Path path) {
            return path;
        }
    }

    public static class UUIDMapper implements PathMapper<UUID> {
        @Override
        @NotNull
        public UUID mapTo(final @NotNull Path path) {
            String pathString = path.getFileName().toString();

            if (pathString.contains(".")) {
                pathString = pathString.substring(0, pathString.lastIndexOf('.'));
            }

            return UUID.fromString(pathString);
        }
    }

    public interface Rule {
        boolean matches(final @NotNull String pathString);

        String name();
    }

    public record Or(Rule a, Rule b) implements Rule {
        @Override
        public boolean matches(final @NotNull String pathString) {
            return this.a.matches(pathString) || this.b.matches(pathString);
        }

        @Override
        public String name() {
            return this.a.name() + " or " + this.b.name();
        }
    }

    public record FileExtensionRule(String endsWith) implements Rule {
        @Override
        public boolean matches(final @NotNull String pathString) {
            return pathString.endsWith(this.endsWith);
        }

        @Override
        public String name() {
            return "file extension " + this.endsWith;
        }
    }

    public record PatternRule(Pattern pattern) implements Rule {
        @Override
        public boolean matches(final @NotNull String pathString) {
            return this.pattern.matcher(pathString).matches();
        }

        @Override
        public String name() {
            return "pattern " + this.pattern.pattern();
        }
    }
}
