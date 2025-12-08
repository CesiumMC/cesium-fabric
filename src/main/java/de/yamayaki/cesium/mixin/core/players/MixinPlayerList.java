package de.yamayaki.cesium.mixin.core.players;

import com.llamalad7.mixinextras.sugar.Local;
import de.yamayaki.cesium.api.accessor.DatabaseSetter;
import de.yamayaki.cesium.api.accessor.DatabaseSource;
import de.yamayaki.cesium.api.database.IDBInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public class MixinPlayerList implements DatabaseSource {
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(
            method = "getPlayerAdvancements",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
            )
    )
    private void setAdvancementsStorage(ServerPlayer serverPlayer, CallbackInfoReturnable<PlayerAdvancements> cir, @Local PlayerAdvancements playerAdvancements) {
        ((DatabaseSetter) playerAdvancements).cesium$setStorage(this.cesium$getStorage());
    }

    @Override
    public IDBInstance cesium$getStorage() {
        return ((DatabaseSource) this.server).cesium$getStorage();
    }
}
