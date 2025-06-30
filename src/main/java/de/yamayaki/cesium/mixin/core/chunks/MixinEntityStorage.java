package de.yamayaki.cesium.mixin.core.chunks;

import de.yamayaki.cesium.api.accessor.DatabaseSetter;
import de.yamayaki.cesium.api.accessor.DatabaseSource;
import de.yamayaki.cesium.api.accessor.SpecificationSetter;
import de.yamayaki.cesium.common.lmdb.LMDBInstance;
import de.yamayaki.cesium.common.spec.WorldDatabaseSpecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executor;

@Mixin(EntityStorage.class)
public class MixinEntityStorage {
    //? >= 1.20.6 {
    @Shadow @Final private net.minecraft.world.level.chunk.storage.SimpleRegionStorage simpleRegionStorage;
     //?} else {
    /*@Shadow @Final private net.minecraft.world.level.chunk.storage.IOWorker worker;
    *///?}

    @Inject(method = "<init>", at = @At("RETURN"))
    //? >= 1.20.6 {
     public void initCesiumEntities(net.minecraft.world.level.chunk.storage.SimpleRegionStorage simpleRegionStorage, ServerLevel serverLevel, Executor executor, CallbackInfo ci) {
    //?} else {
    /*public void initCesiumEntities(ServerLevel serverLevel, java.nio.file.Path path, com.mojang.datafixers.DataFixer dataFixer, boolean bl, Executor executor, CallbackInfo ci) {
    *///?}
        LMDBInstance storage = ((DatabaseSource) serverLevel).cesium$getStorage();

        //? >= 1.20.6 {
        ((DatabaseSetter) this.simpleRegionStorage).cesium$setStorage(storage);
        ((SpecificationSetter) this.simpleRegionStorage).cesium$setSpec(WorldDatabaseSpecs.ENTITY);
        //?} else {
        /*((DatabaseSetter) this.worker).cesium$setStorage(storage);
        ((SpecificationSetter) this.worker).cesium$setSpec(WorldDatabaseSpecs.ENTITY);
        *///?}
    }
}
