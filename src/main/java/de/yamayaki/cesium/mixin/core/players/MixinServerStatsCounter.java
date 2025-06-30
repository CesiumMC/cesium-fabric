package de.yamayaki.cesium.mixin.core.players;

import com.google.gson.JsonParseException;
import com.mojang.datafixers.DataFixer;
import de.yamayaki.cesium.api.accessor.DatabaseSetter;
import de.yamayaki.cesium.common.lmdb.LMDBInstance;
import de.yamayaki.cesium.common.spec.PlayerDatabaseSpecs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.StatsCounter;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.util.UUID;

@Mixin(ServerStatsCounter.class)
public abstract class MixinServerStatsCounter extends StatsCounter implements DatabaseSetter {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    private File file;

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    public abstract void parseLocal(DataFixer dataFixer, String string);

    @Unique
    private LMDBInstance database;

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/io/File;isFile()Z"
            )
    )
    public boolean killInitialLoad(File file) {
        return false;
    }

    @Unique
    private UUID getUuid() {
        return UUID.fromString(this.file.getName().replace(".json", ""));
    }

    @Override
    public void cesium$setStorage(LMDBInstance storage) {
        this.database = storage;

        String json = this.database
                .getDatabase(PlayerDatabaseSpecs.STATISTICS)
                .getValue(this.getUuid());

        if (json == null) {
            return;
        }

        try {
            this.parseLocal(this.server.getFixerUpper(), json);
        } catch (JsonParseException var5) {
            LOGGER.error("Couldn't parse statistics for player {}", this.getUuid(), var5);
        } catch (Exception var4) {
            LOGGER.error("Couldn't read statistics for player {}", this.getUuid(), var4);
        }
    }

    @Redirect(
            method = "save",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/apache/commons/io/FileUtils;writeStringToFile(Ljava/io/File;Ljava/lang/String;)V",
                    remap = false
            )
    )
    public void redirectWrite(File file, String data) {
        this.database
                .getDatabase(PlayerDatabaseSpecs.STATISTICS)
                .stageChange(this.getUuid(), data);
    }
}
