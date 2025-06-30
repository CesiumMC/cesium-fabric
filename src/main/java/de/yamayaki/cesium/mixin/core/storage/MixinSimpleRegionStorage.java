package de.yamayaki.cesium.mixin.core.storage;

import de.yamayaki.cesium.accessor.DatabaseActions;
import de.yamayaki.cesium.accessor.DatabaseSetter;
import de.yamayaki.cesium.accessor.SpecificationSetter;
import de.yamayaki.cesium.common.DatabaseSpec;
import de.yamayaki.cesium.common.lmdb.LMDBInstance;
import net.minecraft.world.level.chunk.storage.IOWorker;

//? >= 1.20.6 {
@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.level.chunk.storage.SimpleRegionStorage.class)
//?}
public class MixinSimpleRegionStorage implements DatabaseSetter, SpecificationSetter, DatabaseActions {
    /*? >= 1.20.6 {*/ @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final /*?}*/ private IOWorker worker;

    @Override
    public void cesium$setStorage(LMDBInstance dbInstance) {
        ((DatabaseSetter) this.worker).cesium$setStorage(dbInstance);
    }

    @Override
    public void cesium$setSpec(DatabaseSpec<?, ?> databaseSpec) {
        ((SpecificationSetter) this.worker).cesium$setSpec(databaseSpec);
    }

    @Override
    public void cesium$flush() {
        ((DatabaseActions) this.worker).cesium$flush();
    }

    @Override
    public void cesium$close() {
        ((DatabaseActions) this.worker).cesium$close();
    }
}
