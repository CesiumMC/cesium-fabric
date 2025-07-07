package de.yamayaki.cesium.common.lmdb;

import de.yamayaki.cesium.common.DatabaseSpec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.lmdbjava.ByteArrayProxy;
import org.lmdbjava.CopyFlags;
import org.lmdbjava.Env;
import org.lmdbjava.EnvFlags;
import org.lmdbjava.LmdbException;
import org.lmdbjava.Stat;
import org.lmdbjava.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LMDBInstance {
    private static final Logger LOGGER = LoggerFactory.getLogger(LMDBInstance.class);
    private static final int MAX_COMMIT_ATTEMPTS = 3;

    private final Reference2ObjectMap<DatabaseSpec<?, ?>, KVDatabase<?, ?>> databases = new Reference2ObjectOpenHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    protected final Env<byte[]> env;
    protected final long resizeStep;

    protected final boolean logsMapGrows;

    protected volatile boolean dirty = false;

    public LMDBInstance(final Path databasePath, final DatabaseSpec<?, ?>[] databases, final boolean logMapGrows, final boolean isUncompressed) {
        this.env = Env.create(ByteArrayProxy.PROXY_BA)
                .setMaxDbs(databases.length)
                .open(databasePath.toFile(), EnvFlags.MDB_NOLOCK, EnvFlags.MDB_NOSUBDIR);

        this.resizeStep = Arrays.stream(databases).mapToLong(DatabaseSpec::initialSize).sum();

        if (this.env.info().mapSize < this.resizeStep) {
            this.env.setMapSize(this.resizeStep);
        }

        for (final DatabaseSpec<?, ?> spec : databases) {
            this.databases.put(spec, new KVDatabase<>(this, spec, isUncompressed));
        }

        this.logsMapGrows = logMapGrows;
    }

    @SuppressWarnings("unchecked")
    public <K, V> KVDatabase<K, V> getDatabase(DatabaseSpec<K, V> spec) {
        final KVDatabase<?, ?> database = this.databases.get(spec);

        if (database == null) {
            throw new NullPointerException("No database is registered for spec " + spec);
        }

        return (KVDatabase<K, V>) database;
    }

    public void flushChanges() {
        if (!this.dirty) return;

        this.lock.writeLock()
                .lock();

        try {
            for (final KVDatabase<?, ?> txn : this.databases.values()) {
                txn.prepareCommit();
            }

            for (int attempts = 1; attempts < MAX_COMMIT_ATTEMPTS + 1; attempts++) {
                try (final Txn<?> txn = this.prepareTransaction()) {
                    txn.commit();
                    break;
                } catch (final LmdbException l) {
                    if (l instanceof Env.MapFullException) {
                        this.growMap();
                        attempts--;
                        continue;
                    }

                    LOGGER.info("Commit of transaction failed; trying again ({}/{}): {}", attempts, MAX_COMMIT_ATTEMPTS, l.getMessage());
                }

                if (attempts == MAX_COMMIT_ATTEMPTS) {
                    throw new RuntimeException("Could not commit transactions!");
                }
            }

            for (final KVDatabase<?, ?> txn : this.databases.values()) {
                txn.cleanupCommit();
            }

            this.dirty = false;
        } finally {
            this.lock.writeLock()
                    .unlock();
        }
    }

    private Txn<?> prepareTransaction() throws LmdbException {
        final Txn<byte[]> txn = this.env.txnWrite();

        try {
            for (final KVDatabase<?, ?> dbi : this.databases.values()) {
                dbi.addChanges(txn);
            }
        } catch (final LmdbException l) {
            txn.abort();
            txn.close();

            throw l;
        }

        return txn;
    }

    private void growMap() {
        final long oldSize = this.env.info().mapSize;
        final long newSize = oldSize + this.resizeStep;

        this.env.setMapSize(newSize);

        if (this.logsMapGrows) {
            LOGGER.info("Grew map size from {} to {} MB", (oldSize / 1024 / 1024), (newSize / 1024 / 1024));
        }
    }

    public void copyTo(final Path path) {
        this.lock.writeLock()
                .lock();

        try {
            this.env.copy(path.toFile(), CopyFlags.MDB_CP_COMPACT);
        } finally {
            this.lock.writeLock()
                    .unlock();
        }
    }

    public List<Stat> getStats() {
        this.lock.readLock()
                .lock();

        try {
            return this.databases.values().stream()
                    .map(KVDatabase::getStats)
                    .toList();
        } finally {
            this.lock.readLock()
                    .unlock();
        }
    }

    public ReentrantReadWriteLock getLock() {
        return this.lock;
    }

    public boolean closed() {
        return this.env.isClosed();
    }

    public void close() {
        this.flushChanges();

        for (KVDatabase<?, ?> database : this.databases.values()) {
            database.close();
        }

        this.env.close();
    }
}
