package de.yamayaki.cesium.mixin.core.players;

import de.yamayaki.cesium.accessor.DatabaseSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
    @Shadow
    private PlayerList playerList;

    @Inject(
            method = "stopServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;removeAll()V"
            )
    )
    private void postSaveAllPlayerList(CallbackInfo ci) {
        ((DatabaseSource) this.playerList)
                .cesium$getStorage()
                .close();
    }
}
