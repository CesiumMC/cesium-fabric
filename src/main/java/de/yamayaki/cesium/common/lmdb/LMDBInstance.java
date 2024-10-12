package de.yamayaki.cesium.common.lmdb;

import de.yamayaki.cesium.CesiumConfig;
import de.yamayaki.cesium.api.database.DatabaseSpec;
import de.yamayaki.cesium.api.database.IDBInstance;
import de.yamayaki.cesium.api.database.IKVDatabase;
import de.yamayaki.cesium.api.database.IKVTransaction;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.lmdbjava.ByteArrayProxy;
import org.lmdbjava.CopyFlags;
import org.lmdbjava.Env;
import org.lmdbjava.EnvFlags;
import org.lmdbjava.EnvInfo;
import org.lmdbjava.LmdbException;
import org.lmdbjava.Stat;
import org.lmdbjava.Txn;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LMDBInstance implements IDBInstance {
    private final Reference2ObjectMap<DatabaseSpec<?, ?>, KVDatabase<?, ?>> databases = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectMap<DatabaseSpec<?, ?>, KVTransaction<?, ?>> transactions = new Reference2ObjectOpenHashMap<>();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    protected final Logger logger;
    protected final boolean logsMapGrows;

    protected final Env<byte[]> env;

    protected final int MAX_COMMIT_TRIES = 3;
    protected final int resizeStep;

    protected volatile boolean isDirty = false;

    public LMDBInstance(final Path databasePath, final DatabaseSpec<?, ?>[] databases, final Logger logger, final CesiumConfig config) {
        this.logger = logger;
        this.logsMapGrows = config.logMapGrows();

        this.env = Env.create(ByteArrayProxy.PROXY_BA)
                .setMaxDbs(databases.length)
                .open(databasePath.toFile(), EnvFlags.MDB_NOLOCK, EnvFlags.MDB_NOSUBDIR);

        this.resizeStep = Arrays.stream(databases).mapToInt(DatabaseSpec::getInitialSize).sum();

        EnvInfo info = this.env.info();
        if (info.mapSize < this.resizeStep) {
            this.env.setMapSize(this.resizeStep);
        }

        for (DatabaseSpec<?, ?> spec : databases) {
            KVDatabase<?, ?> database = new KVDatabase<>(this, spec, !config.isUncompressed());

            this.databases.put(spec, database);
            this.transactions.put(spec, new KVTransaction<>(database));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> IKVDatabase<K, V> getDatabase(DatabaseSpec<K, V> spec) {
        KVDatabase<?, ?> database = this.databases.get(spec);

        if (database == null) {
            throw new NullPointerException("No database is registered for spec " + spec);
        }

        return (IKVDatabase<K, V>) database;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> IKVTransaction<K, V> getTransaction(DatabaseSpec<K, V> spec) {
        KVTransaction<?, ?> transaction = this.transactions.get(spec);

        if (transaction == null) {
            throw new NullPointerException("No transaction is registered for spec " + spec);
        }

        return (IKVTransaction<K, V>) transaction;
    }

    @Override
    public void flushChanges() {
        if (!this.isDirty) {
            return;
        }

        this.lock.writeLock()
                .lock();

        try {
            this.commitTransaction();
            this.isDirty = false;
        } finally {
            this.lock.writeLock()
                    .unlock();
        }
    }

    private void commitTransaction() {
        this.snapshotCreate();

        for (int tries = 1; tries < MAX_COMMIT_TRIES + 1; tries++) {
            try (final Txn<?> txn = this.prepareTransaction()) {
                txn.commit();

                break;
            } catch (final LmdbException l) {
                if (l instanceof Env.MapFullException) {
                    this.growMap();

                    tries--;
                    continue;
                }

                this.logger.info("Commit of transaction failed; trying again ({}/{}): {}", tries, this.MAX_COMMIT_TRIES, l.getMessage());
            }

            if (tries == MAX_COMMIT_TRIES) {
                throw new RuntimeException("Could not commit transactions!");
            }
        }

        this.snapshotClear();
    }

    private Txn<?> prepareTransaction() throws LmdbException {
        final Iterator<KVTransaction<?, ?>> it = this.transactions.values()
                .iterator();

        final Txn<byte[]> txn = this.env.txnWrite();

        try {
            while (it.hasNext()) {
                KVTransaction<?, ?> transaction = it.next();
                transaction.addChanges(txn);
            }
        } catch (LmdbException l) {
            txn.abort();
            throw l;
        }

        return txn;
    }

    private void snapshotCreate() {
        for (final KVTransaction<?, ?> txn : this.transactions.values()) {
            txn.createSnapshot();
        }
    }

    private void snapshotClear() {
        for (final KVTransaction<?, ?> txn : this.transactions.values()) {
            txn.clearSnapshot();
        }
    }

    private void growMap() {
        EnvInfo info = this.env.info();

        long oldSize = info.mapSize;
        long newSize = oldSize + (long) this.resizeStep;

        this.env.setMapSize(newSize);

        if (this.logsMapGrows) {
            this.logger.info("Grew map size from {} to {} MB", (oldSize / 1024 / 1024), (newSize / 1024 / 1024));
        }
    }

    public void createCopy(final Path path) {
        this.lock.writeLock()
                .lock();

        try {
            this.env.copy(path.toFile(), CopyFlags.MDB_CP_COMPACT);
        } finally {
            this.lock.writeLock()
                    .unlock();
        }
    }

    @Override
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

    @Override
    public ReentrantReadWriteLock getLock() {
        return this.lock;
    }

    @Override
    public boolean closed() {
        return this.env.isClosed();
    }

    @Override
    public void close() {
        this.flushChanges();

        for (KVDatabase<?, ?> database : this.databases.values()) {
            database.close();
        }

        this.env.close();
    }
}
