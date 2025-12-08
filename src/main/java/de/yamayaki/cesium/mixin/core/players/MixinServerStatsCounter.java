package de.yamayaki.cesium.mixin.core.players;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.DataFixer;
import de.yamayaki.cesium.api.accessor.DatabaseSource;
import de.yamayaki.cesium.api.database.IDBInstance;
import de.yamayaki.cesium.common.spec.PlayerDatabaseSpecs;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.util.StrictJsonParser;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.UUID;

@Mixin(ServerStatsCounter.class)
public class MixinServerStatsCounter {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    public void parse(DataFixer dataFixer, JsonElement jsonElement) {
        throw new RuntimeException("Tried to call dummy method!");
    }

    @Shadow
    @Final
    private Path file;

    @Unique
    private IDBInstance database;

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/nio/file/Files;isRegularFile(Ljava/nio/file/Path;[Ljava/nio/file/LinkOption;)Z"
            )
    )
    public boolean cancelInitialLoad(Path attrs, LinkOption[] ioe) {
        return false;
    }

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    public void loadFromDb(MinecraftServer minecraftServer, Path path, CallbackInfo ci) {
        this.database = ((DatabaseSource) minecraftServer).cesium$getStorage();

        String json = this.database
                .getDatabase(PlayerDatabaseSpecs.STATISTICS)
                .getValue(this.getUuid());

        if (json == null) {
            return;
        }

        try (StringReader reader = new StringReader(json)) {
            JsonElement jsonElement = StrictJsonParser.parse(reader);
            this.parse(minecraftServer.getFixerUpper(), jsonElement);
        } catch (JsonParseException var5) {
            LOGGER.error("Couldn't parse statistics for player {}", this.getUuid(), var5);
        } catch (Exception var4) {
            LOGGER.error("Couldn't read statistics for player {}", this.getUuid(), var4);
        }
    }



    @Unique
    private UUID getUuid() {
        final int count = this.file.getNameCount();
        return UUID.fromString(this.file.getName(count - 1).toString().replace(".json", ""));
    }

    @Redirect(
            method = "save",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/FileUtil;createDirectoriesSafe(Ljava/nio/file/Path;)V")
    )
    public void disableFolderCreation(Path path) {
        // Do nothing
    }

    @Redirect(
            method = "save",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/nio/file/Files;newBufferedWriter(Ljava/nio/file/Path;Ljava/nio/charset/Charset;[Ljava/nio/file/OpenOption;)Ljava/io/BufferedWriter;",
                    remap = false
            )
    )
    public BufferedWriter redirectWrite(Path path, Charset cs, OpenOption[] options) {
        return new BufferedWriter(new StringWriter() {
            @Override
            public void close() {
                MixinServerStatsCounter.this.database
                        .getTransaction(PlayerDatabaseSpecs.STATISTICS)
                        .add(MixinServerStatsCounter.this.getUuid(), this.toString());
            }
        });
    }
}
