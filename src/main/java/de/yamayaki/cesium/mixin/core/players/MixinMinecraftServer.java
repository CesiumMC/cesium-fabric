package de.yamayaki.cesium.mixin.core.players;

import com.mojang.datafixers.DataFixer;
import de.yamayaki.cesium.CesiumMod;
import de.yamayaki.cesium.api.accessor.DatabaseSetter;
import de.yamayaki.cesium.api.accessor.DatabaseSource;
import de.yamayaki.cesium.api.database.IDBInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.Proxy;
import java.nio.file.Path;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer implements DatabaseSource {
    @Shadow
    public Path getWorldPath(LevelResource levelResource) {
        throw new RuntimeException("Tried to access dummy method!");
    }

    @Shadow
    @Final
    protected PlayerDataStorage playerDataStorage;

    @Unique
    private IDBInstance playerDB;

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    public void initPlayerDatabase(Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, Proxy proxy, DataFixer dataFixer, Services services, LevelLoadListener levelLoadListener, CallbackInfo ci) {
        final Path path = this.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).getParent();
        this.playerDB = CesiumMod.openPlayerDB(path);

        ((DatabaseSetter) this.playerDataStorage).cesium$setStorage(this.playerDB);
    }

    @Inject(
            method = "stopServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;removeAll()V"
            )
    )
    private void postSaveAllPlayerList(CallbackInfo ci) {
        this.playerDB.close();
    }

    @Override
    public IDBInstance cesium$getStorage() {
        return this.playerDB;
    }
}
