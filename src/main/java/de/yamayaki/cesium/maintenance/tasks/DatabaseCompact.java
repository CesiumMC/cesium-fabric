package de.yamayaki.cesium.maintenance.tasks;

import de.yamayaki.cesium.CesiumMod;
import de.yamayaki.cesium.api.database.IDBInstance;
import de.yamayaki.cesium.maintenance.AbstractTask;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class DatabaseCompact extends AbstractTask {
    public DatabaseCompact(final LevelStorageSource.LevelStorageAccess levelStorageAccess, final RegistryAccess registryAccess) {
        super("Compact", levelStorageAccess, registryAccess);
    }

    @Override
    protected void runTasks() {
        this.totalElements.set(this.levels.size());

        for (final ResourceKey<Level> levelResourceKey : this.levels) {
            this.currentElement.incrementAndGet();
            this.currentLevel.set(levelResourceKey);

            this.compactLevelDatabase(levelResourceKey);
        }

        this.running.set(false);
    }

    private void compactLevelDatabase(final ResourceKey<Level> level) {
        final Path dimensionPath = this.levelAccess.getDimensionPath(level);

        final Path originalPath = dimensionPath.resolve("chunks" + CesiumMod.getFileEnding());
        final Path copyPath = dimensionPath.resolve("chunks.copy");

        final IDBInstance dbInstance = CesiumMod.openWorldDB(dimensionPath);

        this.status.set("Compacting level data for " + level.location().getPath());

        try {
            dbInstance.createCopy(copyPath);
            dbInstance.close();

            if (Files.isRegularFile(copyPath) && Files.isRegularFile(originalPath)) {
                Files.move(copyPath, originalPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (final Throwable t) {
            throw new RuntimeException("Failed to compact level.", t);
        } finally {
            if (!dbInstance.closed()) {
                dbInstance.close();
            }
        }
    }
}
