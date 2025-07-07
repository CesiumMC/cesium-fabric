package de.yamayaki.cesium.maintenance.tasks;

import de.yamayaki.cesium.CesiumMod;
import de.yamayaki.cesium.FileHelper;
import de.yamayaki.cesium.common.lmdb.LMDBInstance;
import de.yamayaki.cesium.maintenance.AbstractTask;
import de.yamayaki.cesium.maintenance.WorldInfo;

import java.nio.file.Path;

public class DatabaseCompact extends AbstractTask {
    public DatabaseCompact(final WorldInfo worldInfo) {
        super("Compact", worldInfo);
        this.start();
    }

    @Override
    protected void runOnPlayerData(final Path storagePath) {
        // We do not compact player data
    }

    @Override
    protected void runOnDimension(final Path storagePath) {
        final Path source = storagePath.resolve(CesiumMod.dbFileName("chunks"));
        final Path target = storagePath.resolve("chunks.copy");

        final LMDBInstance lmdb = CesiumMod.openWorldDB(storagePath);

        try {
            lmdb.copyTo(target);
            lmdb.close();

            FileHelper.atomicReplace(target, source);
        } catch (final Throwable t) {
            throw new RuntimeException("Failed to compact level.", t);
        } finally {
            if (!lmdb.closed()) {
                lmdb.close();
            }
        }
    }
}
