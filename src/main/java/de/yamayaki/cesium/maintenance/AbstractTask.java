package de.yamayaki.cesium.maintenance;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractTask {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractTask.class);

    private final WorldInfo world;
    private final Thread worker;

    private int dimensionIndex = -1;

    protected final AtomicBoolean running = new AtomicBoolean(true);
    protected final AtomicReference<String> status = new AtomicReference<>();
    protected final AtomicInteger totalElements = new AtomicInteger(0);
    protected final AtomicInteger currentElement = new AtomicInteger(0);

    public AbstractTask(final String task, final WorldInfo levels) {
        this.status.set("Loading ...");
        this.world = levels;
        this.worker = this.createWorkerThread(task);
    }

    private @NotNull Thread createWorkerThread(final String task) {
        final Thread workerThread = new Thread(this::runTasks, "Cesium-Maintenance");
        workerThread.setDaemon(true);
        workerThread.setUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.error("Uncaught exception while {} world!", task, throwable);
            this.running.set(false);
        });

        return workerThread;
    }

    private void runTasks() {
        this.status.set("Working on player data ...");

        this.runOnPlayerData(this.world.root());

        this.status.set("Working on dimension data ...");

        for (final WorldInfo.DimensionInfo dimensionInfo : this.world.levels()) {
            this.dimensionIndex++;
            this.runOnDimension(dimensionInfo.path());
        }

        this.running.set(false);
    }

    protected abstract void runOnPlayerData(final Path storagePath);

    protected abstract void runOnDimension(final Path storagePath);

    protected void start() {
        this.worker.start();
    }

    public void cancelTask() {
        this.running.set(false);

        this.status.set("Cancelling task ...");

        try {
            this.worker.join();
        } catch (InterruptedException ignored) {
        }
    }

    public boolean running() {
        return this.running.get();
    }

    public String levelName() {
        return (this.dimensionIndex != -1)
                ? this.world.levels()[this.dimensionIndex].name()
                : "<unnamed>";
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

    public Logger logger() {
        return LOGGER;
    }

    public enum Task {
        TO_ANVIL,
        TO_CESIUM,
        COMPACT
    }
}
