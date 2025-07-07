package de.yamayaki.cesium.maintenance.tasks;

import de.yamayaki.cesium.MinecraftHelper;
import de.yamayaki.cesium.maintenance.AbstractTask;
import de.yamayaki.cesium.maintenance.WorldInfo;
import de.yamayaki.cesium.maintenance.storage.IWorldStorage;
import de.yamayaki.cesium.maintenance.storage.anvil.AnvilChunkStorage;
import de.yamayaki.cesium.maintenance.storage.anvil.AnvilPlayerStorage;
import de.yamayaki.cesium.maintenance.storage.cesium.CesiumChunkStorage;
import de.yamayaki.cesium.maintenance.storage.cesium.CesiumPlayerStorage;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DatabaseConvert extends AbstractTask {
    private final Task task;

    public DatabaseConvert(final Task task, final WorldInfo worldInfo) {
        super("Convert", worldInfo);
        this.task = task;
        this.start();
    }

    @Override
    protected void runOnPlayerData(final Path storagePath) {
        try (
                final IWorldStorage.IPlayerStorage _old = this.pStorage(storagePath, true);
                final IWorldStorage.IPlayerStorage _new = this.pStorage(storagePath, false)
        ) {
            copyAllElements(_old, _new);
        } catch (final Throwable t) {
            throw new RuntimeException("Could not copy all player data.", t);
        }
    }

    @Override
    protected void runOnDimension(final Path storagePath) {
        try (
                final IWorldStorage.IChunkStorage _old = this.cStorage(storagePath, true);
                final IWorldStorage.IChunkStorage _new = this.cStorage(storagePath, false)
        ) {
            copyAllElements(_old, _new);
        } catch (final Throwable t) {
            throw new RuntimeException("Could not copy all level data.", t);
        }
    }

    private <Type> void copyAllElements(final IWorldStorage<Type> source, final IWorldStorage<Type> target) {
        final Iterator<Type> iterator;

        {
            final List<Type> elements = source.getAllKeys();
            iterator = elements.iterator();

            this.totalElements.set(elements.size());
            this.currentElement.set(0);
        }

        final int taskCount = Runtime.getRuntime().availableProcessors();
        final List<CompletableFuture<Void>> copyTasks = new ArrayList<>(taskCount);

        int copiedItems;

        while (this.running.get() && iterator.hasNext()) {
            copiedItems = this.currentElement.addAndGet(1);

            copyTasks.add(copyBetween(iterator.next(), source, target));

            if ((copiedItems % taskCount) == 0) {
                CompletableFuture.allOf(copyTasks.toArray(CompletableFuture[]::new)).join();
                target.flush();
                copyTasks.clear();
            }
        }

        CompletableFuture.allOf(copyTasks.toArray(CompletableFuture[]::new)).join();
    }

    private static <Type> CompletableFuture<Void> copyBetween(final Type key, final IWorldStorage<Type> source, final IWorldStorage<Type> target) {
        return CompletableFuture.runAsync(() -> {
            LOGGER.debug("Copying {} from {} to {}", key, source, target);
            source.copyTo(key, target);
        }, MinecraftHelper.executorPool()).exceptionally((throwable) -> {
            LOGGER.error("Could not copy data into new storage!", throwable);
            return null;
        });
    }

    private @NotNull IWorldStorage.IChunkStorage cStorage(final Path path, final boolean old) {
        if (!old) {
            return this.task == Task.TO_ANVIL ? new AnvilChunkStorage(LOGGER, path) : new CesiumChunkStorage(LOGGER, path);
        } else {
            return this.task == Task.TO_ANVIL ? new CesiumChunkStorage(LOGGER, path) : new AnvilChunkStorage(LOGGER, path);
        }
    }

    private @NotNull IWorldStorage.IPlayerStorage pStorage(final Path path, final boolean old) {
        if (!old) {
            return this.task == Task.TO_ANVIL ? new AnvilPlayerStorage(LOGGER, path) : new CesiumPlayerStorage(LOGGER, path);
        } else {
            return this.task == Task.TO_ANVIL ? new CesiumPlayerStorage(LOGGER, path) : new AnvilPlayerStorage(LOGGER, path);
        }
    }
}
