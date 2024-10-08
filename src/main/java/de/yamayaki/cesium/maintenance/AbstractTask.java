package de.yamayaki.cesium.maintenance;

import com.mojang.logging.LogUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractTask {
    protected final Logger LOGGER = LogUtils.getLogger();

    protected final LevelStorageSource.LevelStorageAccess levelAccess;
    protected final List<ResourceKey<Level>> levels;

    protected final Thread workerThread;
    protected final AtomicBoolean running = new AtomicBoolean(true);

    protected final AtomicReference<String> status = new AtomicReference<>();

    protected final AtomicInteger totalElements = new AtomicInteger(0);
    protected final AtomicInteger currentElement = new AtomicInteger(0);
    protected final AtomicReference<ResourceKey<Level>> currentLevel = new AtomicReference<>(null);

    public AbstractTask(final String task, final LevelStorageSource.LevelStorageAccess levelAccess, final RegistryAccess registryAccess) {
        this.levelAccess = levelAccess;

        this.levels = registryAccess
                .lookupOrThrow(Registries.LEVEL_STEM)
                .registryKeySet()
                .stream().map(Registries::levelStemToLevel)
                .toList();

        this.status.set("Loading required data into memory ...");

        this.workerThread = this.createWorkerThread(task);
        this.workerThread.start();
    }

    public @NotNull Thread createWorkerThread(final String task) {
        final Thread workerThread = new Thread(this::runTasks, "Cesium-" + task + "-Database");
        workerThread.setDaemon(true);
        workerThread.setUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.error("Uncaught exception while {} world!", task, throwable);
            this.running.set(false);
        });

        return workerThread;
    }

    protected abstract void runTasks();

    public void cancelTask() {
        this.running.set(false);

        try {
            this.workerThread.join();
        } catch (InterruptedException ignored) {
        }
    }

    public boolean running() {
        return this.running.get();
    }

    public String levelName() {
        final ResourceKey<Level> curLevel = this.currentLevel.get();
        return curLevel == null ? "<unnamed>" : curLevel.toString();
    }

    public String status() {
        return this.status.get();
    }

    public int totalElements() {
        return this.totalElements.get();
    }

    public int currentElement() {
        return this.currentElement.get();
    }

    public double percentage() {
        return this.currentElement() / (double) Math.max(this.totalElements(), 1);
    }

    public enum Task {
        TO_ANVIL,
        TO_CESIUM,
        COMPACT
    }
}
